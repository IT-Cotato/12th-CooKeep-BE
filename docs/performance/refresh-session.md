# DB 기반 Refresh Session의 Redis 전환 성능 비교

## 1. 측정 목적

DB `user_sessions` 테이블에 저장하던 Refresh Session을 Redis TTL 구조로 전환하고 Refresh Token Rotation과 재사용 탐지를 추가했을 때, 전체 Refresh 인증 흐름의 처리량·응답시간·저장소 부하가 어떻게 달라지는지 확인했다.

비교 대상은 다음 두 커밋이다.

| 구분 | 커밋 | 구현 |
| --- | --- | --- |
| A | `b68c85c` | DB `user_sessions` 기반 Refresh Session |
| B | `363fa0a` | Redis Session + Refresh Token Rotation + 재사용 탐지 |

이 비교는 저장소 명령 하나만 측정한 micro benchmark가 아니다. 사용자 상태 조회, `lastAccessAt` 갱신, JWT 처리, 응답 직렬화를 포함한 `POST /api/auth/refresh` end-to-end 비교다. B에는 Redis 전환뿐 아니라 새 Access·Refresh Token 생성, Cookie 갱신, SHA-256 digest 계산, Lua Script 기반 Rotation 비용도 포함된다.

Access Token 활성 세션 검증이 추가된 C 단계는 Refresh API 요청에 Access Token을 사용하지 않으므로 이번 A/B 비교에서 제외했다.

## 2. 기존 방식의 한계

A는 Refresh 요청마다 JWT를 검증한 뒤 `user_sessions`를 조회하고, DB에 원문으로 저장된 Refresh Token과 요청 Token을 비교한다.

```text
Refresh JWT 검증
→ 사용자 상태 조회
→ user_sessions 조회
→ 원본 Refresh Token 비교
→ lastAccessAt 갱신
→ 새 Access Token 발급
```

이 구조에는 다음 한계가 있었다.

- Refresh 요청마다 Session 확인용 DB SELECT가 추가된다.
- Refresh Token 원문이 DB에 저장된다.
- 동일 Refresh Token을 만료 전까지 반복 사용할 수 있다.
- 동일 Token의 동시 사용과 Rotation 이후 재사용을 식별할 수 없다.
- Session 만료 데이터를 애플리케이션과 DB가 관리해야 한다.

## 3. Redis 전환 구조

B는 사용자별 활성 Session 하나를 Redis에 저장한다.

```text
key   = auth:session:{userId}
value = {sessionId}:{refreshTokenDigest}
TTL   = 최초 로그인 시점부터 남은 Refresh Token 수명
```

Refresh 요청은 Lua Script 한 번으로 현재 digest 비교와 다음 digest 교체를 원자적으로 처리한다.

```text
Refresh JWT 검증
→ 사용자 상태 조회
→ Lua Script로 현재 digest 검증 및 다음 digest 교체
→ lastAccessAt 갱신
→ 새 Access·Refresh Token 발급
→ 새 Refresh Cookie 반환
```

- Refresh Token 원문 대신 SHA-256 digest만 저장한다.
- Rotation 이후 이전 Token은 즉시 사용할 수 없다.
- 재사용이 감지되면 현재 Session 전체를 폐기한다.
- Redis TTL로 만료를 관리하되 Rotation 시 절대 만료 시각을 연장하지 않는다.
- Redis 장애 시 인증을 허용하지 않는 fail-closed 방식을 적용한다.

## 4. 커밋 차이 점검

측정 전에 `git diff b68c85c..363fa0a`를 확인했다.

성능에 영향을 주는 주요 차이는 다음과 같다.

- A의 `user_sessions` SELECT와 원문 Token 비교 제거
- B의 새 Refresh JWT 생성 및 Cookie 갱신 추가
- B의 요청 Refresh Token과 신규 Refresh Token에 대한 SHA-256 digest 계산이 각각 1회씩, 총 2회 추가
- B의 Redis Lua `EVALSHA` 및 내부 `GET`·`SET` 추가

사용자 상태 조회, `lastAccessAt` 갱신, JPA/Hikari 설정, 로깅 수준, Docker 자원 제한, 응답 body 구조는 같은 조건을 유지했다. 따라서 결과는 단순히 DB 명령을 Redis 명령으로 치환한 수치가 아니라 보안 기능이 강화된 B 전체 흐름의 수치다.

