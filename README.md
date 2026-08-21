# order-settlement-async

주문 이벤트를 Kafka로 전달하고 정산 원장을 비동기로 생성하는 프로젝트입니다.

`주문 이벤트 API → Kafka → Consumer → MySQL 정산 원장`

## 핵심 구현

- `POST /api/v1/events/orders` 요청을 `202 Accepted`로 응답하고 Kafka에 비동기 발행
- Kafka 레코드 키를 `order:{orderId}`로 고정해 같은 주문의 파티션 순서 보장
- `X-Trace-Id` 헤더와 이벤트 `traceId`를 함께 전달해 발행·소비 로그 추적
- Producer 멱등성(`enable.idempotence=true`, `acks=all`) 적용
- `eventId`, `idempotencyKey`, `orderId`에 DB 유니크 제약을 적용해 중복 정산 방지
- 지수 백오프 재시도 후 `order-created.v1-dlt`로 실패 이벤트 격리
- 원본 토픽과 DLT를 각각 3개 파티션으로 구성해 DLT 파티션 보존
- Flyway로 H2 테스트 스키마와 MySQL 실행 스키마 분리 관리

## 기술 스택

- Kotlin, Spring Boot, Spring Kafka
- MySQL 8, JPA/Hibernate, Flyway
- Kafka, Docker Compose
- Gradle, JUnit 5

## 실행

### 1. 로컬 인프라

```bash
docker compose up -d
docker compose ps
```

- MySQL: `localhost:3307`
- Kafka: `localhost:9092`
- Adminer: `http://localhost:18080`
- Kafka UI: `http://localhost:18082`

### 2. 애플리케이션

MySQL을 사용하는 `prod` 프로필에는 다음 환경변수가 필요합니다.

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://localhost:3307/order_settlement?allowPublicKeyRetrieval=true
DB_USERNAME=app
DB_PASSWORD=app
```

```bash
cd app
./gradlew bootRun
```

- Health: `http://localhost:8080/actuator/health`

## API 확인

주문 이벤트를 발행합니다.

```http
POST /api/v1/events/orders
Content-Type: application/json

{
  "orderId": 1001,
  "userId": 42,
  "amount": 15000.00,
  "currency": "KRW",
  "traceId": "order-trace-1001",
  "idempotencyKey": "order-create-1001",
  "forceFail": false
}
```

Consumer가 처리한 정산 원장을 조회합니다.

```http
GET /api/v1/settlements/1001
```

`forceFail=true`로 발행하면 Consumer 재시도와 DLT 이동을 확인할 수 있습니다.

## 테스트

```bash
cd app
./gradlew test
```

테스트는 H2 MySQL 호환 모드와 별도 Flyway 마이그레이션을 사용하므로 Docker 없이 실행할 수 있습니다.

## 구현 과정에서 확인한 문제

- Consumer 예외를 내부에서 삼키면 offset이 커밋되어 재시도가 불가능해지므로 예외를 다시 던지도록 구성했습니다.
- DLT가 원본 파티션을 유지하려면 두 토픽의 파티션 수가 같아야 하므로 토픽 생성 설정과 테스트에서 함께 보장합니다.
- 애플리케이션 조회 후 중복을 판단하는 방식만으로는 동시 소비를 막을 수 없어 DB 유니크 제약을 최종 방어선으로 사용합니다.

## 프로젝트 보기

루트의 `viewer.html`을 브라우저로 열면 주요 흐름과 결과를 한 화면에서 확인할 수 있습니다.
