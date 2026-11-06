# Runtime image for any Ledgerline service. Build the jars first (./gradlew bootJar), then e.g.
#   docker build -f infra/docker/service.Dockerfile --build-arg SERVICE=ledger-service .
FROM eclipse-temurin:25.0.5_8-jre-noble

ARG SERVICE

RUN groupadd --system app && useradd --system --gid app --no-create-home app

WORKDIR /app
COPY --chmod=0755 infra/docker/healthcheck.sh /app/healthcheck.sh
COPY services/${SERVICE}/build/libs/${SERVICE}.jar /app/app.jar

USER app
ENV SERVER_PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=6 CMD ["/app/healthcheck.sh"]
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
