# Photobooth Backend

포토부스 서비스 백엔드 (Java 17 / Spring Boot 4.1.1 / JPA / PostgreSQL).

## 명령어

- 로컬 DB: `docker compose -f docker-compose-local.yml up -d`
- 실행: `./gradlew bootRun` (기본 프로파일 `local`, 별도 설정 불필요)
- 테스트: `./gradlew test`
- 빌드(테스트 포함): `./gradlew build`
- API 문서: `http://localhost:8080/swagger-ui/index.html`

## 패키지 컨벤션

`domain/{도메인}/{controller,service,repository,entity,dto,error}` 구조를 모든 도메인에서 동일하게 유지한다.
전역 공통은 `global/{config,common,error,entity,s3}`에 둔다.

- 에러 코드: 도메인별 `error/XxxErrorCode.java`에서 `ErrorCode` enum 구현 (`HttpStatus`, 코드 문자열, 메시지)
- 공통 응답 포맷: `global/common/CommonResponse`
- 예외 처리: `global/error/GlobalExceptionHandler` + `CustomException`

## 금지 규칙

- 사용자가 명시적으로 요청하지 않는 한 `git commit` 하지 않는다 (스테이징까지만).
- `docker-compose-prod.yml`, `Dockerfile`의 메모리 설정(`mem_limit`, `JAVA_OPTS`)은 EC2가 약 900MB로 작아 OOMKilled 이력이 있는 영역이다 — 값을 건드릴 땐 `docs/incidents/oom-killer.md` 먼저 확인.

## 특히 조심할 도메인

- `domain/partner` (배정 로직): 우선순위·페널티 계산이 복잡하고 회귀가 반복된 이력이 있다 (`docs/partner-assignment.md` 참고). 이 로직을 변경할 때는 구현 전에 기존 우선순위/페널티 규칙과의 상호작용을 먼저 확인하고, 변경 후 관련 테스트(`PartnerServiceTest` 등)를 반드시 돌린다.

## 참고 문서

- `docs/partner-assignment.md` — 배정 로직 스펙
- `docs/incidents/` — 과거 장애 기록 (있으면 항상 먼저 확인)
