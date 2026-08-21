# YouTube Comment Bot

Java 21과 Spring Boot 4.1.0으로 만드는 로컬 YouTube 댓글 작성 보조 도구입니다.

## 로컬 실행

필수 도구는 Java 21과 Docker Desktop입니다. PostgreSQL을 먼저 실행한 다음 Spring Boot를 시작합니다.

```powershell
docker compose up -d postgres

$keyBytes = [byte[]]::new(32)
$tokenKeyRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$tokenKeyRng.GetBytes($keyBytes)
$tokenKeyRng.Dispose()
$env:YOUTUBE_TOKEN_ENCRYPTION_KEY = [Convert]::ToBase64String($keyBytes)
[Array]::Clear($keyBytes, 0, $keyBytes.Length)

.\gradlew.bat bootRun
```

`YOUTUBE_TOKEN_ENCRYPTION_KEY`는 실행할 때마다 바꾸면 기존 refresh token을 복호화할 수 없습니다. 실제 OAuth 연결 정보를 유지하려면 처음 생성한 32바이트 키를 운영체제의 안전한 비밀 저장소에 보관하고 동일한 Base64 값을 주입해야 합니다. 키를 교체할 때는 `YOUTUBE_TOKEN_ENCRYPTION_KEY_VERSION`도 올리고 저장된 토큰을 다시 암호화해야 합니다.

애플리케이션과 데이터베이스는 각각 `127.0.0.1:8080`, `127.0.0.1:5432`에만 바인딩됩니다. 서버와 데이터베이스 연결 상태는 다음 명령으로 확인할 수 있습니다.

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
```

종료할 때는 실행 중인 Spring Boot 프로세스를 중단하고 PostgreSQL 컨테이너를 내립니다. 데이터는 `postgres-data` 볼륨에 유지됩니다.

```powershell
docker compose down
```

## 빌드와 테스트

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

구현 범위와 정책 결정은 [구현 계획](docs/youtube-comment-bot-implementation-plan.md)과 [댓글 컨텍스트 출처](docs/comment-context-sources.md)를 참고하세요.
