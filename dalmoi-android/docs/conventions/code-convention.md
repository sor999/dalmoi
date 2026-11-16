# 달모이 Android 개발 컨벤션

## 1. 적용 범위와 기준

Android 앱의 기본 스택, MVVM 구조, 상태 관리 및 코드 작성 규칙을 정의한다. 현재 `dalmoi-android`에는 앱 소스·Gradle 프로젝트가 없으므로 아래 스택은 **첫 구현에 적용할 기본안**이며 설치·빌드 완료 상태를 뜻하지 않는다. 실제 도입 버전과 SDK 범위는 프로젝트 생성 시 호환성을 검증해 Gradle에 기록한다.

- 공통 작업 규칙은 [Android 지침](../../AGENTS.md)과 루트 지침을 따른다.
- Android 앱의 코드 작성, 계층별 책임, 의존성 방향과 상태 관리 방식을 다룬다.
- Android 앱과 Spring Boot 서버의 빌드 및 기술별 컨벤션은 독립적으로 관리한다.
- 현재 요구사항에 필요하지 않은 모듈, 공통 Base 클래스, 라이브러리와 추상화는 추가하지 않는다.
- 기능·API의 기준은 [SRS](https://www.notion.so/4151e1ec7daf8047a86ced015e0d3521), [API 명세서](https://www.notion.so/4191e1ec7daf80fa9e90f63efcaa39e4), [Figma](https://www.figma.com/design/nJh1EjT7VOKrswZ6zLV3Pw/Untitled?node-id=1-289)다. 계약 변경 시 서버 영향도 함께 확인한다.

## 2. 기본 기술 스택

| 영역 | 사용 기준 | 적용 방식 |
| --- | --- | --- |
| 언어·빌드 | Kotlin, Gradle Kotlin DSL, Version Catalog | 버전은 `gradle/libs.versions.toml`에서 관리 |
| UI | Jetpack Compose + Material 3 | Single Activity, Route와 Screen 분리 |
| 아키텍처 | MVVM + 단방향 데이터 흐름 | presentation/domain/data 책임 분리 |
| 화면 상태 | StateFlow | 읽기 전용 `StateFlow<UiState>` 노출, 내부 `MutableStateFlow` 관리 |
| 비동기 | Coroutines + Flow | 업무 작업은 `viewModelScope`, 지속 데이터는 Flow로 제공 |
| HTTP·JSON | Ktor Client + OkHttp + kotlinx.serialization | REST API 호출과 DTO 직렬화 설정 중앙화 |
| 의존성 주입 | 생성자 주입 + 수동 DI | AppContainer와 AndroidX ViewModel Factory에서 조립 |
| 내비게이션 | AndroidX Navigation Compose | 타입이 있는 목적지와 식별자 전달 |
| 로컬 DB | 필요 시 Room + KSP | 영속 저장 요구가 있는 데이터에 한해 도입 |
| 이미지 | 필요 시 Coil | 이미지 로딩 요구가 있을 때 도입 |
| 테스트 | JVM 단위 테스트, kotlinx-coroutines-test, Ktor MockEngine, Compose UI 테스트 | 도메인·상태·API 경계·사용자 동작별 검증 |

달모이는 Android 단일 플랫폼과 단일 `app` 모듈을 기준으로 구성한다. DI와 HTTP 도구를 변경할 때는 필요성과 영향 범위를 기록하고 같은 역할의 라이브러리를 중복 도입하지 않는다.

추가 SDK와 플랫폼 구현체는 도입 시 선정 이유와 사용 범위를 기록하고 계층 경계를 유지한다. compileSdk·targetSdk·minSdk·JDK·AGP·Kotlin·Compose 버전은 지원 기기 범위와 라이브러리 호환성을 검증한 뒤 Gradle 설정에 명시한다.

## 3. 패키지와 의존성 방향

초기에는 단일 `app` 모듈에서 패키지로 책임을 분리한다. Gradle 멀티모듈 전환은 기능 규모나 빌드·소유권 분리가 필요할 때 검토한다. 아래는 생성할 구조의 예시이며 모든 디렉터리를 미리 만들 필요는 없다.

```text
com.sor999.dalmoi
├── app/                         # Application, MainActivity, AppContainer, Factory
├── navigation/                  # NavHost, 목적지 타입
├── presentation/
│   ├── course/                  # auth, crew, session, record도 같은 방식
│   │   ├── route/
│   │   ├── screen/
│   │   ├── state/
│   │   ├── viewmodel/
│   │   └── component/           # 해당 기능에서만 쓰는 UI
│   └── common/                  # 실제로 공유하는 theme, component, formatter
├── domain/
│   ├── model/
│   ├── repository/              # 인터페이스
│   └── usecase/                 # 필요한 업무 규칙·조합
├── data/
│   ├── remote/                  # HTTP, WebSocket, DTO, 매퍼
│   ├── local/                   # 도입 시 저장소·Entity·DAO
│   └── repository/              # domain Repository 구현
└── platform/                    # 위치·권한 등 Android SDK 경계
```

- 의존 방향은 `presentation → domain`, `data → domain`이다. `app`에서 구현체를 조립한다.
- domain은 Compose, Android Context, 지도 SDK, HTTP DTO, Room Entity에 의존하지 않는다.
- ViewModel은 Repository 인터페이스 또는 UseCase를 받는다. HTTP·DB·지도 구현체를 직접 생성하지 않는다.
- 단순 조회는 ViewModel에서 Repository 인터페이스를 호출해도 된다. 여러 Repository 조합, 재사용되는 업무 규칙, 검증은 UseCase로 분리한다. 모든 호출을 감싸는 전달용 UseCase를 의무화하지 않는다.
- UI 공통화는 실제 재사용과 동일한 동작이 확인된 뒤 수행한다. 기능 코드를 무조건 `common`, `util`로 옮기지 않는다.

## 4. MVVM과 화면 책임

데이터 흐름은 `사용자 입력 → ViewModel 함수 → Repository/UseCase → UiState → Screen`으로 유지한다.

| 구성 | 책임 | 피해야 할 일 |
| --- | --- | --- |
| Activity | 앱 진입, 테마, NavHost와 Android 진입점 연결 | 화면별 업무 로직·API 호출 |
| Route | ViewModel 획득, lifecycle에 맞춘 상태 수집, 권한 요청·내비게이션 연결 | 거리 계산·응답 DTO 해석 |
| Screen | 전달받은 상태 렌더링과 콜백 호출 | ViewModel·Repository 직접 접근 |
| ViewModel | 사용자 의도 처리, 화면 상태 전이, 작업 취소·중복 제어 | Activity·NavController·지도 View 보관 |
| Repository | 원격·로컬 데이터 접근 및 도메인 모델 반환 | UI 문자열 포맷·화면 이동 |
| UseCase | 여러 작업 조합과 재사용되는 업무 규칙 | Android UI 의존 |

- `CourseEditorRoute`, `CourseEditorScreen`, `CourseEditorViewModel`, `CourseEditorUiState`처럼 화면 역할이 드러나는 이름을 쓴다.
- Route는 `collectAsStateWithLifecycle()`로 상태를 수집한다. Screen에는 불변 상태와 `onSaveClick`, `onUndoClick` 같은 콜백을 전달한다.
- Screen은 ViewModel 없이 Preview와 UI 테스트에서 실행할 수 있게 한다.
- 다이얼로그 열림, 스크롤 등 화면 안에서만 의미 있는 상태는 `remember`/`rememberSaveable`로 관리할 수 있다. 저장·검증·서버 상태는 ViewModel에서 관리한다.
- Composable 본문에서 요청을 실행하지 않는다. 최초 로드는 ViewModel 초기화 또는 ID를 key로 한 `LaunchedEffect` 중 한 곳에서 시작하며 재구성으로 중복 요청하지 않는다.
- ViewModel은 AndroidX 수명주기 소유자와 Factory를 통해 획득한다. Composable 재구성마다 생성하지 않는다.

## 5. UiState와 이벤트

- ViewModel은 `private val _uiState`와 읽기 전용 `val uiState = _uiState.asStateFlow()`를 제공한다. 외부에서 상태를 수정하지 않는다.
- 화면 데이터는 `data class`, 상호 배타적인 로딩·성공·실패 단계는 `sealed interface` 등으로 표현한다. 여러 Boolean의 모순되는 조합을 피한다.
- `_uiState.update { it.copy(...) }`로 새 상태를 발행한다. 발행한 `List`를 가변 리스트로 캐스팅하거나 내부 컬렉션을 직접 수정하지 않는다.
- `onTitleChanged()`, `saveCourse()`, `undoLastPoint()`처럼 사용자의 의도를 표현한다. 외부에 범용 setter를 공개하지 않는다.
- 저장 중에도 기존 입력과 경로를 유지하고 버튼을 비활성화해 중복 요청을 방지한다. 실패 시 입력을 지우지 않는다.
- 저장 성공·권한 거부 등 중요한 결과는 상태로 남긴다. 화면 이동이 필요하면 결과 ID를 상태에 담고 Route에서 처리 후 확인 함수를 호출한다. 중복 처리 여부를 명시한다.
- Snackbar 등 유실을 허용하는 일시 알림만 effect 스트림으로 보낼 수 있다. SharedFlow/Channel이 비활성 화면에 반드시 전달된다고 가정하지 않는다.
- 회전에는 ViewModel, 작은 복원 값에는 `SavedStateHandle`을 사용한다. ViewModel이 프로세스 종료까지 보존된다고 가정하지 않으며 대량 GPS 이력은 saved state에 넣지 않는다.

## 6. 코루틴과 Flow

- 화면 업무 작업은 `viewModelScope.launch`에서 시작하고 데이터 계층은 main-safe `suspend` 함수 또는 `Flow`로 제공한다.
- 블로킹 파일·DB·계산 작업만 적절한 Dispatcher로 이동한다. 테스트에서 제어해야 하는 Dispatcher와 시계는 주입한다. suspend라는 이유만으로 모두 `Dispatchers.IO`로 감싸지 않는다.
- `GlobalScope`, 임의의 영구 CoroutineScope를 만들지 않는다. 장기 위치 수집에는 별도 소유자와 명확한 시작·종료 수명주기를 둔다.
- `CancellationException`은 다시 던진다. `runCatching`으로 취소를 일반 오류로 삼키지 않는다.
- 검색·재조회는 이전 작업 취소나 최신 요청 식별로 늦은 응답이 새 상태를 덮어쓰지 않게 한다. 저장은 작업 가드로 중복 실행을 차단한다.
- 위치 콜백을 Flow로 감쌀 때 구독 해제 시 콜백을 제거한다. 재구독마다 WebSocket·GPS 수집이 중복 시작되지 않게 한다.

## 7. 네트워크·DTO·오류

- Ktor Client 설정은 한 곳에서 구성하고, 연결·요청 timeout과 종료 책임을 명시한다. 화면마다 HttpClient를 생성하지 않는다.
- 요청은 `CreateCourseRequest`, 응답은 `CourseResponse`처럼 역할을 구분하고 `@Serializable`은 전송 DTO에 둔다. JSON 필드명 차이는 `@SerialName`으로 표현한다.
- DTO → domain 변환은 data 계층의 `toDomain()`, domain → 요청은 `toRequest()` 같은 명시적 매퍼에서 수행한다. Screen과 ViewModel에 DTO를 전달하지 않는다.
- Repository의 일회성 실패 가능 작업은 `suspend fun ...: Result<T>`를 기본으로 한다. Flow에는 문서화된 오류 상태 또는 처리 규칙을 둔다. 예외·Result·null을 같은 실패 의미로 혼용하지 않는다.
- 연결 실패, HTTP 오류, 응답 파싱 실패, 인증 만료를 구분하고 서버의 `code`를 해석한다. 원본 예외 메시지를 사용자에게 그대로 표시하지 않고 UI 메시지로 변환한다.
- 401 처리·토큰 삭제·로그인 이동의 책임을 중앙화한다. 서버 명세에 없는 토큰 갱신 API를 가정하지 않는다.
- POST 저장·참여·종료 요청을 무조건 자동 재시도하지 않는다. 멱등성 보장이 없는 요청은 결과 확인과 사용자 재시도 동작을 설계한다.
- 날짜·시간은 API의 UTC/ISO 8601 계약을 유지하고 표시 단계에서 사용자 시간대로 변환한다. 거리·시간 숫자를 계층 간에 포맷 문자열로 전달하지 않는다.
- 운영용 앱에 Fake 응답을 정상 서버 응답처럼 연결하지 않는다. Preview·테스트의 Fake는 명시적으로 분리한다.

## 8. 의존성 주입과 로컬 저장

- 생성자 주입을 기본으로 하고 AppContainer에서 HTTP Client·Repository·UseCase·Factory를 조립한다. ViewModel 내부에서 전역 컨테이너를 조회하지 않는다.
- 앱 수명과 화면 수명을 구분한다. Activity 재생성이나 ViewModel 종료로 다른 화면이 사용하는 앱 범위 Client를 닫지 않는다.
- Context가 필요한 구현체는 플랫폼/data 경계에서 Application Context를 받는다. 화면 ViewModel에 Activity Context를 전달하지 않는다.
- Room은 오프라인 저장 등 영속 데이터 요구가 생기면 도입한다. Entity를 UI에 직접 전달하지 않고 스키마 변경 시 마이그레이션을 검증한다.
- 토큰 저장은 별도 인터페이스로 감싸며 보안 요구에 맞는 구현을 선택한다. 일반 설정 저장소를 자격 증명 보호 수단으로 간주하지 않는다.
- base URL 등 환경값을 빌드 설정으로 분리한다. `local.properties`, 토큰, 키, 서명 파일은 커밋하지 않는다. 앱 바이너리에 포함한 값은 비밀로 보호된다고 가정하지 않는다.

## 9. Compose·리소스·네이밍

- 들여쓰기는 공백 4칸, 클래스·인터페이스·Composable은 `PascalCase`, 함수·변수·패키지는 역할에 맞는 `camelCase`/소문자를 사용한다. 상수 `const val`은 `UPPER_SNAKE_CASE`로 통일한다.
- `val`을 우선하고 가시성은 필요한 범위로 제한한다. `!!`, 와일드카드 import, 무의미한 축약, 불필요한 주석을 피한다.
- 매개변수가 길면 여러 줄과 trailing comma를 사용한다. 같은 타입 인자가 여럿이면 named argument로 의미를 드러낸다.
- 외부에서 배치할 Composable은 `modifier: Modifier = Modifier`를 받고 최상위 UI에 적용한다. 입력값·콜백·선택 인자의 순서를 일관되게 유지한다.
- 색상·타이포그래피·반복 간격은 테마 토큰을 사용한다. 사용자 표시 문자열은 Android string 리소스로 관리하고 접근성 설명·터치 영역·글자 확대·시스템 inset을 확인한다.
- 리소스 파일과 키는 `snake_case`, 기능 접두사를 사용한다. 예: `course_save`, `course_limit_reached`.
- ID·거리·시간은 `courseId`, `distanceMeters`, `durationSeconds`처럼 의미와 단위를 드러낸다. API의 km 값은 매핑 경계에서 명시적으로 변환한다.
- 화면 이동에는 `courseId`·`sessionId` 같은 작은 식별자를 전달하고 목적지에서 조회한다. 전체 경로·토큰·큰 객체를 내비게이션 인자로 넘기지 않는다.
- 포매터·lint 도구를 도입할 때 설정과 실행 명령을 함께 기록한다. 아직 설치하지 않은 ktlint·detekt를 필수 통과 도구로 기재하지 않는다.

## 10. 테스트와 검증

- domain의 검증·계산 로직, ViewModel 상태 전이, Repository 매핑·오류를 JVM 테스트로 검증한다. 네트워크·시계는 Fake/MockEngine과 테스트 시계로 제어한다.
- ViewModel 비동기 테스트는 `runTest`와 테스트 Dispatcher로 실행한다. 실제 지연이나 운영 서버에 의존하지 않는다.
- 성공뿐 아니라 로딩·실패·재시도·중복 저장·요청 취소·역순 응답을 확인한다. private 구현이나 호출 횟수만 확인하는 테스트보다 사용자 관찰 결과를 검증한다.
- Compose UI 테스트는 사용자 동작과 접근성 semantics를 확인한다. 플랫폼 SDK 연동·권한·화면 재생성·자원 해제는 필요한 기기/에뮬레이터 검증을 수행한다.
- Android 프로젝트가 구성되면 해당 디렉터리의 wrapper로 관련 테스트 → lint → debug 빌드를 실행한다. 아래는 `:app` 모듈 구성 후 사용할 예시이며 현재 실행 가능한 명령이 아니다.

```bash
cd dalmoi-android
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
# 기기·에뮬레이터가 필요한 변경
./gradlew :app:connectedDebugAndroidTest
```

모듈명과 task는 실제 Gradle 구성에 맞춰 갱신한다. 문서만 변경한 경우 링크·내용·변경 범위를 검토하고 앱 빌드 성공으로 보고하지 않는다. 완료 보고에는 실제 실행 명령·결과와 미검증 이유를 남긴다.
