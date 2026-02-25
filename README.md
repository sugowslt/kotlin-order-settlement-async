# project2-order-settlement-async

Scale & Async 역량 증명을 위한 이벤트 기반 주문/정산 POC 프로젝트입니다.

## 1) 프로젝트 목표
- 주문 이벤트 기반 비동기 처리 흐름 설계
- Redis/Kafka를 활용한 확장성/안정성 실험 기반 마련
- 장애 시나리오(중복/지연) 대응 전략을 문서와 코드로 증명

## 2) 현재 상태 (Week 1 시작)
- ISSUE-1 완료: 이벤트 중심 시나리오 정의
- ISSUE-2 완료: Docker 기반 인프라 세팅(MySQL/Redis/Kafka)
- ISSUE-3 완료: Spring Boot Kotlin 프로젝트 부트스트랩 + Redis/Kafka 설정 로딩 확인
- ISSUE-4~5 대기: 캐시 설계/Kafka POC

관련 문서:
- `activity-plan.md`
- `week1-issues.md`
- `event-scenario.md`
- `infra-healthcheck.md`

## 3) Week 1 실행 순서
1. 이벤트 시나리오 확정 (`event-scenario.md`)
2. Docker Compose로 MySQL/Redis/Kafka 기동
3. Spring Boot Kotlin 프로젝트 생성 및 환경변수 템플릿 정리
4. 캐시 대상 API/무효화 정책 정의
5. Kafka 발행/구독 POC 구현

## 4) 인프라 실행 가이드 (ISSUE-2)
1. 인프라 기동
	- `docker compose up -d`
2. 상태 확인
	- `docker compose ps`
3. 포트 점검
	- MySQL: `3307`, Redis: `6379`, Kafka: `9092`
4. 헬스체크 명령
	- 자세한 명령은 `infra-healthcheck.md` 참고

## 5) 시각화 확인 포인트
- Adminer (MySQL UI): `http://localhost:18080`
  - System: `MySQL`, Server: `project2-mysql`, User: `app`, Password: `app`, DB: `order_settlement`
- Redis Commander (Redis UI): `http://localhost:18081`
- Kafka UI: `http://localhost:18082`

위 3개 화면으로 데이터/큐/토픽 상태를 시각적으로 확인할 수 있습니다.

## 6) 완료 기준 (Week 1 DoD)
- Redis/Kafka 포함 로컬 환경 재현 가능
- 이벤트 발행/소비 로그 확인
- 다음 주 성능 측정 계획(p95/TPS) 수립

## 7) 다음 단계
- ISSUE-4: 캐시 대상 API 설계

## 8) ISSUE-3 완료 결과
- 산출물
	- `app/` (Spring Boot Kotlin 프로젝트)
	- `app/src/main/resources/application.yml`
	- `app/.env.template`
	- `app/src/main/kotlin/com/sugowslt/ordersettlementasync/config/InfraProperties.kt`
	- `app/src/main/kotlin/com/sugowslt/ordersettlementasync/config/InfraPropertiesLoader.kt`
- 검증
	- `cd app && .\gradlew.bat test` 통과
	- `cd app && $env:REDIS_HOST='localhost'; $env:REDIS_PORT='6379'; $env:KAFKA_BOOTSTRAP_SERVERS='localhost:9092'; $env:KAFKA_CONSUMER_GROUP='order-settlement-group'; .\gradlew.bat bootRun`
	- 부팅 로그 확인: `infra.config.loaded redisHost=localhost redisPort=6379 kafkaBootstrapServers=localhost:9092 consumerGroup=order-settlement-group serverPort=8080`