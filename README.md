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

## Google OAuth 설정

Google Cloud Console에서 OAuth 동의 화면과 웹 애플리케이션 클라이언트를 만들고 승인된 리디렉션 URI를 `http://127.0.0.1:8080/oauth/callback`으로 등록합니다. 클라이언트 값은 소스에 기록하지 않고 실행 환경에 주입합니다.

```powershell
$env:GOOGLE_OAUTH_CLIENT_ID = "발급받은-client-id"
$env:GOOGLE_OAUTH_CLIENT_SECRET = "발급받은-client-secret"
```

서버를 시작한 뒤 `http://127.0.0.1:8080/oauth`에서 사용자가 직접 Google 연결을 시작합니다. 애플리케이션은 `youtube.force-ssl` 범위만 요청하며, 인증된 YouTube 채널의 ID와 이름을 작성 채널로 고정합니다. 작성 채널을 바꾸려면 화면에서 Google 연결을 해제한 뒤 다른 계정으로 다시 연결해야 합니다. 액세스 토큰은 저장하지 않고 refresh token만 AES-GCM으로 암호화해 PostgreSQL에 저장합니다.

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