## 5. 측정 환경

측정일은 2026-07-31이며 상세 환경은 [`environment.md`](../../performance/refresh-session/results/environment.md)에 기록했다.

| 구성 요소 | 조건 |
| --- | --- |
| Host | Windows, Docker Desktop, WSL2 |
| Docker Engine | 29.5.3 |
| Docker 가용 자원 | 8 vCPU, 약 7.61 GiB |
| Application | 2 CPU, 768 MiB, JVM Heap 512 MiB |
| MySQL | `mysql:8.0.36`, 1 CPU, 1 GiB |
| Redis | `redis:7.2-alpine`, 0.5 CPU, 256 MiB |
| k6 | `grafana/k6:0.49.0`, 1 CPU, 512 MiB |

A와 B 이미지는 측정 전에 각각 `b68c85c`, `363fa0a`에서 빌드해 고정했다. 측정 도중 checkout, build, 대규모 파일 작업은 수행하지 않았다.

## 6. 측정 방법

### 사용자 구성

총 100명을 seed하고 용도를 분리했다.

| 용도 | 사용자 수 | 범위 |
| --- | ---: | --- |
| 워밍업 | 10명 | benchmark 1~10 |
| 본 측정 | 50명 | benchmark 11~60 |
| 예비 | 40명 | benchmark 61~100 |

본 측정에서는 VU 1개가 사용자 1명과 독립 Cookie 하나를 사용한다. 여러 VU가 같은 Refresh Token을 공유하지 않는다.

### 부하 조건

- API: `POST /api/auth/refresh`
- 부하: 50 VU
- 본 측정: 180초
- 반복: A/B 각각 유효 결과 5회
- 모델: 응답 직후 다음 요청을 보내는 closed workload
- B는 Rotation 응답의 새 Cookie를 다음 요청에 계속 사용
- 로그인과 워밍업 요청은 본 측정 결과에서 제외

따라서 결과는 “동시 사용자 50명이 응답 직후 Refresh 요청을 반복하는 조건에서 관측된 처리량과 응답시간”으로 해석한다. VU나 요청률을 단계적으로 높여 포화 지점을 찾은 테스트가 아니므로 시스템의 최대 처리량을 의미하지 않는다. closed workload에서는 응답시간이 낮아지면 같은 VU가 더 많은 요청을 보내므로 RPS와 지연시간이 서로 연관된다.

### 워밍업

처음 제안된 30초 워밍업으로는 JVM과 컨테이너 처리량이 안정되지 않았다. 사전 측정에서 A는 후반 구간이 다시 하락했고 B는 180초까지 처리량이 계속 상승했다. 완전한 정상 상태를 확인했다고 볼 수는 없지만, 모든 실행에 동일한 180초 워밍업을 적용하고 워밍업 사용자를 본 측정 사용자와 분리했다.

실행 순서는 시간대 편향을 줄이기 위해 교차했다.

```text
1라운드: A1 → B1
2라운드: B2 → A2
3라운드: A3 → B3
4라운드: B4 → A4
5라운드: A5 → B5
```

각 실행마다 애플리케이션 컨테이너를 재생성하고 Health Check를 확인한 뒤 seed, Session 초기화, 워밍업, 본 측정 사용자 로그인, MySQL·Redis 통계 초기화, 본 측정 순서로 진행했다.

### RPS 계산과 유효성

k6 Counter의 rate는 시나리오 전체 실행 수명과 `gracefulStop` 등의 영향을 받을 수 있어 참고값으로만 사용했다. 공식 RPS는 로그인과 워밍업을 제외하고, 모든 회차에 동일하게 적용한 180초 부하 구간을 기준으로 직접 계산했다.

```text
Refresh RPS = 완료된 Refresh 요청 수 / 180초
```

공식 RPS는 180초 부하 구간 동안 시작되어 `gracefulStop` 내 완료된 요청 수를 180초로 나누어 계산했다. 모든 회차에 동일한 분모와 종료 기준을 적용했으며, 모든 공식 회차에서 시작 요청 수와 완료 요청 수가 같음을 확인했다.

모든 공식 실행은 다음 조건을 통과했다.

- 측정 사용자 Cookie 50개 준비
- `refresh_started = refresh_completed = http_reqs`
- 성공 응답과 Access Token 반환 확인
- 오류율 0%
- 측정 중 애플리케이션 재시작 없음

