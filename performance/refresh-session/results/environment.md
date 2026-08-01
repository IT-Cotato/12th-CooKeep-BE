# 측정 환경

## 비교 대상

| 구분 | 커밋 | Docker 이미지 |
| --- | --- | --- |
| A | `b68c85c` | `cookeep-benchmark-a:b68c85c` |
| B | `363fa0a` | `cookeep-benchmark-b:363fa0a` |

각 이미지는 detached worktree에서 측정 전에 빌드했다. 현재 작업 트리와 무관한 고정 이미지를 사용했으며 측정 중 build나 checkout을 수행하지 않았다.

## Host와 컨테이너

- 측정일: 2026-07-31
- Host: Windows, Docker Desktop, WSL2
- Docker Engine: 29.5.3
- Kernel: `6.18.33.2-microsoft-standard-WSL2`
- Docker 가용 CPU: 8
- Docker 가용 메모리: 8,176,398,336 bytes(약 7.61 GiB)

| 서비스 | 이미지 | CPU | 메모리 |
| --- | --- | ---: | ---: |
| Application | 고정 A/B 이미지 | 2.0 | 768 MiB |
| JVM | Application 내부 | - | `-Xms512m -Xmx512m` |
| MySQL | `mysql:8.0.36` | 1.0 | 1 GiB |
| Redis | `redis:7.2-alpine` | 0.5 | 256 MiB |
| k6 | `grafana/k6:0.49.0` | 1.0 | 512 MiB |
| Session 준비 | `node:20-alpine` | 0.5 | 256 MiB |

## 애플리케이션 설정

- Spring profile: `benchmark`
- MySQL Performance Schema: 활성화
- MySQL max connections: 200
- Redis persistence: 비활성화
- Refresh Cookie Secure: 비활성화(로컬 HTTP)
- root/application/security 로그: WARN
- Hibernate SQL 및 format SQL 출력: 비활성화

## 사용자와 부하

- seed: 100명
- 워밍업: 10명(benchmark 1~10)
- 본 측정: 50명(benchmark 11~60)
- 예비: 40명(benchmark 61~100)
- 본 측정: 50 VU, 180초, closed workload
- 워밍업: 10 VU, 180초
- 각 VU는 독립 사용자와 Refresh Cookie를 사용

## 컨테이너 재사용 정책

- 애플리케이션 컨테이너: 실행마다 재생성
- MySQL·Redis 컨테이너: 동일 컨테이너를 유지
- 각 실행 전 benchmark Session 상태와 사용자 seed를 초기화
- 본 측정 직전 MySQL Performance Schema/상태와 Redis commandstats를 초기화

MySQL·Redis 컨테이너를 유지한 이유는 매 실행마다 저장소 자체의 cold start가 결과에 섞이는 것을 피하기 위해서다. 동일 정책을 A/B에 모두 적용했다.
