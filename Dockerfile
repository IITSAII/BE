# 1단계: 빌드
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY src src
RUN ./gradlew bootJar --no-daemon

# 2단계: 실행
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
COPY --from=build /app/build/libs/*.jar app.jar
RUN chown spring:spring app.jar
USER spring

EXPOSE 8080
# JAVA_OPTS로 힙 크기 등을 주입할 수 있도록 shell form 사용 (exec form은 환경변수 확장이 안 됨).
# EC2 메모리가 작아(약 900MB) 힙 제한 없이 기본값으로 돌리면 JVM이 시스템 메모리를 다 잡아먹고
# 리눅스 OOM Killer에 의해 강제 종료되는 문제가 반복됐다 (docker-compose-prod.yml에서 기본값 설정).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