## 7. 실행별 결과

단위는 RPS와 ms다.

| 구현 | 회차 | 완료 요청 | RPS | 평균 | p50 | p90 | p95 | p99 | 최대 | 오류율 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| A | 1 | 50,176 | 278.76 | 179.00 | 170.38 | 260.83 | 308.46 | 424.25 | 1,021.54 | 0% |
| A | 2 | 46,299 | 257.22 | 193.99 | 181.48 | 285.43 | 336.62 | 475.87 | 1,373.27 | 0% |
| A | 3 | 49,405 | 274.47 | 181.81 | 163.92 | 277.62 | 334.93 | 584.42 | 1,430.62 | 0% |
| A | 4 | 41,379 | 229.88 | 217.11 | 197.80 | 335.85 | 406.00 | 609.58 | 1,875.91 | 0% |
| A | 5 | 38,073 | 211.52 | 235.96 | 213.94 | 375.21 | 455.41 | 670.02 | 1,241.67 | 0% |
| B | 1 | 55,514 | 308.41 | 161.72 | 154.30 | 240.15 | 279.65 | 371.99 | 951.40 | 0% |
| B | 2 | 50,959 | 283.11 | 176.07 | 162.25 | 264.89 | 311.22 | 498.07 | 1,760.16 | 0% |
| B | 3 | 52,641 | 292.45 | 170.60 | 160.22 | 261.76 | 301.99 | 431.97 | 953.88 | 0% |
| B | 4 | 50,655 | 281.42 | 176.85 | 149.72 | 272.11 | 356.29 | 729.91 | 3,907.61 | 0% |
| B | 5 | 55,493 | 308.29 | 161.78 | 149.92 | 250.95 | 289.03 | 424.87 | 1,044.11 | 0% |

## 8. 구현별 중앙값과 분산

대표값은 5회 중앙값을 사용한다. IQR은 Q3-Q1이며 사분위수는 linear interpolation 방식으로 계산했다.

| 지표 | A 중앙값 | A IQR | A 최소~최대 | B 중앙값 | B IQR | B 최소~최대 | 중앙값 변화 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| RPS | 257.22 | 44.59 | 211.52~278.76 | 292.45 | 25.19 | 281.42~308.41 | **13.7% 증가** |
| 평균 | 193.99 ms | 35.30 | 179.00~235.96 | 170.60 ms | 14.29 | 161.72~176.85 | **12.1% 감소** |
| p50 | 181.48 ms | 27.42 | 163.92~213.94 | 154.30 ms | 10.29 | 149.72~162.25 | 15.0% 감소 |
| p90 | 285.43 ms | 58.23 | 260.83~375.21 | 261.76 ms | 13.94 | 240.15~272.11 | 8.3% 감소 |
| p95 | 336.62 ms | 71.07 | 308.46~455.41 | 301.99 ms | 22.19 | 279.65~356.29 | **10.3% 감소** |
| p99 | 584.42 ms | 133.71 | 424.25~670.02 | 431.97 ms | 73.20 | 371.99~729.91 | 26.1% 감소 |

구현별 중앙값 비교는 A 5회의 중앙값과 B 5회의 중앙값을 비교한다. 이 기준에서 B는 A 대비 RPS가 13.7% 증가하고 평균 응답시간과 p95가 각각 12.1%, 10.3% 감소했다. 시간대가 가까운 A/B를 짝지은 라운드별 변화율 중앙값은 다음 절에서 별도로 제시한다.

RPS와 평균 응답시간은 A/B 범위가 겹치지 않았고 5개 라운드 모두 B가 같은 방향으로 개선됐다. p95는 5개 라운드에서 모두 감소했으며 구현별 중앙값 기준 10.3%, 라운드별 변화율 중앙값 기준 9.8% 개선됐다. 다만 회차별 범위가 일부 겹쳐 개선 폭에는 실행 환경 변동이 포함됐을 수 있다. p99 중앙값은 감소했지만 B의 최대값이 A보다 크고 라운드 2·4에서 악화되는 등 회차별 방향과 변동 폭이 일관되지 않아 꼬리 지연이 안정적으로 개선됐다고 단정하지 않는다.

## 9. 라운드별 짝 비교

