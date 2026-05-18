# MySQL 데이터베이스 환경 셋업 가이드

## 필수 설치 요구사항
- Docker & Docker Compose

## 빠른 시작

### 1. 저장소 클론
```bash
git clone <repository-url>
cd AMR-monitoring-smart-factory
```

### 2. 환경 설정
```bash
cp .env.example .env
# (필요시 .env에서 DB 비밀번호 등 수정)
```

### 3. 컨테이너 시작
```bash
docker-compose up -d
```

### 4. 데이터베이스 확인
```bash
docker-compose exec mysql mysql -u root -p$DB_ROOT_PASSWORD -e "USE amr_monitoring; SHOW TABLES;"
```

## 주요 명령어

| 명령어 | 설명 |
|--------|------|
| `docker-compose up -d` | MySQL 컨테이너 시작 |
| `docker-compose down` | 컨테이너 중지 및 제거 |
| `docker-compose logs -f mysql` | 실시간 로그 확인 |
| `docker-compose exec mysql mysql -u root -p` | MySQL CLI 접속 |

## 연결 정보
- **Host**: `localhost` 또는 `127.0.0.1`
- **Port**: `3306` (`.env`에서 변경 가능)
- **Database**: `amr_monitoring`
- **User**: `amr_user`
- **Password**: `amr_password` (`.env.example` 참고)

## 초기 데이터 자동 로드
`init.sql`의 모든 테이블과 데이터는 컨테이너 시작 시 자동으로 로드됩니다.

## 데이터 영속성
`mysql_data` 볼륨에 데이터가 저장되므로, 컨테이너를 재시작해도 데이터가 유지됩니다.
- 데이터를 초기화하려면: `docker-compose down -v`

## 문제 해결

### 포트 3306이 이미 사용 중인 경우
`.env` 파일에서 `DB_PORT` 값을 변경하세요:
```
DB_PORT=3307
```

### 컨테이너 시작 실패
로그를 확인하세요:
```bash
docker-compose logs mysql
```

## Java 백엔드와의 연결
`application.yaml`에서 다음과 같이 설정하세요:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/amr_monitoring
    username: amr_user
    password: amr_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```
