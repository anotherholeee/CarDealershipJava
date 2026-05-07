# syntax=docker/dockerfile:1

FROM node:20-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
COPY --from=frontend-build /workspace/frontend/dist ./src/main/resources/static
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/logs \
    && mkdir -p /app/static \
    && chown -R app:app /app
COPY --from=build /workspace/target/autosalon.jar /app/app.jar
COPY --from=frontend-build /workspace/frontend/dist /app/static
RUN apk add --no-cache wget \
    && chown -R app:app /app/app.jar /app/static
USER app
EXPOSE 8080
ENV JAVA_OPTS=""
ENV SPRING_WEB_RESOURCES_STATIC_LOCATIONS="file:/app/static/,classpath:/static/"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
