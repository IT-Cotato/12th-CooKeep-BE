# Refresh Session benchmark

DB `user_sessions` 기반 Refresh 흐름(A)과 Redis Session + Refresh Token Rotation 흐름(B)을 동일한 로컬 Docker 환경에서 비교한다.

## 고정 환경

- A: `b68c85c`
- B: `363fa0a`
- MySQL 8.0.36: 1 CPU, 1 GiB
- Redis 7.2: 0.5 CPU, 256 MiB
- Application: 2 CPU, 768 MiB, JVM Heap 512 MiB
- k6 0.49.0: 1 CPU, 512 MiB
- seed 사용자 100명
- 워밍업 사용자 10명, 본 측정 사용자 50명, 예비 사용자 40명
- 180초 워밍업 후 50 VU로 180초 본 측정
- 공식 측정은 A와 B를 교차 순서로 각각 5회 실행

각 VU는 서로 다른 사용자와 Cookie를 사용한다. B에서는 Rotation 응답의 새 Refresh Cookie를 다음 요청에 계속 사용한다.

## 실행 전 준비

- Docker Desktop 및 Docker Compose
- Node.js 20 이상
- 비교 커밋을 checkout할 detached worktree 공간
- 로컬 벤치마크 전용 DB·JWT·VAPID 값

예시 파일을 복사한 뒤 로컬 값으로 교체한다. 이 파일은 Git에서 제외된다.

```powershell
Copy-Item performance\refresh-session\.env.example performance\refresh-session\.env
```

현재 seed의 BCrypt fixture는 격리된 benchmark 사용자 비밀번호와 연결돼 있으므로 `BENCHMARK_USER_PASSWORD`는 예시 값을 유지한다. DB·JWT·VAPID 값은 벤치마크 전용 값으로 교체하며 운영 자격증명을 복사하지 않는다.

PowerShell 세션에 환경변수를 불러온다.

```powershell
Get-Content performance\refresh-session\.env |
  Where-Object { $_ -and -not $_.StartsWith("#") } |
  ForEach-Object {
    $name, $value = $_.Split("=", 2)
    Set-Item -Path "Env:$name" -Value $value
  }
```

필수 변수는 `BENCHMARK_DB_PASSWORD`, `BENCHMARK_USER_PASSWORD`, `BENCHMARK_JWT_ACCESS_SECRET`, `BENCHMARK_JWT_REFRESH_SECRET`, `BENCHMARK_VAPID_PUBLIC_KEY`, `BENCHMARK_VAPID_PRIVATE_KEY`다. 값이 없으면 Compose 또는 측정 스크립트가 즉시 실패한다.

> 이 환경은 로컬 벤치마크 전용이다. 자동화는 benchmark DB seed를 다시 만들고 Redis `FLUSHDB`를 실행하므로 운영 DB·Redis 또는 외부에서 접근 가능한 인프라를 연결하면 안 된다.


## 이미지 준비

A와 B를 detached worktree에서 각각 빌드해 이미지와 커밋을 고정한다.

```powershell
git worktree add --detach C:\worktrees\cookeep-refresh-a b68c85c
git worktree add --detach C:\worktrees\cookeep-refresh-b 363fa0a

$env:BENCHMARK_APP_CONTEXT = "C:\worktrees\cookeep-refresh-a"
$env:BENCHMARK_APP_IMAGE = "cookeep-benchmark-a:b68c85c"
docker compose -f docker-compose.benchmark.yml build benchmark-app

$env:BENCHMARK_APP_CONTEXT = "C:\worktrees\cookeep-refresh-b"
$env:BENCHMARK_APP_IMAGE = "cookeep-benchmark-b:363fa0a"
docker compose -f docker-compose.benchmark.yml build benchmark-app
```

측정 중에는 checkout이나 build를 수행하지 않는다.

## 단일 실행

MySQL과 Redis를 먼저 실행한다.

```powershell
docker compose -f docker-compose.benchmark.yml up -d benchmark-mysql benchmark-redis
```

필수 환경변수를 지정하고 실행한다.

```powershell
$env:STAGE = "a"
$env:BENCHMARK_APP_IMAGE = "cookeep-benchmark-a:b68c85c"
$env:BENCHMARK_COMMIT = "b68c85c"
$env:ROUND = "1"
$env:ROUND_ORDER = "A1-B1"
$env:WARMUP_SECONDS = "180"
$env:MEASUREMENT_SECONDS = "180"
$env:ATTEMPT = "1"
node performance\refresh-session\scripts\run-benchmark.mjs
```

