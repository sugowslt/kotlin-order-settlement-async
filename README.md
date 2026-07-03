# order-settlement-async

주문 이벤트를 Kafka로 발행/소비하고, 로컬 인프라에서 성능을 반복 측정해본 프로젝트입니다.

## 무엇을 해결했나

- `POST /events/order-created` 이벤트 발행 API
- Producer/Consumer 기본 흐름 구현
- 실패 케이스 검증: `forceFail=true`일 때 consumer에서 catch 후 재시도
- Redis, MySQL, Kafka, Adminer, Kafka UI, Redis Commander까지 docker-compose로 재현
- Week2 성능 실측 1~4차 + Kafka 옵션 비교(`acks=1|all`, partition)
- 추적: consumer 로그에 `event.traceId`를 포함시켜서 요청 경로를 확인할 수 있게 함

## 기술 스택

- Kotlin + Spring Boot
- Kafka, Redis, MySQL
- Docker Compose
- Gradle, JUnit

## 실행 방법

### 인프라 기동

```bash
docker compose up -d
docker compose ps
```

포트:
- MySQL: `3307`
- Redis: `6379`
- Kafka: `9092`

UI:
- Adminer: `http://localhost:18080`
- Redis Commander: `http://localhost:18081`
- Kafka UI: `http://localhost:18082`

### 앱 실행

```bash
cd app
./gradlew bootRun
```

- App: `http://localhost:8080`
- 부팅 시 `infra.config.loaded ...` 로그를 확인하면 됩니다.

## clone만 해도 바로 보기

루트에 `viewer.html`을 넣어뒀어요. clone만 해도 브라우저로 바로 확인 가능합니다.

```bash
git clone git@github.com:sugowslt/kotlin-order-settlement-async.git
open kotlin-order-settlement-async/viewer.html
```

별도 빌드나 설정 없이 열 수 있습니다.

## 트러블슈팅

- gradlew 경로: `cd app`을 먼저 해야 정상 실행됩니다.
- Kafka 직렬화: 초기엔 복잡하게 하다가, 그냥 `String` 키/값으로 단순화하는 게 빠르다고 판단했어요.
- 고부하 측정: 요청량보다 제어 방식이 더 중요함. step load로 먼저 안정성을 검증한 뒤 확장 부하로 넘어갔습니다.
