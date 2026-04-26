FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY build/libs/queueforge-*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/queueforge
ENV SPRING_DATASOURCE_USERNAME=queueforge
ENV SPRING_DATASOURCE_PASSWORD=queueforge

ENTRYPOINT ["java", "-jar", "app.jar"]
