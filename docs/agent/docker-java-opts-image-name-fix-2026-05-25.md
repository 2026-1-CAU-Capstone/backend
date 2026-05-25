# Docker 설정 수정 – JAVA_OPTS 명시화 & IMAGE_NAME 흐름 분석 & pgvector 서비스 추가

**작업일**: 2026-05-25

---

## 1. 작업 내용

### 수정 파일
- `docker-compose-server.yml`
  - `environment` 섹션에 `JAVA_OPTS` 명시적 추가
  - `pgvector` 서비스 추가
  - `pgvector-data` 볼륨 추가
  - `backend` 서비스의 `depends_on`에 `pgvector` 추가
- `.env.prod.example`
  - `RAG_DB_NAME` 항목 추가
  - `RAG_DB_URL` 호스트를 `pgvector-host` → compose 서비스명 `pgvector`로 수정

```yaml
# 변경 전
environment:
  - SPRING_PROFILES_ACTIVE=prod

# 변경 후
environment:
  - SPRING_PROFILES_ACTIVE=prod
  - JAVA_OPTS=${JAVA_OPTS}
```

---

## 2. 설계 의도

### JAVA_OPTS 흐름 (수정 후)

```
.env.prod.example (예시 파일)
  └─▶ secrets.ENV_FILE (GitHub Secret에 실제 값 저장)
        └─▶ CI/CD deploy job: echo "${{ secrets.ENV_FILE }}" > .env
              └─▶ docker-compose-server.yml
                    ├─ env_file: .env         ← 컨테이너 전체 환경변수 주입
                    └─ environment:
                         └─ JAVA_OPTS=${JAVA_OPTS}  ← 명시적 override
                               └─▶ Dockerfile ENTRYPOINT
                                     └─ java $JAVA_OPTS -jar app.jar  ← 실제 사용
```

#### 왜 `env_file`만으로도 동작했나?
`env_file: .env`는 `.env` 파일의 **모든 키-값**을 컨테이너 환경변수로 주입한다.  
따라서 `JAVA_OPTS`도 이미 컨테이너에 전달되고 있었으며, Dockerfile ENTRYPOINT인 `java $JAVA_OPTS -jar app.jar`가 이를 참조한다.

#### 그렇다면 왜 `environment`에 추가했나?
- **가시성**: `environment` 섹션은 "이 서비스에서 중요하게 동작하는 변수"를 명시하는 역할이다. JAVA_OPTS처럼 JVM 동작에 직접 영향을 주는 설정은 명시적으로 보이게 하는 것이 좋다.
- **의도 명확화**: `env_file`에 묻혀 있으면 유지보수 시 발견하기 어렵다.
- **우선순위**: `environment` 섹션은 `env_file`보다 우선순위가 높으므로, compose 파일 수준에서 직접 오버라이드 가능하다.

---

## 3. IMAGE_NAME이 어떻게 주입되는가

`docker-compose-server.yml`에서 `${IMAGE_NAME}`은 **Docker Compose의 variable substitution** 기능으로 치환된다.

### 동작 원리
Docker Compose는 compose 파일 실행 시 **같은 디렉토리의 `.env` 파일**을 자동으로 읽어 `${}` 변수를 치환한다.  
이는 `env_file`과 다른 별도 메커니즘이다.

| 기능 | 역할 | 시점 |
|------|------|------|
| `env_file: .env` | 컨테이너 내부 환경변수 주입 | 컨테이너 실행 시 |
| `.env` 자동 로드 | compose 파일 내 `${}` 변수 치환 | compose 파싱 시 |

### CI/CD 주입 흐름 (`.github/workflows/cicd.yml`)

```
build job
  └─▶ Set image name step
        └─▶ OWNER=$(echo '...' | tr 'upper' 'lower')
              └─▶ image=ghcr.io/$OWNER/jazzify-backend  (GITHUB_OUTPUT에 저장)

deploy job
  └─▶ Create .env file step
        ├─ echo "${{ secrets.ENV_FILE }}" > .env       # prod용 설정 전체
        └─ echo "IMAGE_NAME=${{ needs.build.outputs.image }}" >> .env  # ← 여기서 주입

  └─▶ Copy files to server (scp)
        └─ docker-compose-server.yml + .env → 서버 /home/ubuntu/

  └─▶ Deploy to server (ssh)
        └─ docker compose -f docker-compose-server.yml up -d
              └─ Compose가 /home/ubuntu/.env 를 읽어 ${IMAGE_NAME} 치환
```

### IMAGE_NAME 실제 값 예시
```
ghcr.io/dksvl/jazzify-backend:latest
```
(GitHub 계정명을 소문자로 변환한 형태)

---

## 4. 개발자가 알아둬야 하는 내용

### 로컬 개발 시
`docker-compose-server.yml`을 로컬에서 직접 실행하려면 `.env` 파일에 `IMAGE_NAME`을 수동으로 추가해야 한다.

```bash
# 예시 – 로컬 빌드 이미지 사용 시
echo "IMAGE_NAME=ghcr.io/your-username/jazzify-backend" >> .env
```

### secrets.ENV_FILE 관리
GitHub Secret `ENV_FILE`에는 `.env.prod.example`을 참고해 실제 값을 채운 내용을 저장한다.  
`JAVA_OPTS`도 이 Secret에 포함시켜야 prod 서버에서 JVM 튜닝이 적용된다.

### JAVA_OPTS 기본값 (Dockerfile)
`.env`에 `JAVA_OPTS`가 없더라도 Dockerfile의 `ENV JAVA_OPTS=...` 기본값이 사용된다.

```dockerfile
ENV JAVA_OPTS="-Xms256m -Xmx512m \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:+OptimizeStringConcat \
  -Djava.security.egd=file:/dev/./urandom"
```

prod 서버에서는 `.env`의 `JAVA_OPTS`가 이 기본값을 오버라이드한다.

### env_file vs environment 우선순위
```
environment 섹션 > env_file > Dockerfile ENV
```
우선순위가 높을수록 나중에 적용되어 이전 값을 덮어씀.

