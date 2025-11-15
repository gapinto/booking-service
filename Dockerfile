FROM eclipse-temurin:17-jdk AS deps
WORKDIR /workspace
COPY pom.xml .
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
RUN mvn -q -DskipTests dependency:go-offline

FROM deps AS test
COPY src ./src
RUN mvn -q test

FROM deps AS package
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre AS runtime
ENV JAVA_OPTS=""
WORKDIR /app
COPY --from=package /workspace/target/booking-service-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]


