FROM gradle:8.5-jdk21 AS build
WORKDIR /app

COPY gradle/ gradle/
COPY gradlew gradlew.bat build.gradle settings.gradle ./

RUN ./gradlew dependencies --no-daemon

COPY src/ src/

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -g 1000 spring && \
    adduser -D -s /bin/sh -G spring -u 1000 spring

COPY --from=build /app/build/libs/*.jar app.jar

RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8090

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8090/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
