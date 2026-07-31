# B 보안 동작 검증

대상 애플리케이션은 Redis Session과 Refresh Token Rotation 및 절대 만료시각 보완이 적용된 `0d5d992` 커밋이다. benchmark 환경의 실제 API와 Redis를 사용했으며 결과 원문은 `security-verification.json`에 저장했다.

| 검증 | 관측 결과 | 판정 |
| --- | --- | --- |
| 로그인 Session 생성 | `auth:session:90` 생성, Cookie 발급 | 통과 |
| 원본 Token 미저장 | `{sessionId}:{64자리 SHA-256 digest}` 패턴, 원본 불포함 | 통과 |
| 절대 TTL 유지 | Rotation 전·후 만료 epoch 1,786,721,863,000ms 동일, TTL 감소 1,616ms(경과 1,449ms) | 통과 |
| 이전 Token 재사용 | 첫 Rotation `200`, 이전 Token `401 AUTH-015`, 만료 Cookie | 통과 |
| 재사용 탐지 후 승자 폐기 | 새 Token 재사용 시 `401 AUTH-002` | 통과 |
| 동일 Token 동시 요청 | 상태 코드 `200, 401`, 거부 응답 `AUTH-015` | 통과 |
| 동시 탐지 후 승자 폐기 | 새 Token 재사용 시 `401 AUTH-002` | 통과 |
| 다른 로그인 Session 격리 | 이전 Session `401 AUTH-002`, 현재 Session `200` | 통과 |
| TTL 만료 | `401 AUTH-002`, 만료 Cookie | 통과 |
| Redis 장애 중 Refresh | `503 AUTH-016` | 통과 |
| Redis 장애 중 로그인 | `503 AUTH-016`, Cookie 미발급 | 통과 |

## 동시 Refresh 정책

동일 Refresh Token으로 두 요청이 동시에 들어오면 Lua Script의 digest 비교·교체가 원자적으로 실행돼 한 요청만 Rotation에 성공한다. 다른 요청은 이미 Rotation된 Token의 재사용으로 판단되고 현재 Session 전체를 삭제한다.

그 결과 먼저 성공한 요청이 받은 새 Refresh Token도 더 이상 사용할 수 없다. 탈취 Token 재사용 시 현재 Session을 안전하게 폐기하는 정책이지만, 재사용 공격과 정상적인 클라이언트 경쟁 요청을 구분하지 않으므로 정상적인 동시 요청에서도 재로그인이 필요할 수 있다. 클라이언트는 Refresh 요청을 single-flight 방식으로 합쳐야 한다. 프런트엔드의 single-flight 구현 여부는 이번 백엔드 측정 범위에서 확인하지 않았으며 운영 적용 전 확인·보완해야 한다.

## Redis 장애

검증 중 benchmark Redis 컨테이너를 실제로 중단했다. Refresh와 로그인 모두 `503 AUTH-016`으로 실패했고 로그인 응답에는 Refresh Cookie가 없었다. 검증 후 같은 Redis 컨테이너를 재시작하고 `PING=PONG`을 확인했다.
