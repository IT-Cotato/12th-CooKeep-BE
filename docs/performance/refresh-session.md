# Refresh Session Redis 전환 성능 비교

## 측정 목적

DB에 원문으로 저장하던 Refresh Session을 Redis TTL 기반 세션으로 전환했을
때의 응답 지연, 처리량, DB 부하 변화를 같은 조건에서 비교한다. Access Token
활성 세션 확인을 위해 추가되는 Redis 조회 비용도 별도로 측정한다.

## 측정 조건

- 측정 환경: 로컬 Docker
- 반복 횟수: 각 단계 3회
- 결과값: 3회 중앙값과 최소·최대 범위
- 부하: 사용자별 독립 Cookie를 사용하는 50 VU, 3분

## 예비 측정 결과

구현 검증 단계에서 50 VU, 30초 조건으로 3회 측정했다. 아래 값은 최종
3분 비교가 아니라 시나리오와 Rotation Cookie 흐름이 정상 동작하는지
검증한 예비 결과다.

| 단계 | RPS | 평균 | p50 | p95 | p99 | 오류율 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| A. DB Session | 재측정 필요 | 재측정 필요 | 재측정 필요 | 재측정 필요 | 재측정 필요 | 재측정 필요 |
| B/C. Redis Rotation + Access Session 검증 | **243** | **197 ms** | **180 ms** | **398 ms** | **582 ms** | **0%** |

B/C 3회 범위:

| 항목 | 최소 | 중앙값 | 최대 |
| --- | ---: | ---: | ---: |
| RPS | 168 | 243 | 342 |
| 평균 | 140 ms | 197 ms | 283 ms |
| p50 | 117 ms | 180 ms | 255 ms |
| p95 | 305 ms | 398 ms | 593 ms |
| p99 | 472 ms | 582 ms | 951 ms |
| 오류율 | 0% | 0% | 0% |

원시 결과는 `performance/refresh-session/results/redis-run2.json`,
`redis-run3.json`, `redis-run4.json`에 저장했다.

## 저장소 부하 확인

통계 초기화 후 seed 재실행 없이 별도 50 VU·30초 부하를 실행했다. Refresh
13,294건이 모두 성공했고 약 441 RPS, 평균 111ms, p95 212ms, p99 309ms였다.
이 실행은 저장소 명령 수 확인용이며 위 3회 중앙값에는 포함하지 않았다.

- Redis
  - `EVALSHA`: 13,294회 — digest 비교·교체를 수행하는 Rotation Lua
  - `GET`: 13,294회 — Lua 내부 현재 세션 조회
  - `SET`: 13,344회 — 로그인 세션 생성 50회 + Rotation 교체 13,294회
  - Rotation 스크립트 Redis 실행시간: 호출당 평균 약 54.8µs
- MySQL
  - 사용자 ID 조회: 13,278회
  - `lastAccessAt`을 포함한 사용자 갱신: 13,328회
  - `user_sessions` 조회·갱신: 0회

Redis 전환으로 Refresh Session 테이블 접근은 제거됐지만 사용자 상태 조회와
`lastAccessAt` 갱신 때문에 Refresh 요청당 DB 트랜잭션은 남는다.

## 기준선 측정 주의사항

최초 DB 기준선 smoke 측정은 k6가 iteration마다 Cookie jar를 초기화하여
첫 요청 이후 Refresh Cookie가 전송되지 않았다. 이 결과는 서버 성능이
아니라 잘못된 부하 스크립트의 401 응답이므로 비교 대상에서 제외했다.
`noCookiesReset: true`를 적용한 동일 스크립트로 Redis 전환 직전 커밋을
별도 worktree에서 실행해야 유효한 A/B 비교가 된다.

## 해석 원칙

Refresh 처리에는 사용자 상태 조회와 `lastAccessAt` 갱신이 남아 있으므로
전체 DB 접근이 사라지지는 않는다. 세션 테이블 조회 제거, DB connection
사용량 감소, Rotation 원자성, Access Token 즉시 폐기와 그에 따른 Redis
조회 비용을 함께 평가한다.

예비 측정의 변동 폭이 크므로 최고 수치를 성과로 사용하지 않는다. 최종
보고에서는 3분 측정 3회의 중앙값과 범위, 호스트 CPU·메모리, MySQL
statement/HikariCP, Redis command 수치를 함께 기록한다.
