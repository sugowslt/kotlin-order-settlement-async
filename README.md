# project2-order-settlement-async

주문·정산 흐름을 이벤트 기반으로 확장해 보기 위해 만든 POC 프로젝트입니다.

핵심은 Kafka를 붙였다는 사실 자체보다,
로컬 인프라를 재현하고 성능을 반복 측정하면서 병목을 찾는 과정을 남긴 점입니다.

## 무엇을 구현했나
- `POST /api/v1/events/orders` 이벤트 발행 API
- Producer/Consumer 기본 흐름
- 실패 이벤트 로깅 처리
- Redis/Kafka/MySQL Docker 환경 구성
- Adminer / Redis Commander / Kafka UI 연결
- Week2 성능 실측(1~4차) + Kafka 옵션 비교 실험

## 기술 스택
- Kotlin + Spring Boot
- Kafka, Redis, MySQL
- Docker Compose
- Gradle, JUnit

## 빠른 실행

### 1) 인프라 기동
```powershell
docker compose up -d
docker compose ps
```

포트
- MySQL: `3307`
- Redis: `6379`
- Kafka: `9092`

UI
- Adminer: `http://localhost:18080`
- Redis Commander: `http://localhost:18081`
- Kafka UI: `http://localhost:18082`

### 2) 앱 실행
```powershell
cd app
.\gradlew.bat test

$env:REDIS_HOST='localhost'
$env:REDIS_PORT='6379'
$env:KAFKA_BOOTSTRAP_SERVERS='localhost:9092'
$env:KAFKA_CONSUMER_GROUP='order-settlement-group'
.\gradlew.bat bootRun
```

- App: `http://localhost:8080`
- 부팅 시 `infra.config.loaded ...` 로그 확인

## 시연할 때 이렇게 보여주면 편함
1. API로 이벤트 발행
2. 앱 로그에서 publish/consume 확인
3. Kafka UI에서 토픽 메시지 확인
4. Adminer/Redis Commander에서 데이터/캐시 확인

한 줄로 말하면,
요청이 이벤트로 넘어가고 저장/캐시까지 반영되는 흐름을 눈으로 보여주는 프로젝트입니다.

## 정상/실패 검증 포인트

### 정상
- `kafka.publish.success`
- `kafka.consume.success`

### 실패
- `forceFail=true` 이벤트 발행
- `kafka.consume.failed ... reason=forced consume failure`

실패 케이스를 일부러 열어둔 이유는,
성공 시연만 하는 프로젝트보다 실제 운영 감각을 보여주기 좋기 때문입니다.

## 성능 측정 요약

### Week2 1차
- A: avg `1.15ms`, p95 `1.52ms`, TPS `120`, Error `0.00%`
- B: avg `1.16ms`, p95 `1.33ms`, TPS `300`, Error `0.00%`
- C: avg `1.22ms`, p95 `1.45ms`, TPS `200`, Error `0.00%`

### 2차 개선 후
- A: avg `1.01ms`, p95 `1.30ms`
- B: avg `1.17ms`, p95 `1.34ms`
- C: avg `1.08ms`, p95 `1.38ms`

### 3차/4차
- Step load(`500 -> 700 -> 900 RPS`) 안정 확인
- 확장 부하(`1200 -> 1500 -> 1800 RPS`)에서도 목표 기준 유지

### 옵션 실험
- `acks=1|all x partition=1|3`
- 네 조합 모두 Error `0.00%`
- 로컬 단일 브로커 환경에서는 `acks=all` 조합이 avg/p95에서 소폭 우세

## 트러블슈팅에서 배운 점
- 실행 경로가 틀리면 `gradlew.bat`부터 막힘
- Kafka 직렬화/역직렬화는 초기에 단순하게 맞추는 편이 빠름
- 고부하 측정 스크립트는 요청량보다 "제어 방식"이 더 중요함

상세 기록
- `troubleshooting-log.md`

## 관련 문서
- `activity-plan.md`
- `event-scenario.md`
- `cache-policy.md`
- `infra-healthcheck.md`
- `performance-plan-week2.md`
- `week1-issues.md`
- `week3-alert-tuning-notes.md`
- `week4-drill-result.md`
- `week4-operations-report.md`

## 현재 상태
- 목표 범위 완료
- 성능 실측과 옵션 비교까지 마무리
- 시연 가능한 상태

## 라이선스
MIT License. 자세한 내용은 `LICENSE` 파일을 참고하세요.
