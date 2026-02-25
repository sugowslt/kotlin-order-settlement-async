# project2-order-settlement-async

Scale & Async 역량 증명을 위한 이벤트 기반 주문/정산 POC 프로젝트입니다.

## 1) 프로젝트 목표
- 주문 이벤트 기반 비동기 처리 흐름 설계
- Redis/Kafka를 활용한 확장성/안정성 실험 기반 마련
- 장애 시나리오(중복/지연) 대응 전략을 문서와 코드로 증명

## 2) 현재 상태 (Week 1 시작)
- ISSUE-1 완료: 이벤트 중심 시나리오 정의
- ISSUE-2~5 대기: 인프라/부트스트랩/캐시 설계/Kafka POC

관련 문서:
- `activity-plan.md`
- `week1-issues.md`
- `event-scenario.md`

## 3) Week 1 실행 순서
1. 이벤트 시나리오 확정 (`event-scenario.md`)
2. Docker Compose로 MySQL/Redis/Kafka 기동
3. Spring Boot Kotlin 프로젝트 생성 및 환경변수 템플릿 정리
4. 캐시 대상 API/무효화 정책 정의
5. Kafka 발행/구독 POC 구현

## 4) 완료 기준 (Week 1 DoD)
- Redis/Kafka 포함 로컬 환경 재현 가능
- 이벤트 발행/소비 로그 확인
- 다음 주 성능 측정 계획(p95/TPS) 수립

## 5) 다음 단계
- ISSUE-2: Docker 기반 인프라 세팅부터 진행