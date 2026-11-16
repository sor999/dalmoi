---
name: review
description: Git 커밋 및 Pull Request의 변경사항을 분석하고, Spring Boot 프로젝트의 컨벤션을 기준으로 버그, 설계, 성능, 동시성, DB 정합성, 테스트 품질을 검토한다. 코드 리뷰 또는 PR 리뷰 요청 시 사용한다.
---
## 리뷰 절차

1. `git diff` 또는 PR 변경사항과 변경 목적을 확인한다.
2. 관련 Entity, Service, Repository, DTO, Controller, 테스트와 기존 유사 구현을 확인한다.
3. 프로젝트 컨벤션과 비교하고 변경으로 인한 영향 범위를 분석한다.
4. 실제 문제와 단순 개선 제안을 구분한다.

## 핵심 검토 항목

- **정합성:** 비즈니스 규칙, 예외 처리, null, 경계값, 데이터 무결성
- **동시성:** Race Condition, 트랜잭션, 락 필요성 및 범위, 데드락
- **성능:** N+1, 불필요한 쿼리, 인덱스, 락 경합
- **설계:** 책임 분리, 중복 코드, 추상화, 유지보수성
- **DB:** FK, UNIQUE, CASCADE, Soft Delete와 Hard Delete, Flyway
- **API:** HTTP 상태 코드, Validation, 인증 및 인가, 응답 일관성
- **테스트:** 정상 및 예외 시나리오, 경계값, 테스트 격리, 동시성 검증

## 프로젝트 컨벤션

아래 스타일은 기존 팀 규칙이 없는 경우의 기본안이다. Java 버전과 기존 아키텍처를 먼저 확인하며, Lombok·JPA·Security·OpenAPI 등 기술별 규칙은 해당 기술을 사용하는 경우에 적용한다. 이 문서만을 이유로 의존성이나 공통 타입을 추가하지 않는다.

### Entity

- `@Getter`, `@NoArgsConstructor(access = PROTECTED)` 사용하고 `@Setter`는 지양한다.
- `@Builder`가 적용된 `private` 생성자를 정의한다.
- 객체 생성은 `public static create()`에서 `builder().build()`로 반환한다.
- 상태 변경은 `update()`, `changeXXX()`, `completeXXX()` 등 도메인 메서드를 사용한다.
- 소유권과 상태 검증은 `validateOwner()`, `validateXXX()` 등 Entity 내부에서 처리한다.

### DTO

- DTO는 Java `record`를 우선 사용한다.
- 요청 DTO는 `XXXRequest`, 응답 DTO는 `XXXResponse`로 명명한다.
- Response DTO 생성은 정적 팩토리 메서드를 활용한다.
    - `from()`: 하나의 원본 객체를 DTO로 변환할 때 사용한다.
    - `of()`: 여러 객체나 값을 조합하여 DTO를 생성할 때 사용한다.
- 직접 생성자 호출보다는 정적 팩토리 메서드를 우선 사용한다.

### Service

- `@Service`, `@RequiredArgsConstructor`를 사용한다.
- 클래스에 `@Transactional(readOnly = true)`, 쓰기 메서드에 `@Transactional`을 적용한다.
- Repository 필드명은 축약 없이 전체 도메인명을 사용한다. 예: `orderRepository`
- 상태 변경과 도메인 검증은 가능한 Entity에 위임한다.
- 응답 변환은 DTO의 정적 팩토리 메서드를 활용한다.

### Controller

- REST API 경로와 버전은 프로젝트의 기존 정책을 따른다. 별도 규칙이 없는 신규 API에는 `/api/v1`을 기본안으로 사용한다.
- 인증 주체는 기존 Security 설정과 principal 타입을 따른다. 특정 ID 타입을 강제하지 않는다.
- Request DTO에 `@Valid`를 적용한다.
- 기존 Controller와 HTTP 상태 코드 및 `ResponseEntity` 사용 방식을 통일한다.
- OpenAPI/Swagger를 사용하는 프로젝트에서는 `@Operation`, `@ApiResponse`로 API 명세를 작성한다.

### Repository 및 예외

- `JpaRepository`를 기반으로 구현한다.
- 단순 조회는 파생 쿼리를 사용하고 복잡한 조회는 `@Query` 또는 QueryDSL을 활용한다.
- 커스텀 예외와 오류 코드는 기존 예외 계층 및 전역 응답 체계를 따른다. `BusinessException`, `ErrorCode`가 있으면 활용한다.
- 조회 실패는 `orElseThrow()`로 명시적으로 처리한다.

### 테스트

- Controller는 `@WebMvcTest`와 Mock 객체를 활용한다.
- Service 및 Repository 통합 테스트는 실제 운영 DB와 같은 엔진의 Testcontainers 또는 기존 테스트 인프라를 활용한다.
- 모든 테스트 메서드에 `@DisplayName`을 작성한다.
- `@DisplayName`은 테스트 목적과 기대 결과를 명확하게 표현한다.
- 정상, 예외, 경계값, 권한 시나리오를 검증한다.
- 동시성 테스트는 실제 락 경합과 결과를 검증한다.

### 코드 스타일

- `var`, 와일드카드보다 명시적 타입을 우선 사용한다.
- 메서드와 변수명은 역할이 명확하게 드러나도록 작성한다.
- 기존 패키지 구조, import 및 네이밍 규칙을 유지한다.
- 불필요한 주석과 중복 코드를 지양한다.

## 리뷰 원칙

- 변경 코드뿐 아니라 관련 기존 구현도 확인한다.
- 프로젝트 컨벤션을 일반적인 취향보다 우선한다.
- 실제 버그, 설계 개선, 스타일 제안을 구분한다.
- 불필요한 최적화와 과도한 추상화를 강요하지 않는다.
- 이미 반영되었거나 작성된 리뷰를 중복하지 않는다.
- 근거 없는 문제를 만들어내지 않는다.
- 기존 구현과 다른 부분은 이유를 확인한 후 개선을 제안한다.
- 컨벤션 위반은 기존 코드 또는 명시된 프로젝트 규칙을 근거로 판단한다.

## 출력 형식

각 리뷰는 다음 형식으로 작성한다.

- **우선순위:** 필수 / 권장 / 선택
- **위치:** 파일명 및 라인
- **문제:** 발생 원인과 실제 영향
- **개선:** 구체적인 수정 방향
- **리뷰 코멘트:** GitHub에 바로 작성할 수 있는 정중하고 간결한 한국어

치명적인 문제부터 출력하며, 문제없는 부분은 불필요하게 지적하지 않는다.
