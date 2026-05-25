# 백엔드 Dockerfile (backend/Dockerfile)
FROM eclipse-temurin:21-jre-jammy

# 작업 디렉토리 설정
WORKDIR /app

ENV TZ=Asia/Seoul

# JVM 메모리 설정
# -Xms: 초기 힙 크기 / -Xmx: 최대 힙 크기
# 서버 사양에 맞게 JAVA_OPTS 환경변수로 오버라이드 가능
#   예) docker run -e JAVA_OPTS="-Xms256m -Xmx512m" ...
ENV JAVA_OPTS="-Xms256m -Xmx512m \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:+OptimizeStringConcat \
  -Djava.security.egd=file:/dev/./urandom"

# JAR 파일을 컨테이너로 복사
COPY build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
