FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

COPY libs ./libs
COPY src ./src

RUN mkdir -p build/classes \
    && find src/main/java -name "*.java" > sources.txt \
    && javac -encoding UTF-8 -parameters -cp "libs/*" -d build/classes @sources.txt \
    && cp -r src/main/resources/. build/classes/

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /app/build/classes ./classes
COPY --from=builder /app/libs ./libs

EXPOSE 8080

ENTRYPOINT ["java", "-cp", "classes:libs/*", "com.medicare.MediCareApplication"]
