# 제외 실행

## B 4차 attempt1

- 파일: `excluded-b-run4-attempt1.json`
- 시작: 2026-07-31T07:24:40.846Z
- 측정 command 시작: 2026-07-31T07:29:14.239Z
- 측정 command 종료: 2026-07-31T07:32:02.695Z
- 실행 시간: 168.456초
- 설정 시간: 180초
- 시작 요청: 51,814
- 완료 요청: 51,764

측정 제어 프로세스를 종료하면서 k6가 설정한 180초를 채우지 못했고, 진행 중 요청 50개가 남았다.

```text
refresh_started != refresh_completed
actual duration differs from configured duration by more than 1%
```

객관적인 유효성 기준을 위반해 공식 결과에서 제외했다. 원시 k6 결과, 워밍업 결과, MySQL·Redis·Docker 지표와 애플리케이션 로그는 삭제하지 않고 보존했다. 동일 조건의 attempt2가 모든 유효성 검사를 통과해 공식 B 4차 값으로 사용됐다.

이 실행을 제외한 이유는 수치가 좋거나 나빴기 때문이 아니다. B 4차 attempt1의 직접 환산 RPS는 287.58이었지만, 측정시간과 시작·완료 요청 수가 맞지 않아 채택하지 않았다.
