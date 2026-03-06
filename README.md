# project2-order-settlement-async

Scale & Async 역량을 보여주기 위한 이벤트 기반 주문·정산 POC 프로젝트입니다.

Kafka, Redis, MySQL, Docker 기반의 로컬 인프라를 직접 구성하고, 이벤트 발행/소비 흐름과 성능 측정 결과까지 문서로 남긴 것이 핵심입니다.

## 1) 프로젝트 한눈에 보기
- 주문 생성 이벤트를 Kafka로 발행/소비하는 비동기 흐름 구현
- Redis, MySQL, Kafka를 포함한 로컬 인프라 직접 구성
- Adminer, Redis Commander, Kafka UI로 데이터 흐름을 눈으로 확인 가능
- 성능 실측을 1차~4차, 추가 옵션 실험까지 반복 수행
- 트러블슈팅을 원인·해결·선택 이유까지 기록

## 2) 이 프로젝트에서 보여주려는 것
- 동기 API 이후 비동기 처리로 확장하는 사고 과정
- 인프라를 포함한 로컬 재현성 확보
- Kafka 기반 이벤트 흐름의 기본 구조 이해
- 고부하에서도 p95, TPS, 에러율을 근거로 설명하는 방식
- 장애와 설정 이슈를 문서화하는 습관

## 3) 기술 스택
- Language: Kotlin
- Framework: Spring Boot
- Infra: Docker Compose, MySQL, Redis, Kafka
- UI Tools: Adminer, Redis Commander, Kafka UI
- Test/Build: Gradle, JUnit

## 4) 현재 구현 범위
- ISSUE-1 완료: 이벤트 중심 시나리오 정의
- ISSUE-2 완료: Docker 기반 인프라 세팅
- ISSUE-3 완료: Spring Boot Kotlin 프로젝트 부트스트랩 + 설정 로딩 확인
- ISSUE-4 완료: 캐시 대상 API/키 전략/무효화 정책 정의
- ISSUE-5 완료: Kafka 발행/구독 POC + 소비 실패 로그 처리
- Week 2 성능 실측 1차~4차 완료
- Kafka `acks x partition` 옵션 비교 실험 완료

관련 문서
- `activity-plan.md`
- `event-scenario.md`
- `cache-policy.md`
- `infra-healthcheck.md`
- `performance-plan-week2.md`
- `troubleshooting-log.md`
- `week1-issues.md`

## 5) 핵심 흐름

### 5-1. API
- 이벤트 발행: `POST /api/v1/events/orders`

### 5-2. 내부 처리
- `OrderEventController`가 주문 이벤트를 받음
- `OrderEventProducer`가 Kafka로 이벤트 발행
- `OrderEventConsumer`가 이벤트를 소비
- 실패 이벤트는 로그로 남겨 재현 가능하도록 처리

### 5-3. 시각적으로 확인할 수 있는 포인트
- Kafka UI: 토픽과 메시지 흐름 확인
- Adminer: MySQL 데이터 상태 확인
- Redis Commander: 캐시 키와 상태 확인

## 6) 실행 가이드

### 6-1. 사전 준비
- JDK 17
- Docker Desktop

### 6-2. 인프라 실행
```powershell
docker compose up -d
docker compose ps
```

기본 포트
- MySQL: `3307`
- Redis: `6379`
- Kafka: `9092`

시각화 도구
- Adminer: `http://localhost:18080`
- Redis Commander: `http://localhost:18081`
- Kafka UI: `http://localhost:18082`

### 6-3. 애플리케이션 실행
```powershell
cd app
.\gradlew.bat test

$env:REDIS_HOST='localhost'
$env:REDIS_PORT='6379'
$env:KAFKA_BOOTSTRAP_SERVERS='localhost:9092'
$env:KAFKA_CONSUMER_GROUP='order-settlement-group'
.\gradlew.bat bootRun
```

확인 포인트
- 부팅 로그에서 `infra.config.loaded ...` 확인
- 기본 서버 주소: `http://localhost:8080`

## 7) 빠른 시연 순서
1. `docker compose up -d`로 인프라 실행
2. `app` 실행 후 API 호출 준비
3. `POST /api/v1/events/orders`로 이벤트 발행
4. 애플리케이션 로그에서 publish / consume 로그 확인
5. Kafka UI에서 토픽 흐름 확인
6. Adminer, Redis Commander에서 저장/캐시 상태 확인

