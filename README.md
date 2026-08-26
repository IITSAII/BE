# Photobooth

포토부스 서비스 백엔드입니다.

## 기술 스택

- Java 17 / Spring Boot
- Spring Data JPA / PostgreSQL
- springdoc-openapi (Swagger)

## 실행 방법

### 1. 로컬 DB 실행

Docker Desktop을 켠 상태에서 실행합니다.

```bash
docker compose -f docker-compose-local.yml up -d
```

### 2. 애플리케이션 실행

기본 프로파일은 `local`입니다 (`./gradlew bootRun`, `./gradlew test` 모두 별도 설정 없이 `local`을 기본으로 사용).

```bash
./gradlew bootRun
```

실행 후 API 문서는 `http://localhost:8080/swagger-ui/index.html`에서 확인할 수 있습니다.

`application-local.yml`의 DB 접속 정보는 위 `docker-compose-local.yml` 기본값(`localhost:5544`, `photobooth`/`photobooth`)과 일치하도록 되어 있어 별도 `.env` 설정 없이 바로 실행됩니다. 로컬 DB를 다른 포트/계정으로 띄웠다면 `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` 환경변수로 덮어쓸 수 있습니다.

### 3. 프로파일

| 프로파일 | 용도 | 설정 파일 |
|---|---|---|
| local | 로컬 개발 | `application-local.yml` |
| prod | 배포 | `application-prod.yml` (환경변수로 값 주입) |

## 패키지 구조

```
com.iitsaii.photobooth
├── global
│   ├── config    
│   ├── common    
│   ├── error     
│   └── entity   
├── session      
├── payment       
├── photo        
├── printjob     
└── magazine      
```

각 도메인 패키지는 `controller` / `service` / `repository` / `entity` / `dto` 레이어로 구성되며,
도메인 전용 에러 코드가 필요하면 해당 도메인 패키지 아래 `error`를 둔다 (예: `session.error.SessionErrorCode`).