RPS 증가는 `(B-A)/A`, 응답시간 감소는 `(A-B)/A`로 계산했다.

| 라운드 | 순서 | RPS 변화 | 평균 감소 | p95 감소 | p99 감소 |
| --- | --- | ---: | ---: | ---: | ---: |
| 1 | A → B | +10.64% | 9.65% | 9.34% | 12.32% |
| 2 | B → A | +10.07% | 9.24% | 7.55% | -4.66% |
| 3 | A → B | +6.55% | 6.17% | 9.84% | 26.09% |
| 4 | B → A | +22.42% | 18.54% | 12.24% | -19.74% |
| 5 | A → B | +45.75% | 31.44% | 36.53% | 36.59% |
| 중앙값 | - | **+10.64%** | **9.65%** | **9.84%** | 12.32% |

라운드별 비교는 같은 라운드에서 가까운 시간대에 실행한 A/B의 변화율을 계산한 뒤 그 중앙값을 사용한다. 이 기준에서는 RPS가 10.6% 증가하고 평균 응답시간과 p95가 각각 9.7%, 9.8% 감소했다.

후반 라운드에서 특히 A의 처리량 저하와 응답시간 증가가 관측되어 라운드별 변화율의 분산이 커졌다. 장시간 실행에 따른 호스트 상태, MySQL 부하 또는 기타 환경 요인이 영향을 줬을 가능성이 있으나, 자원 지표를 충분히 수집하지 못해 이번 측정만으로 원인을 특정할 수는 없다. B가 먼저 실행된 2·4라운드와 A가 먼저 실행된 1·3·5라운드 모두 RPS·평균·p95는 B가 개선됐으므로 순서 하나만으로 결과 방향이 결정된 것은 아니다. 다만 개선 폭의 정확한 일반화에는 독립된 Linux 서버나 CI Runner에서의 추가 측정이 필요하다.

## 10. MySQL·Redis 부하 변화

| 항목 | A | B |
| --- | ---: | ---: |
| 전체 MySQL statement / Refresh | 5.958~5.970회 | 4.946~4.957회 |
| `user_sessions` SELECT / Refresh | 약 0.998회 | 0회 |
| 사용자 상태 SELECT / Refresh | 약 1회 | 약 1회 |
| `lastAccessAt` UPDATE / Refresh | 약 1회 | 약 1회 |
| Redis `EVALSHA` / Refresh | 0회 | 1회 |
| Lua 내부 `GET` / Refresh | 0회 | 1회 |
| Lua 내부 `SET` / Refresh | 0회 | 1회 |
| Redis Lua 호출당 시간 | - | 중앙값 약 51.81 μs |
| MySQL `Max_used_connections` | 11 | 11 |

B는 Session 관련 DB SELECT를 제거해 요청당 MySQL statement를 약 1회 줄였다. 대신 Refresh마다 Redis `EVALSHA` 1회와 Script 내부 `GET`·`SET`이 발생한다. 전체 DB 접근은 사라지지 않았다. 사용자 상태 조회와 `lastAccessAt` 갱신이 남아 있기 때문이다.

`Connections`와 `Max_used_connections`는 처리 요청 수와 측정용 통계 조회 자체의 영향이 있고, `Max_used_connections`는 두 구현 모두 11로 같아 이번 조건에서 유의미한 차이를 보이지 않았다.

## 11. 보안 동작 검증

B 커밋을 실제 API와 Redis에 연결해 11개 검증을 수행했고 모두 통과했다.

| 검증 항목 | 결과 |
| --- | --- |
| 로그인 시 Redis Session 생성 | 성공 |
| 원본 Refresh Token 미저장, digest 저장 | 성공 |
| Rotation 후 절대 만료 시각 미연장 | 성공 |
| 이전 Token 재사용 탐지 | `401 AUTH-015`, Cookie 만료 |
| 재사용 탐지 후 Rotation 승자 Token 폐기 | 이후 `401` |
| 동일 Token 동시 요청 | 하나 `200`, 하나 `401 AUTH-015` |
| 동시 재사용 탐지 후 승자 Token 폐기 | 이후 `401` |
| 이전 로그인 Session이 현재 로그인 Session을 폐기하지 않음 | 현재 Session Refresh `200` |
| TTL 만료 Session 사용 | `401`, Cookie 만료 |
| Redis 장애 중 Refresh | `503 AUTH-016` |
| Redis 장애 중 로그인 | `503 AUTH-016`, Cookie 미발급 |

