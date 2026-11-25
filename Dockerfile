# Stage 1: Build file JAR
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true

# Stage 2: Run ứng dụng
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy file jar đã build từ Stage 1
COPY --from=build /app/target/*.jar app.jar

# Copy sẵn thư mục uploads để các ảnh bánh trái hiển thị ngay
COPY uploads ./uploads

EXPOSE 8080

ENTRYPOINT ["java", "-Dserver.port=${PORT:8080}", "-jar", "app.jar"]
