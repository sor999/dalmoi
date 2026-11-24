# Android 코드 형식 자동화

## 실행

다음 명령은 `android/`에서 실행한다. 현재 wrapper는 Gradle 9.8.2이며 실행 JDK 17 이상이 필요하다. 현재 CI의 JDK는 25이다. 이는 형식 검사 환경이며 추후 AGP·Kotlin·Compose 버전은 앱 생성 시 호환성을 확인해 결정한다.

```sh
./gradlew spotlessApply
./gradlew spotlessCheck
./gradlew androidLint
```

Spotless 8.11.0과 ktlint 1.8.0을 사용한다. 버전은 `gradle/libs.versions.toml`에 고정하고 `.editorconfig`를 Android Studio와 공유한다. Kotlin 파일과 Gradle Kotlin DSL을 대상으로 하며 빌드 출력·생성 코드는 제외한다.

네 칸 공백, LF, 파일 끝 개행, 120자 줄 길이 및 trailing comma 규칙을 적용한다. `@Composable` 함수의 PascalCase 이름을 허용한다. 의미 단위별 빈 줄은 리뷰로 확인한다. Java/Spring 전용 애너테이션·예외 선언 규칙을 Kotlin에 그대로 적용하지 않는다.

## 커밋 훅

저장소 루트에서 `./scripts/install-git-hooks.sh`를 한 번 실행한다. 기존 공통 훅이 설치되어 있다면 다시 설정할 필요 없다. 훅은 스테이징된 스냅샷에 대해 변경 영역별 검사를 실행한다.

- 백엔드 변경: `spotlessCheck checkstyleMain checkstyleTest`
- Android 변경: `spotlessCheck`
- 양쪽 변경: 두 검사 모두 실행

훅은 자동 수정·자동 스테이징하지 않는다. 실패하면 해당 영역에서 `spotlessApply`로 수정 후 필요한 변경을 다시 스테이징한다. Android Lint는 SDK가 필요한 검사이므로 커밋 훅 대신 CI에서 실행한다.

## CI와 Android Lint의 현재 범위

`.github/workflows/android-quality.yml`에서 `spotlessCheck androidLint`를 실행한다. `androidLint`는 `com.android.application` 또는 `com.android.library` 플러그인이 적용된 모듈들의 실제 `lint` task를 실행한다.

현재는 앱 모듈이 없으므로 사유를 출력하고 SKIPPED 처리한다. Android Lint·APK 빌드·앱 테스트는 아직 실행 가능한 상태가 아니며, Kotlin 형식 검사 성공과 구분한다. 앱을 생성할 때 `settings.gradle.kts`에 모듈을 포함하고 AGP/SDK를 설정하면 해당 모듈의 Lint가 자동 연결된다.

CI를 머지 필수 검사로 강제하려면 저장소 브랜치 규칙에 `Android quality / quality`를 등록한다. detekt는 이번 설정에 포함하지 않는다.