상세 응답은 [`security-verification.md`](../../performance/refresh-session/results/security-verification.md)와 `security-verification.json`에 저장했다.

동일 Refresh Token으로 동시 요청이 발생하면 재사용 공격과 정상적인 클라이언트 경쟁 요청을 구분하지 않고 활성 Session 전체를 폐기한다. 따라서 첫 요청이 받은 새 Refresh Token도 사용할 수 없다. 클라이언트는 Refresh 요청을 single-flight 방식으로 합쳐야 하며, 해당 제어가 없으면 정상적인 동시 요청에서도 재로그인이 필요할 수 있다. 프런트엔드의 single-flight 구현 여부는 이번 백엔드 측정 범위에서 확인하지 않았으므로 운영 적용 전 확인·보완해야 한다.

## 12. 제외 실행

B 4차 attempt1은 상위 측정 프로세스가 중단되면서 168.456초만 실행됐다. `refresh_started=51,814`, `refresh_completed=51,764`로 진행 중 요청 50개가 남아 유효성 기준을 충족하지 못했다. 원본과 제외 사유를 보존하고 동일 조건의 attempt2를 공식 B 4차 값으로 사용했다.

마음에 들지 않는 수치를 이유로 제외한 실행은 없다. 상세 내용은 [`excluded-runs.md`](../../performance/refresh-session/results/excluded-runs.md)에 기록했다.

## 13. 한계

- 로컬 Windows Docker Desktop 측정이므로 운영 Linux 환경의 절대 성능으로 일반화할 수 없다.
- 후반부에 특히 A의 성능 저하가 관측됐으나 애플리케이션, MySQL, JVM 및 호스트 자원 지표를 충분히 수집하지 못해 원인을 구분하지 못했다.
- 180초 워밍업 후에도 B의 사전 워밍업 처리량이 계속 상승해 완전한 정상 상태를 입증하지 못했다.
- closed workload이므로 RPS와 응답시간은 서로 연관된다.
- Docker CPU·메모리 샘플러가 동기 k6 프로세스 동안 멈춰 실행당 시작·종료 경계 샘플 2개만 남았다. 평균·최대 CPU/메모리 지표로 사용하지 않았다.
- JVM GC, HikariCP active·pending은 시계열로 수집하지 못했다.
- MySQL과 Redis는 같은 Docker Desktop 호스트의 CPU·I/O를 공유한다.
- Redis가 인증 Session의 기준 저장소가 되어 장애 시 인증 전체가 fail-closed되는 가용성 비용이 생긴다.
- Redis Rotation과 DB의 `lastAccessAt` 갱신은 분산 트랜잭션이 아니다.
- 동일 Refresh Token의 동시 요청은 재사용 공격과 정상 경쟁 요청을 구분하지 않고 활성 Session을 폐기하므로, 클라이언트의 single-flight 제어가 없으면 정상 요청에서도 재로그인이 필요할 수 있다.
- p99는 라운드 2·4에서 악화되어 일관된 개선으로 판단할 수 없다.

## 14. 요약

> DB `user_sessions` 기반 Refresh 인증을 Redis TTL 구조로 전환하고, Lua Script 기반 Refresh Token Rotation과 재사용 탐지를 적용했다. 로그인과 워밍업을 본 측정에서 분리한 뒤 50 VU·180초 조건에서 A/B를 교차 순서로 각각 5회 측정했다. 구현별 중앙값 기준으로 Redis 기반 구현은 DB 기반 구현 대비 RPS가 13.7% 증가하고 평균 응답시간과 p95가 각각 12.1%, 10.3% 감소했다. 시간대가 가까운 A/B의 라운드별 변화율 중앙값은 RPS 10.6% 증가, 평균 응답시간 9.7% 감소, p95 9.8% 감소였다. 또한 요청당 약 1회 발생하던 `user_sessions` SELECT를 제거한 대신 Redis Lua 호출이 요청당 1회 추가됐다. p99는 회차별 방향과 변동 폭이 일관되지 않아 꼬리 지연이 안정적으로 개선됐다고 단정하지 않았다.

원시 결과와 집계 파일은 [`performance/refresh-session/results`](../../performance/refresh-session/results)에 보존했다.
