# Project2 - Week 1 이슈

## 주간 목표
- Scale & Async 프로젝트의 베이스를 만들고 Redis/Kafka 실험 가능한 환경을 구축한다.

## 이슈 목록 (체크리스트)

### ISSUE-1: 이벤트 중심 시나리오 정의
- [x] 시나리오 확정: 주문 생성 -> 정산/알림 이벤트
- [x] 이벤트 키/페이로드 스키마 초안 작성
- [x] 실패 시나리오 2개 정의(중복/지연)

완료 기준
- 이벤트 흐름 다이어그램 또는 텍스트 설계 완료

산출물
- `event-scenario.md`

---

### ISSUE-2: Docker 기반 인프라 세팅
- [x] MySQL/Redis/Kafka Compose 구성
- [x] 로컬 기동 및 포트 충돌 점검
- [x] 헬스체크 명령 정리

완료 기준
- 3개 컴포넌트 정상 기동 확인

산출물
- `docker-compose.yml`
- `infra-healthcheck.md`

검증 결과
- `docker compose up -d` 기동 확인
- 포트 확인: 3307/6379/9092 + 18080/18081/18082
- 헬스체크: mysql(`mysqld is alive`), redis(`PONG`), kafka(topic list 명령 정상 실행)
- 시각화 확인: Adminer/Redis Commander/Kafka UI 접속 가능

---

### ISSUE-3: Spring Boot 프로젝트 생성
- [x] Kotlin + Spring Boot 생성
- [x] Redis/Kafka 연동 의존성 추가
- [x] 환경변수 템플릿 정리

완료 기준
- 앱 기동 시 Redis/Kafka 접속 설정 로딩 확인

산출물
- `app/`
- `app/src/main/resources/application.yml`
- `app/.env.template`
- `app/src/main/kotlin/com/sugowslt/ordersettlementasync/config/InfraProperties.kt`
- `app/src/main/kotlin/com/sugowslt/ordersettlementasync/config/InfraPropertiesLoader.kt`

검증 결과
- `cd app && .\gradlew.bat test` 통과
- `bootRun` 로그에서 `infra.config.loaded redisHost=localhost redisPort=6379 kafkaBootstrapServers=localhost:9092 consumerGroup=order-settlement-group serverPort=8080` 확인

---

### ISSUE-4: 캐시 대상 API 설계
- [x] 조회 API 1개 정의
- [x] 캐시 키 전략 정의
- [x] 무효화 트리거 정의

완료 기준
- 캐시 정책 문서 1개 작성

산출물
- `cache-policy.md`

검증 결과
- 조회 API 정의: `GET /api/v1/orders/{orderId}/summary`
- 캐시 키 전략: `order:summary:v1:{orderId}` (TTL 60초)
- 무효화 트리거: 주문 상태 변경 / 정산 상태 변경 / 운영자 수동 수정

---

### ISSUE-5: Kafka POC(발행/구독)
- [x] 이벤트 발행 코드 1개
- [x] 이벤트 수신 코드 1개
- [x] 소비 실패 로그 처리 1개

완료 기준
- 로컬에서 발행/소비 로그 확인

산출물
- `app/src/main/kotlin/com/sugowslt/ordersettlementasync/api/OrderEventController.kt`
- `app/src/main/kotlin/com/sugowslt/ordersettlementasync/kafka/OrderEventProducer.kt`
- `app/src/main/kotlin/com/sugowslt/ordersettlementasync/kafka/OrderEventConsumer.kt`
- `app/src/main/kotlin/com/sugowslt/ordersettlementasync/event/OrderCreatedEvent.kt`

검증 결과
- API 호출: `POST /api/v1/events/orders` (정상/강제실패 2건)
- 발행 로그 확인: `kafka.publish.success ...`
- 소비 로그 확인: `kafka.consume.success ...`
- 실패 로그 확인: `kafka.consume.failed ... reason=forced consume failure`

## Week 1 DoD
- [x] Redis/Kafka 포함 로컬 환경 재현 가능
- [x] 이벤트 POC 동작 확인
- [ ] 다음 주 성능 측정 계획(p95/TPS) 수립
