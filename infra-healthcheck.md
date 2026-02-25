# Infra Healthcheck Commands (ISSUE-2)

## 1) 인프라 기동
```powershell
docker compose up -d
```

## 2) 컨테이너 상태 확인
```powershell
docker compose ps
```

## 3) 포트 점검
- MySQL: `3307`
- Redis: `6379`
- Kafka: `9092`
- Adminer: `18080`
- Redis Commander: `18081`
- Kafka UI: `18082`

```powershell
Test-NetConnection localhost -Port 3307
Test-NetConnection localhost -Port 6379
Test-NetConnection localhost -Port 9092
Test-NetConnection localhost -Port 18080
Test-NetConnection localhost -Port 18081
Test-NetConnection localhost -Port 18082
```

## 4) MySQL 헬스체크
```powershell
docker exec project2-mysql mysqladmin ping -h localhost -u root -proot
```

## 5) Redis 헬스체크
```powershell
docker exec project2-redis redis-cli ping
```

## 6) Kafka 헬스체크
```powershell
docker exec project2-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

## 7) 시각화 UI 접속
- Adminer: `http://localhost:18080`
- Redis Commander: `http://localhost:18081`
- Kafka UI: `http://localhost:18082`

## 8) 종료
```powershell
docker compose down
```

## 9) 장애/오류 기록 확인
- 오류 발생 및 해결 이력은 `troubleshooting-log.md`에서 확인한다.
- 각 항목은 아래 기준으로 정리한다.
	- 오류 발생 원인
	- 해결 방법
	- 여러 방법 중 해당 방법을 선택한 이유
