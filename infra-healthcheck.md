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
