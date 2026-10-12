FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY projects/quanlydetai/pom.xml ./pom.xml
RUN mvn -B -ntp dependency:go-offline
COPY projects/quanlydetai/src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app && mkdir -p /app/uploads && chown app:app /app/uploads
COPY --from=build --chown=app:app /workspace/target/quanlydetai-0.0.1-SNAPSHOT.jar /app/app.jar
COPY --chown=app:app docker/start.sh /app/start.sh
USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -Duser.timezone=Asia/Ho_Chi_Minh"
ENTRYPOINT ["sh", "/app/start.sh"]
