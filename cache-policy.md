# Cache Policy Draft (ISSUE-4)

## 1) 조회 API 정의
- API: `GET /api/v1/orders/{orderId}/summary`
- 목적: 주문 단건 조회 시 주문/정산 상태를 한 번에 반환해 대시보드 및 운영 조회 응답 시간을 줄인다.

### 응답 스키마(초안)
```json
{
  "orderId": 10001,
  "orderStatus": "CREATED",
  "settlementStatus": "COMPLETED",
  "amount": 15000.50,
  "currency": "KRW",
  "updatedAt": "2026-02-25T12:00:02Z",
  "traceId": "trace-order-001"
}
```

## 2) 캐시 키 전략
- 키 패턴: `order:summary:v1:{orderId}`
- 값: API 응답 JSON 직렬화 데이터
- TTL: `60초`
- 선택 이유
  - 주문/정산 상태 조회는 읽기 비율이 높고 최신성 요구가 초 단위로 충분함
  - v1 prefix를 포함해 스키마 변경 시 안전한 키 분리 가능

## 3) 캐시 동작 규칙
1. 조회 요청 시 `order:summary:v1:{orderId}` 조회
2. hit: 캐시 값 반환
3. miss: DB 조회 후 캐시 저장(TTL 60초) 후 응답
4. 캐시 저장 실패 시: DB 조회 응답은 정상 반환(캐시 실패로 요청 실패시키지 않음)

## 4) 무효화 트리거
- 트리거 A: 주문 상태 변경 이벤트 처리(예: 취소, 승인)
  - 삭제 키: `order:summary:v1:{orderId}`
- 트리거 B: 정산 상태 변경 이벤트 수신(`SettlementCompleted` 등)
  - 삭제 키: `order:summary:v1:{orderId}`
- 트리거 C: 수동 재처리/관리자 수정
  - 관리용 무효화 API 또는 운영 스크립트로 동일 키 삭제

## 5) 정합성/안정성 원칙
- 캐시 미스나 캐시 장애 시 DB fallback으로 가용성 우선
- 무효화 실패 시 TTL 만료로 최종 정합성 확보
- traceId를 로그에 포함해 캐시 hit/miss/evict 흐름을 추적

## 6) 운영 관측 포인트
- `cache.hit.count`
- `cache.miss.count`
- `cache.evict.count`
- `cache.get.latency.ms` (p95)

## 7) 완료 기준 매핑(ISSUE-4)
- 조회 API 1개 정의: `GET /api/v1/orders/{orderId}/summary`
- 캐시 키 전략 정의: `order:summary:v1:{orderId}`, TTL 60초
- 무효화 트리거 정의: 상태 변경 이벤트/관리자 수정 기반 3개
