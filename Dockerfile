# Estágio 1: Build da aplicação com Gradle e Java 21
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew

COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# Estágio 2: Imagem enxuta e segura de Produção (JRE 21)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Executa com usuário não-root por segurança em produção
RUN addgroup -S autorecibo && adduser -S autorecibo -G autorecibo
USER autorecibo:autorecibo

COPY --from=build /app/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
