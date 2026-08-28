# Repository Guidelines

## Project Structure & Module Organization

This repository contains an in-progress Java 21/Spring Boot 4.1 application. Production code lives in `src/main/java/com/example/youtubebot` and is organized by feature: `context`, `generation`, `oauth`, `publishing`, and `security`. Keep new code with the feature that owns it instead of recreating a broad `persistence` or generic `service` package.

Configuration and Thymeleaf templates live in `src/main/resources`; append-only Flyway migrations live in `src/main/resources/db/migration`. Tests mirror the production packages under `src/test/java`, with shared PostgreSQL Testcontainers setup in `support`. The documents in `docs/` define the target MVP and policy constraints; verify the current implementation in source and tests before assuming a planned feature exists.

## Build, Test, and Development Commands

Use the committed Gradle Wrapper rather than a system Gradle installation. Java 21 and Docker Desktop are required.

- `docker compose config --quiet` — validate the pinned PostgreSQL Compose configuration.
- `.\gradlew.bat test --no-daemon` — run unit, MVC, and Testcontainers integration tests.
- `.\gradlew.bat build --no-daemon` — reproduce the CI compile, test, and package gate.
- `git diff --check` — detect whitespace errors before committing.
- `docker compose up -d postgres` followed by `.\gradlew.bat bootRun` — run the application locally after setting the environment variables documented in `README.md`.

Tests start an isolated PostgreSQL container and do not require the Compose database. Do not start or restart the application, Compose services, or Ollama unless the task requires it. `ollama pull qwen3:4b` is an optional setup step for the planned generation flow, not part of the build.

## Coding Style & Naming Conventions

Use four-space indentation for Java and two spaces for YAML. Follow Java conventions: `UpperCamelCase` types, `lowerCamelCase` methods and fields, and lowercase package names. Name Spring components by responsibility, such as `GoogleOAuthService` or `CommentPublishService`.

Preserve the type-safe domain style established in the current code: use enums for states, records or domain types for structured JSON and creation inputs, narrow transition methods such as `markPublishing()`, and read projections where callers do not need mutable entities. Avoid stringly typed states, long positional constructors, broad setters, and unstructured JSON strings.

Name Flyway files `V<N>__description.sql`. Never rewrite an applied migration; add the next version and keep JPA configured with `ddl-auto: validate`. When upgrading PostgreSQL, keep the pinned image and digest in `compose.yml` and the shared Testcontainers base aligned. No formatter or linter is configured, so keep changes IDE-formatted and avoid unrelated reformatting.

## Testing Guidelines

Use JUnit tests ending in `Test` for focused unit or HTTP-client tests and `IT` for Spring/Testcontainers integration tests. Extend `PostgreSqlIntegrationTest` when a test needs the real Flyway/PostgreSQL stack. Use the existing `MockRestServiceServer` pattern for `RestClient` tests and gateway stubs for service or MVC integration tests; use a wire-level mock server only when the protocol behavior requires it.

Automated tests must never contact real Google, YouTube, or Ollama endpoints and must never publish a real comment. Cover URL validation, OAuth state and PKCE, AES-GCM failures, structured AI output, CSRF, duplicate-post guards, concurrent publishing, and ambiguous publish results as the corresponding features are implemented. Run the focused tests while iterating and `.\gradlew.bat build --no-daemon` before opening a PR.

## Security and Product Guardrails

- Use official Google and YouTube APIs. Do not add cookie-based login, service accounts for user actions, transcript scraping, or unofficial download endpoints.
- Request only the documented `youtube.force-ssl` scope and identify the fixed author channel with `channels.list(mine=true)`. Changing the author channel requires disconnecting and reconnecting.
- Treat AI output as a draft. Display the target video, author channel, and final text, then require explicit approval for every publish request.
- Never automatically retry `commentThreads.insert`; keep ambiguous results in an `UNKNOWN`-style state for manual resolution and preserve the per-video duplicate guard.
- Keep the application, PostgreSQL, and Ollama on loopback interfaces. Do not add a cloud AI fallback or grant Ollama tools, file access, shell access, or network access.
- Never commit or log API keys, OAuth tokens, client secrets, encryption keys, or generated credentials. Protect `YOUTUBE_TOKEN_ENCRYPTION_KEY`, `YOUTUBE_TOKEN_ENCRYPTION_KEY_VERSION`, `GOOGLE_OAUTH_CLIENT_ID`, and `GOOGLE_OAUTH_CLIENT_SECRET`; store only the encrypted refresh token, not access tokens.
- Do not persist public-comment author names, profile images, or channel IDs. Refresh or delete cached YouTube API data within the policy window described in `docs/comment-context-sources.md`.

## Documentation Guidelines

Keep `README.md` synchronized with executable setup and environment-variable changes. Update the implementation plan or context-source document when behavior changes a documented policy, API boundary, retention rule, OAuth scope, or MVP acceptance criterion. Distinguish planned behavior from implemented behavior.

## Commit & Pull Request Guidelines

Follow the repository's Conventional Commit history: `feat: Add youtube parser`, `fix: Reject playlist URLs`, `refactor: Introduce typed creation inputs`, or `docs: Update contributor guidance`. Keep commits focused and stage only the intended files.

Use a fresh branch for each logical change; do not reuse a merged branch for unrelated work. Give the PR a descriptive Conventional Commit-style title rather than a branch name. The PR body must explain behavior and risk, link the relevant plan section or issue, list verification performed, and include screenshots for Thymeleaf UI changes. Call out schema, OAuth scope, environment-variable, dependency, pinned-image, and external-API changes explicitly.
