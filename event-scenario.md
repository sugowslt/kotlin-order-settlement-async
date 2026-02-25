# Event Scenario Draft (ISSUE-1)

## 1) 핵심 시나리오
`주문 생성 -> 정산 이벤트 -> 알림 이벤트`

### 단계별 흐름
1. 주문 생성 API가 요청을 수신한다.
2. 주문 정보를 저장한 뒤 `OrderCreated` 이벤트를 발행한다.
3. 정산 컨슈머가 `OrderCreated`를 수신하고 정산 데이터를 기록한다.
4. 정산 완료 시 `SettlementCompleted` 이벤트를 발행한다.
5. 알림 컨슈머가 `SettlementCompleted`를 수신하고 알림 로그를 기록한다.

## 2) 이벤트 키/페이로드 스키마 초안

### 2-1. OrderCreated
- key: `order:{orderId}`
- payload:
```json
{
  "eventId": "evt-order-created-001",
  "eventType": "OrderCreated",
  "occurredAt": "2026-02-25T12:00:00Z",
  "traceId": "trace-order-001",
  "orderId": 10001,
  "userId": 2001,
  "amount": 15000.50,
  "currency": "KRW",
  "idempotencyKey": "order-create-10001"
}
```

### 2-2. SettlementCompleted
- key: `settlement:{orderId}`
- payload:
```json
{
  "eventId": "evt-settlement-completed-001",
  "eventType": "SettlementCompleted",
  "occurredAt": "2026-02-25T12:00:02Z",
  "traceId": "trace-order-001",
  "orderId": 10001,
  "settlementId": "st-10001",
  "status": "COMPLETED",
  "amount": 15000.50
}
```

## 3) 실패 시나리오 정의

### 3-1. 중복 이벤트 수신
- 상황: 동일 `eventId` 또는 동일 `idempotencyKey`를 가진 메시지가 재전달됨
- 대응: 처리 이력 저장소(예: Redis Set/DB 테이블)로 중복 여부 확인 후 skip

### 3-2. 지연/순서 역전
- 상황: `SettlementCompleted`가 지연되거나 순서가 바뀌어 도착
- 대응: 이벤트 발생 시각/상태 전이 검증으로 비정상 순서 메시지 보류 또는 DLQ 전송

## 4) 운영 확인 포인트
- traceId로 API 로그와 소비 로그를 연결해 동일 요청 흐름 추적
- 각 소비 단계에서 처리시간(ms)과 결과 상태(success/fail) 로깅
- 실패 메시지 재처리 정책(재시도 횟수, DLQ 조건) 문서화

## 5) 다음 이슈 연결
- ISSUE-2에서 위 시나리오를 기준으로 토픽/컨슈머 그룹/인프라 포트 확정