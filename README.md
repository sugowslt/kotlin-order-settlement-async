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
- ISSUE-4 완료: 캐시 대상 API/키 전략/무효화 트리거 정의
- ISSUE-5 완료: Kafka 발행/구독 POC + 소비 실패 로그 처리

관련 문서:
- `activity-plan.md`
- `week1-issues.md`
- `event-scenario.md`
- `infra-healthcheck.md`
- `cache-policy.md`
- `troubleshooting-log.md`

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
- Week 1 DoD 남은 항목: 다음 주 성능 측정 계획(p95/TPS) 수립

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

## 9) ISSUE-5 완료 결과
- 구현
	- 이벤트 발행 API: `POST /api/v1/events/orders`
	- 발행 컴포넌트: `OrderEventProducer`
	- 수신 컴포넌트: `OrderEventConsumer`
	- 이벤트 모델: `OrderCreatedEvent`
- 검증
	- 정상 이벤트 발행 후 `kafka.publish.success`, `kafka.consume.success` 로그 확인
	- `forceFail=true` 이벤트 발행 후 `kafka.consume.failed ... reason=forced consume failure` 로그 확인

## 10) 오류 기록 원칙
- 오류가 발생하고 해결된 경우 반드시 `troubleshooting-log.md`에 아래 3가지를 기록한다.
	1. 오류 발생 원인
	2. 해결 방법
	3. 여러 방법 중 해당 방법을 선택한 이유
- 기록 단위는 “증상 -> 원인 -> 시도한 해결 방법들 -> 최종 해결 -> 선택 이유 -> 재발 방지” 순서를 따른다.
- 오류를 닫을 때는 `troubleshooting-log.md` 상세 기록 + `README.md` 요약 기록을 **항상 함께** 업데이트한다.

## 11) 트러블슈팅 기록 요약 (필수)
- 2026-02-25 / `gradlew.bat` 인식 실패
	- 원인: 실행 경로가 `app`이 아니어서 wrapper 파일 탐색 실패
	- 해결: `-p c:\backendgo\project2\app`로 Gradle 프로젝트 경로 명시
	- 선택 이유: 터미널 cwd 의존성을 제거해 재현성과 자동화 안정성이 높음
- 2026-02-25 / Kafka Consumer 시작 실패 (`NoClassDefFoundError: ... JavaType`)
	- 원인: Kafka JSON 역직렬화에 필요한 Jackson 런타임 클래스 누락
	- 해결: `build.gradle.kts`에 `jackson-databind` 추가
	- 선택 이유: 원인에 직접 대응하는 최소 변경
- 2026-02-25 / Producer 직렬화 실패 (`InvalidDefinitionException`)
	- 원인: 이벤트 시간 타입(`Instant`) 직렬화 충돌
	- 해결: `occurredAt`을 ISO-8601 문자열로 전환
	- 선택 이유: POC 단계에서 설정 복잡도 대비 안정성/호환성이 가장 높음
- 2026-02-25 / Consumer 역직렬화 루프 (`RecordDeserializationException`)
	- 원인: JsonDeserializer 설정/토픽 레코드 포맷 불일치
	- 해결: Kafka value를 `String`으로 통일하고 Consumer 내부에서 수동 파싱
	- 선택 이유: 오류 지점을 애플리케이션 코드에서 제어 가능하고 실패 로그 처리 단순화
- 2026-02-25 / `ObjectMapper` 빈 주입 실패
	- 원인: 기대한 자동 빈 구성이 현재 구조와 불일치
	- 해결: Producer/Consumer 내부 로컬 매퍼(`jacksonObjectMapper()`) 사용
	- 선택 이유: 전역 설정 추가 없이 국소 수정으로 빠르게 안정화 가능

상세 로그는 `troubleshooting-log.md` 참고.