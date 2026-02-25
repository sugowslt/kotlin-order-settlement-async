# Troubleshooting Log

오류가 발생하고 해결된 경우 아래 3가지를 반드시 남깁니다.
1) 오류 발생 원인
2) 해결 방법
3) 여러 방법 중 해당 방법을 선택한 이유

---

## 기록 템플릿 (반복 사용)

### [YYYY-MM-DD] 이슈 제목
- 증상
  - 재현 조건:
  - 에러 로그:
- 원인
  - 
- 시도한 해결 방법
  - 방법 A:
  - 방법 B:
  - 방법 C:
- 최종 해결
  - 
- 선택 이유
  - 방법 A를 선택하지 않은 이유:
  - 방법 B를 선택하지 않은 이유:
  - 최종 방법을 선택한 이유:
- 재발 방지
  - 

---

## 2026-02-25 ISSUE-5 Kafka POC 실행 중 오류 기록

### 1) `gradlew.bat` 인식 실패
- 증상
  - 재현 조건: 루트(`C:\backendgo`)에서 상대 경로 `./gradlew.bat bootRun` 실행
  - 에러 로그: `CommandNotFoundException: .\gradlew.bat`
- 원인
  - `gradlew.bat` 파일은 `project2/app` 하위에 있는데 실행 경로가 루트였음
- 시도한 해결 방법
  - 방법 A: `Set-Location c:\backendgo\project2\app` 후 상대 경로 실행
  - 방법 B: 절대 경로 `c:\backendgo\project2\app\gradlew.bat` 실행
- 최종 해결
  - 절대 경로 + `-p c:\backendgo\project2\app`로 Gradle 프로젝트 경로를 명시
- 선택 이유
  - 방법 A를 선택하지 않은 이유: 백그라운드 터미널의 현재 작업 디렉터리가 보장되지 않아 재현성이 낮음
  - 최종 방법을 선택한 이유: 경로 의존성이 제거되어 자동화/재실행 시 안정적임
- 재발 방지
  - 문서/명령 예시에서 `-p` 옵션 또는 `app` 폴더 진입을 명시

### 2) Kafka Consumer 시작 실패 (`NoClassDefFoundError: com/fasterxml/jackson/databind/JavaType`)
- 증상
  - 에러 로그: `Failed to construct kafka consumer` + `NoClassDefFoundError: ... JavaType`
- 원인
  - Kafka JSON 역직렬화가 필요로 하는 `jackson-databind` 런타임 클래스가 누락됨
- 시도한 해결 방법
  - 방법 A: Kafka JSON 직렬화를 유지하고 누락 의존성 추가
  - 방법 B: JSON 역직렬화를 포기하고 문자열 전송으로 단순화
- 최종 해결
  - 즉시 조치로 `build.gradle.kts`에 `com.fasterxml.jackson.core:jackson-databind` 추가
- 선택 이유
  - 방법 B를 바로 적용하지 않은 이유: 먼저 최소 변경으로 기존 구조를 살리는 것이 영향 범위가 작음
  - 최종 방법을 선택한 이유: 원인(의존성 누락)과 직접적으로 대응되는 최소 수정
- 재발 방지
  - 직렬화 방식 변경 시 런타임 의존성 체크리스트 추가

### 3) Producer 직렬화 실패 (`InvalidDefinitionException`)
- 증상
  - API 호출 시 500 발생, 로그에 `InvalidDefinitionException` (이벤트 내 `Instant` 직렬화 이슈)
- 원인
  - 이벤트 페이로드의 시간 타입(`Instant`) 직렬화 설정이 불안정했고 기본 매퍼 환경과 충돌
- 시도한 해결 방법
  - 방법 A: Jackson JavaTime 모듈/매퍼 설정을 별도로 추가
  - 방법 B: 이벤트의 `occurredAt`을 ISO-8601 문자열로 전환
- 최종 해결
  - `OrderCreatedEvent.occurredAt` 타입을 `String`으로 변경
- 선택 이유
  - 방법 A를 선택하지 않은 이유: 설정 지점이 늘어나 초기 POC 범위를 넘어감
  - 최종 방법을 선택한 이유: 이벤트 계약(JSON) 기준에서는 문자열 타임스탬프가 단순하고 호환성이 높음
- 재발 방지
  - 초기 POC 단계에서는 복합 시간 타입보다 문자열 표준 포맷 우선

### 4) Consumer 역직렬화 루프 (`Cannot construct instance ... no Creators`) 및 핸들러 처리 불가
- 증상
  - 로그: `RecordDeserializationException`, `JsonDeserializer`, `This error handler cannot process SerializationException directly`
- 원인
  - JsonDeserializer 설정/코틀린 데이터 클래스 생성자/기존 토픽 레코드 포맷이 맞지 않아 소비 루프가 실패
- 시도한 해결 방법
  - 방법 A: JsonDeserializer + ErrorHandlingDeserializer + Kotlin module 조합으로 정교하게 설정
  - 방법 B: Kafka value를 문자열(JSON)로 수신 후 애플리케이션에서 수동 파싱
- 최종 해결
  - Producer/Consumer를 `String` 직렬화/역직렬화로 전환하고, Consumer 내부에서 `ObjectMapper`로 수동 파싱
- 선택 이유
  - 방법 A를 선택하지 않은 이유: 설정이 복잡하고 현재 POC 목표(발행/구독/실패 로그 확인) 대비 과도함
  - 최종 방법을 선택한 이유: 오류 지점을 애플리케이션 코드로 명확히 가져와 제어 가능하며, 실패 로그 처리가 단순해짐
- 재발 방지
  - POC 단계 기본값: Kafka value `String`, 실패 처리 로직은 Consumer 내부에서 제어

### 5) `ObjectMapper` 빈 주입 실패
- 증상
  - 에러 로그: `No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper' available`
- 원인
  - Spring Boot 4 구성에서 기대한 방식으로 `ObjectMapper` 빈이 자동 주입되지 않음
- 시도한 해결 방법
  - 방법 A: `@Bean ObjectMapper` 설정 클래스 추가
  - 방법 B: 컴포넌트 내부에서 `jacksonObjectMapper()` 로컬 생성
- 최종 해결
  - Producer/Consumer 내부에 로컬 `jacksonObjectMapper()` 적용
- 선택 이유
  - 방법 A를 선택하지 않은 이유: 전역 설정 추가는 영향 범위가 넓음
  - 최종 방법을 선택한 이유: POC 목적상 국소적 수정이 빠르고 부작용이 적음
- 재발 방지
  - 공통 직렬화 정책이 필요해지는 시점(다수 도메인/공용 모듈)에서 전역 Bean 방식으로 승격
