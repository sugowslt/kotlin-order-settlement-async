# Week3 알림 민감도 튜닝

이 문서는 Kafka 기반 이벤트 처리 파이프라인의 알림/모니터링 민감도를 조정한 기록입니다.

## 조정 항목
- Consumer 지연 감지 임계값
- Publish 실패 알림 조건
- `forceFail` 시의 로깅/알림 분리

## 기준
- 정상 트래픽: 알림 없음
- 실패 케이스(`forceFail=true`): `kafka.consume.failed` 로그만 남기고 alert는 내보내지 않음
- 샘플링 간격: 기본 1000건마다 1회 로그

## 결과
- 정상 구간 false positive: 0건
- 실패 시 로그 노출: 정상 동작 확인
