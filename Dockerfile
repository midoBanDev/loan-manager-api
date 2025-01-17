# 빌드 이미지로 Gradle:8.11.1 & eclipse-temurin:17로 지정
FROM gradle:8.11.1-jdk17 AS builder

# apt-get update로로 Debian/Ubuntu 기반 리눅스 시스템에서 패키지 목록을 최신화 & Git 설치
RUN apt-get update && apt-get install -y git

# 프로젝트 클론
WORKDIR /app
#RUN git clone https://github.com/midoBanDev/loan-manager-api.git .

# 또는 로컬 코드 베이스 정보 카피
COPY . .

# gradlew 파일에 실행 권한 부여
RUN chmod +x gradlew
RUN ./gradlew build --no-daemon
# 빌드
#RUN gradle build


# 런타임 이미지로 eclipse-temurin:17-jre 사용
FROM eclipse-temurin:17-jre

# 기본 유틸리티 설치
# apt-get update: 패키지 목록을 최신 상태로 갱신.
# apt-get install: 필요한 패키지를 설치.
# rm -rf /var/lib/apt/lists/*: 패키지 설치 후 더 이상 필요 없는 캐시를 삭제.
RUN apt-get update && apt-get install -y \
    iputils-ping \
    net-tools \
    curl \
    vim \
    tzdata \
    && rm -rf /var/lib/apt/lists/*

# 애플리케이션을 실행할 작업 디렉토리를 생성
WORKDIR /app

ARG GOOGLE_CLIENT_ID
ARG DB_URL
ARG DB_USERNAME 
ARG DB_PASSWORD
ARG DB_NAME

ENV GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
ENV DB_URL=${DB_URL}
ENV DB_USERNAME=${DB_USERNAME}
ENV DB_PASSWORD=${DB_PASSWORD}
ENV DB_NAME=${DB_NAME}

# 빌드 이미지에서 생성된 JAR 파일을 런타임 이미지로 복사
COPY --from=builder /app/build/libs/loan-manager-api-0.0.1-SNAPSHOT.jar loan-manager-api.jar

EXPOSE 8080 
CMD ["java", "-jar", "-Dspring.profiles.active=prod", "loan-manager-api.jar"]
