# Photobooth

포토부스 서비스 백엔드입니다.

## 기술 스택

- Java 17 / Spring Boot
- Spring Data JPA / PostgreSQL
- springdoc-openapi (Swagger)

## 실행 방법

### 1. 로컬 DB 실행

```bash
docker compose up -d
```

### 2. 애플리케이션 실행

기본 프로파일은 `local`입니다.

```bash
./gradlew bootRun
```

실행 후 API 문서는 `http://localhost:8080/swagger-ui/index.html`에서 확인할 수 있습니다.

### 3. 프로파일

| 프로파일 | 용도 | 설정 파일 |
|---|---|---|
| local | 로컬 개발 | `application-local.yml` |
| prod | 배포 | `application-prod.yml` (환경변수로 값 주입) |

## 패키지 구조

```
com.iitsaii.photobooth
├── global
│   ├── config    # Swagger, JPA Auditing, 타임존 등 전역 설정
│   ├── common    # 공통 응답 포맷(CommonResponse)
│   ├── error     # 에러 코드, 전역 예외 처리
│   └── entity    # BaseEntity, BaseTimeEntity
└── domain
    ├── session   # 포토부스 세션
    ├── payment   # 결제
    ├── photo     # 촬영 이미지
    └── printjob  # 인쇄 작업
```

## 개발 규칙

### DB / Entity 네이밍 규칙

- 테이블명: `snake_case`, 복수형 (예: `print_jobs`)
- 컬럼명: `snake_case` (예: `printed_at`)
- Entity 클래스명: `PascalCase`, 단수형 (예: `PrintJob`)
- Entity 필드명: `camelCase` (예: `printedAt`)
- PK 컬럼명은 `id` 고정
- 외래키 컬럼명은 `{참조 테이블 단수형}_id` (예: `session_id`)
- 모든 Entity는 `BaseEntity` 또는 `BaseTimeEntity`를 상속해 생성/수정/삭제 시각을 관리

### RESTful API 설계 규칙

1. **URI는 리소스(명사)로 표현하고, 행위는 HTTP Method로 표현한다.**
   - `GET /api/sessions/{id}` (O) / `GET /api/getSession` (X)
2. **컬렉션은 복수형 명사를 사용하고 계층 구조로 중첩한다.**
   - `GET /api/sessions/{sessionId}/photos`
3. **응답은 공통 응답 포맷(`CommonResponse`)으로 통일한다.**
   - 성공: `{ "success": true, "data": ..., "error": null }`
   - 실패: `{ "success": false, "data": null, "error": { "code": ..., "message": ... } }`

## 브랜치 전략

- `main`: 배포 브랜치
- `develop`: 개발 통합 브랜치
- 작업 브랜치: `{type}/{작업 내용}` (예: `feat/session-api`, `chore/base-setup`)
