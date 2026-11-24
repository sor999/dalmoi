# 달모이 Dalmoi

달모이는 지도에서 직접 그린 코스를 크루원과 공유하고, 같은 코스로 함께 달리는 Android 앱입니다. 그룹의 위치를 확인하면서도 각자 원하는 시점에 출발하고, 주행을 마치면 자신의 이동 거리·시간·경로를 돌아보는 경험을 목표로 합니다.

[GitHub 저장소](https://github.com/sor999/dalmoi)

## 서비스 기획

### 핵심 사용자 흐름

1. **코스 만들기** — 지도에서 출발점과 도착점, 경로를 그리는 데 필요한 좌표를 찍고 예상 거리를 확인한 뒤 저장합니다. 첫 점과 마지막 점을 포함해 2~50개 좌표를 사용합니다. 예상 거리는 선택한 좌표를 이은 길이이며 실제 보행로와 다를 수 있습니다.
2. **크루 만들기·가입** — 크루장이 초대코드를 공유하고, 친구가 코드로 비공개 크루에 가입합니다. 크루장 포함 최대 10명까지 참여할 수 있습니다.
3. **그룹 러닝 시작** — 크루장이 공통 코스를 선택해 대기방을 열고, 크루원은 대기 중에 참여합니다. 크루장이 그룹을 개시하면 참가자는 각자 개인 주행을 시작합니다.
4. **위치 공유·기록 확인** — 기획상 주행 중 위치는 같은 러닝 세션의 주행 중 참가자끼리 공유합니다. 개인 주행을 종료하면 공유가 끝나며, 정산된 거리·시간·실제 이동 경로는 본인만 조회합니다.

상세 요구사항과 화면 흐름, MVP 검수 기준은 [달모이 기능 요구사항 정의서(SRS)](https://app.notion.com/p/hyunje/Dalmoi-SRS-4151e1ec7daf8047a86ced015e0d3521)에서 확인할 수 있습니다.

### 현재 구현 범위

- **Android** — 지도에서 코스 생성·저장, 크루 생성·가입, 그룹 러닝 참여·시작·종료, 개인 기록 조회를 제공합니다. 주행 시작 시 정확한 위치 권한을 요청하며, 주행 화면에는 저장된 코스 경로가 표시됩니다.
- **백엔드** — GPS 이벤트 검증·저장, 최신 위치 조회, WebSocket 위치 전파, 주행 종료 후 기록 정산을 제공합니다. 현재 구현은 단일 서버와 DB 정산 작업을 사용합니다. Kafka 도입은 후속 범위입니다.
- **후속 연동** — Android에서 주행 좌표를 수집해 서버로 보내고, 참가자의 실시간 위치를 지도에 표시하는 기능은 아직 연결되지 않았습니다. 백그라운드 주행과 오프라인 기록 동기화도 후속 범위입니다.

## 저장소 구성

- `android/`에는 Kotlin·Jetpack Compose 앱과 네이버 지도 연동이 있습니다.
- `backend/`에는 Java 25·Spring Boot API가 있습니다. Spring Security와 JWT로 인증하고, PostgreSQL/PostGIS에 코스와 기록을 저장합니다. Redis는 최신 위치 캐시에 사용하며 WebSocket/STOMP로 위치 이벤트를 전파합니다.

## 로컬 실행

### 백엔드

JDK 25, Docker Compose, OpenSSL이 필요합니다. PostgreSQL/PostGIS와 Redis는 Compose로 띄웁니다.

```bash
cd backend
[ -f .env ] || cp .env.example .env
set -a
. ./.env
set +a
export JWT_SECRET="${JWT_SECRET:-$(openssl rand -base64 32)}"
docker compose up -d --wait
./gradlew bootRun
```

백엔드는 기본 포트 `8080`에서 실행됩니다. API 명세는 [Swagger UI](http://localhost:8080/swagger-ui.html)에서 확인할 수 있습니다.

### Android

Android Studio에서 `android/`를 열고 JDK 25와 Android SDK 37을 설정합니다. 네이버 지도를 사용하려면 `android/local.properties`에 Maps **클라이언트 ID**를 설정합니다.

```properties
dalmoi.naverMapKeyId=YOUR_NAVER_MAPS_CLIENT_ID
```

에뮬레이터에서는 Android Studio에서 앱을 실행합니다. 디버그 앱의 기본 API 주소는 `http://10.0.2.2:8080/api/v1/`입니다.

실제 기기에서 Tailscale로 테스트할 때는 휴대폰과 백엔드 호스트를 같은 Tailnet에 연결합니다. Tailscale Serve로 터널 내부 HTTPS 주소를 `http://127.0.0.1:8080`에 연결하고, 그 HTTPS 주소를 빌드에 지정합니다.

```bash
cd android
./gradlew :app:assembleDebug \
  -Pdalmoi.apiBaseUrl=https://YOUR-BACKEND.YOUR-TAILNET.ts.net/api/v1/
```

APK는 `android/app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다.

## 검증

각 명령은 해당 프로젝트 디렉터리에서 실행합니다. 백엔드 통합 테스트에는 Docker가 필요합니다.

| 경로 | 명령 |
| --- | --- |
| `backend/` | `./gradlew test spotlessCheck checkstyleMain checkstyleTest bootJar` |
| `android/` | `./gradlew :app:testDebugUnitTest spotlessCheck androidLint :app:assembleDebug` |

개발 지침과 화면 스타일은 [AGENTS.md](AGENTS.md)와 [디자인 가이드](DESIGN.md)를 참고하세요.
