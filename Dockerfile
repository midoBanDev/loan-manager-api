# 빌드 이미지로 Gradle:8.11.1 & eclipse-temurin:17로 지정
FROM gradle:8.11.1-jdk17 AS builder

# apt-get update로로 Debian/Ubuntu 기반 리눅스 시스템에서 패키지 목록을 최신화 & Git 설치
RUN apt-get update && apt-get install -y git

# 프로젝트 클론
WORKDIR /app
RUN git clone https://github.com/midoBanDev/loan-manager-api.git .

# 또는 로컬 코드 베이스 정보 카피
#RUN . .



# gradlew 파일에 실행 권한 부여
RUN chmod +x gradlew

# ENV SPRING_PROFILES_ACTIVE=test


# 빌드
RUN ./gradlew build -DGOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}


# 런타임 이미지로 eclipse-temurin:17-jre 사용
FROM eclipse-temurin:17-jre

# 애플리케이션을 실행할 작업 디렉토리를 생성
WORKDIR /app

# 빌드 이미지에서 생성된 JAR 파일을 런타임 이미지로 복사
COPY --from=builder /app/build/libs/loan-manager-api-0.0.1-SNAPSHOT.jar loan-manager-api.jar

EXPOSE 8080 
CMD ["java", "-jar", "-Dspring.profiles.active=prod", "loan-manager-api.jar"]
