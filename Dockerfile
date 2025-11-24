# Etapa de build
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copia o pom e baixa dependências em cache
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Copia o código e compila
COPY src ./src
RUN mvn -B clean package -DskipTests

# Etapa de runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copia o jar gerado
COPY --from=builder /app/target/*.jar app.jar

# Porta padrão do Spring Boot
EXPOSE 8080

# Variável opcional pra tunar a JVM via docker-compose (JAVA_OPTS)
ENV JAVA_OPTS=""

# Sobe o bjorn-ai
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