포트폴리오 설명용으로는
"요청을 받는 API -> Kafka 발행 -> Consumer 처리 -> 저장소/캐시/토픽 확인" 순서로 보여주면 이해가 쉽습니다.

## 8) 검증 포인트

### 8-1. 정상 흐름
- 정상 이벤트 발행 후 `kafka.publish.success` 로그 확인
- 이어서 `kafka.consume.success` 로그 확인

### 8-2. 실패 흐름
- `forceFail=true` 이벤트 발행
- `kafka.consume.failed ... reason=forced consume failure` 로그 확인

이렇게 정상/실패 둘 다 재현 가능하게 만들어 두어,
단순 성공 데모가 아니라 장애 관점까지 함께 설명할 수 있습니다.

## 9) 성능 측정 결과 요약

### 9-1. Week2 1차
- Scenario A: avg `1.15ms` / p95 `1.52ms` / TPS `120` / Error `0.00%`
- Scenario B: avg `1.16ms` / p95 `1.33ms` / TPS `300` / Error `0.00%`
- Scenario C: avg `1.22ms` / p95 `1.45ms` / TPS `200` / Error `0.00%`

### 9-2. 2차 개선 적용 후
- A: avg `1.01ms` / p95 `1.30ms`
- B: avg `1.17ms` / p95 `1.34ms`
- C: avg `1.08ms` / p95 `1.38ms`

### 9-3. 3차 Step Load
- `500 -> 700 -> 900 RPS`
- 900 RPS까지 유의미한 병목 없이 안정 구간 유지

### 9-4. 4차 고부하 확장
- `1200 -> 1500 -> 1800 RPS`
- 1800 RPS에서도 목표 기준 유지
- Error rate `0.00%`

### 9-5. Kafka 옵션 심화 실험
- 비교 조합: `acks=1|all x partition=1|3`
- 네 조합 모두 Error `0.00%`
- 로컬 단일 브로커 환경에서는 `acks=all` 조합이 avg/p95 기준 소폭 우세

정리
- 이 프로젝트는 기능 구현 자체보다,
  "부하를 어떻게 걸었고 결과를 어떻게 비교했는가"를 보여주는 데 의미가 있습니다.

## 10) 트러블슈팅 요약

### 주요 사례
- `gradlew.bat` 인식 실패
  - 원인: 실행 위치 문제
  - 해결: `-p` 옵션 또는 정확한 작업 경로 사용
- Kafka Consumer 시작 실패
  - 원인: Jackson 런타임 클래스 누락
  - 해결: `jackson-databind` 추가
- Producer 직렬화 실패
  - 원인: `Instant` 직렬화 충돌
  - 해결: `occurredAt`을 ISO-8601 문자열로 전환
- Consumer 역직렬화 루프
  - 원인: value 포맷 불일치
  - 해결: Kafka value를 `String`으로 통일 후 수동 파싱
- 고부하 측정 중 타임아웃 폭증
  - 원인: 시간기반 무한 전송 구조
  - 해결: 고정 요청 수 + worker pacing + cooldown 구조로 스크립트 개선

상세 내용은 `troubleshooting-log.md`에 정리되어 있습니다.

## 11) 이 프로젝트를 볼 때 좋은 포인트
- API 한 개만 있어도 비동기 아키텍처를 어떻게 보여줄지
- Kafka, Redis, MySQL을 단순 설치가 아니라 시연 흐름에 어떻게 묶을지
- p95, TPS, 에러율을 반복 실측으로 어떻게 쌓아가는지
- 장애 원인을 찾고 문서에 남기는 방식이 얼마나 재현 가능한지

## 12) 현재 상태
- project2의 목표 범위 완료
- 기능 구현 + 인프라 검증 + 성능 실측 + 옵션 심화 실험까지 마무리
- 포트폴리오 시연용으로 바로 설명 가능한 상태

## 13) 다음에 함께 보면 좋은 화면
- `project1/dashboard`: 전체 포트폴리오 흐름 소개
- `http://localhost:18082`: Kafka UI
- `http://localhost:18080`: Adminer
- `http://localhost:18081`: Redis Commander
- `project3`: Grafana / Prometheus 기반 운영 관측 프로젝트

## 라이선스
MIT License. 자세한 내용은 `LICENSE` 파일을 참고하세요.