B는 `STAGE`, 이미지, 커밋만 변경한다.

```powershell
$env:STAGE = "b"
$env:BENCHMARK_APP_IMAGE = "cookeep-benchmark-b:363fa0a"
$env:BENCHMARK_COMMIT = "363fa0a"
```

실행 순서는 다음과 같이 교차한다.

```text
A1 → B1
B2 → A2
A3 → B3
B4 → A4
A5 → B5
```

## 유효성 기준

`run-benchmark.mjs`는 다음 조건을 만족한 결과만 `a-runN.json` 또는 `b-runN.json`으로 저장한다.

- 준비된 본 측정 Session 50개
- `refresh_started = refresh_completed = http_reqs`
- k6 rate로 역산한 측정시간과 설정 180초의 차이가 1% 이내
- 측정 중 애플리케이션 재시작 없음
- k6 정상 종료

k6 Counter의 rate는 시나리오 전체 실행 수명과 `gracefulStop` 등의 영향을 받을 수 있어 참고값으로만 사용한다. 공식 RPS는 로그인과 워밍업을 제외하고, 모든 회차에 동일하게 적용한 180초 부하 구간을 기준으로 직접 계산한다.

`Refresh RPS = 완료된 Refresh 요청 수 / 180초`

공식 RPS는 180초 부하 구간 동안 시작되어 `gracefulStop` 내 완료된 요청 수를 180초로 나눈 값이다. 모든 공식 회차에서 시작 요청 수와 완료 요청 수가 같음을 확인했다. 유효성 기준을 위반한 결과는 `excluded-*.json`으로 보존한다.

## 집계

```powershell
node performance\refresh-session\scripts\aggregate-results.mjs
```

다음 파일을 생성한다.

- `results/aggregate.json`
- `results/runs.csv`

5회 중앙값, Q1, Q3, IQR, 최소·최대와 라운드별 짝 변화율을 계산한다.

## B 보안 검증

B 애플리케이션과 benchmark MySQL·Redis가 실행 중일 때 다음 명령을 사용한다.

```powershell
node performance\refresh-session\scripts\verify-b-security.mjs
```

원본 미저장, 절대 TTL, Rotation 재사용, 동시 요청, 다른 로그인 Session 격리, 만료, Redis 장애 fail-closed를 검증하고 `results/security-verification.json`에 저장한다.

## 스크립트

| 파일 | 용도 |
| --- | --- |
| `refresh-warmup.js` | 별도 사용자 10명의 워밍업 |
| `refresh-load.js` | 준비된 50개 Session으로 본 측정 |
| `prepare-sessions.mjs` | 본 측정 로그인과 Cookie 파일 생성 |
| `run-benchmark.mjs` | 초기화·워밍업·측정·지표 수집·유효성 판정 |
| `aggregate-results.mjs` | 공식 10회 결과 집계 |
| `verify-b-security.mjs` | B 보안 동작 검증 |

## 결과

공개 결과는 다음처럼 구분한다.

- `a-run1.json`~`a-run5.json`, `b-run1.json`~`b-run5.json`: 공식 실행
- `aggregate.json`, `runs.csv`: 공식 10회 집계
- `environment.md`, `execution-log.md`, `excluded-runs.md`: 환경·순서·제외 근거
- `security-verification.*`: B 보안 동작 검증
- `summary.md`: 사람이 읽기 쉬운 결과 요약
- `raw/*-k6.json`: 공식 결과 및 제외 실행의 k6 원본
- `metrics/*-mysql-*.tsv`, `metrics/*-redis-*.txt`: DB·Redis 명령 근거

다음 자료는 로컬에는 남지만 Git 공개 대상에서는 제외한다.

- 애플리케이션 로그와 런타임 Session 파일
- 워밍업·preflight·smoke 결과
- 분석에 사용하지 못한 실행 시작·종료 시점의 Docker stats 샘플
- `execution-log.md`와 중복되는 `execution-log.txt`

예비 실행은 공식 A/B 5회 집계에 포함하지 않으며, 제외 실행은 `excluded-runs.md`에 사유와 함께 기록한다.

최종 문서는 [`docs/performance/refresh-session.md`](../../docs/performance/refresh-session.md), 원시 결과와 보조 문서는 [`results`](results)에 있다.
