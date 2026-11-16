# Spring Boot Backend Code Convention

## 1. 기본 원칙

아래 스타일은 기존 팀 규칙이 없는 경우의 기본안이다. Java 버전과 기존 아키텍처를 먼저 확인하며, Lombok·JPA·Security·OpenAPI 등 기술별 규칙은 해당 기술을 사용하는 경우에 적용한다. 이 문서만을 이유로 의존성이나 공통 타입을 추가하지 않는다.

- Java와 Spring Boot 기반 기존 코드 구조 및 팀 컨벤션을 우선한다.
- 기능별 도메인 패키지를 사용하고, 내부는 필요에 따라 `controller`, `service`, `repository`, `domain`, `dto`, `exception`으로 나눈다.
- 일반적인 의존 흐름은 Controller → Service → Repository·Domain이며, Domain은 Controller, Service, DTO에 의존하지 않는다.
- Entity를 API 응답으로 직접 반환하지 않는다.
- 공통 기술 요소는 `global`에 두고 도메인별 비즈니스 규칙은 해당 도메인에 둔다.
- 현재 요구사항에 필요하지 않은 추상화·의존성·계층을 추가하지 않는다.

## 2. Entity

- `@Getter`와 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`를 사용하고 `@Setter`는 지양한다.
- 객체 생성용 생성자는 `@Builder`를 적용한 `private` 생성자로 정의한다.
- 객체는 `public static create()` 팩토리 메서드 내부에서 `builder().build()`로 생성한다.
- 상태 변경에는 `update()`, `changeXXX()`, `completeXXX()` 등 의도가 드러나는 도메인 메서드를 사용한다.
- 소유권과 상태에 대한 검증은 가능한 경우 `validateOwner()`, `validateXXX()` 등 Entity 내부에 둔다.
- 상태 변경 규칙을 Service의 필드 조작으로 우회하지 않는다.
- 연관관계의 로딩 전략과 데이터 무결성 제약을 명시적으로 검토한다.

## 3. DTO

- DTO는 Java `record`를 우선 사용한다.
- 요청 DTO는 `XXXRequest`, 응답 DTO는 `XXXResponse`로 명명한다.
- Response DTO 생성은 정적 팩토리 메서드를 활용한다.
  - `from()`: 하나의 원본 객체를 DTO로 변환할 때 사용한다.
  - `of()`: 여러 객체나 값을 조합하여 DTO를 생성할 때 사용한다.
- 직접 생성자 호출보다는 정적 팩토리 메서드를 우선 사용한다.
- 요청 DTO에는 필요한 Jakarta Validation 제약을 선언한다.
- API 응답에는 Entity 자체나 내부 저장소 구현 정보를 노출하지 않는다.

## 4. Service

- `@Service`, `@RequiredArgsConstructor`를 사용하고 생성자 주입을 우선한다.
- 조회 중심 Service는 클래스에 `@Transactional(readOnly = true)`를 적용하고, 쓰기 메서드에는 `@Transactional`을 적용한다.
- Repository 필드명은 도메인명을 생략하지 않는다. 예: `orderRepository`.
- Service는 유스케이스의 흐름과 트랜잭션을 관리하고, Entity 상태 변경과 도메인 검증을 가능한 한 Entity에 위임한다.
- Response 생성은 DTO 정적 팩토리 메서드를 활용한다.
- 락은 정합성을 지키는 데 필요한 경우에만 사용하고, 잠금 대상·범위·획득 순서·트랜잭션 길이를 검토한다.
- 외부 호출과 DB 트랜잭션의 경계 및 실패 시 동작을 확인한다.

## 5. Controller 및 API

- REST API 기본 경로와 버전 정책은 기존 프로젝트 규칙을 따른다. 별도 규칙이 없는 신규 API에는 `/api/v1`을 기본안으로 사용한다.
- 인증 주체는 프로젝트의 기존 Security 설정과 principal 타입으로 식별한다. `Long` ID나 특정 회원 모델을 가정하지 않는다.
- 요청 DTO에 `@Valid`를 적용하고, 경로·쿼리 파라미터에도 필요한 검증을 선언한다.
- 기존 Controller와 HTTP 상태 코드 및 `ResponseEntity` 사용 방식을 일관되게 유지한다.
- OpenAPI/Swagger를 사용하는 프로젝트에서는 `@Operation`, `@ApiResponse` 등으로 요청·응답과 오류를 문서화한다.
- Controller에서는 요청 바인딩과 응답 반환에 집중하고 비즈니스 로직은 Service에 위임한다.

## 6. Repository 및 데이터베이스

- `JpaRepository`를 기본으로 사용한다.
- 단순 조회는 Spring Data 파생 쿼리를 우선 활용하고, 복잡한 조회는 `@Query` 또는 QueryDSL을 사용한다.
- 조회 메서드명은 조회 대상과 조건이 명확하게 드러나도록 작성한다.
- N+1, 불필요한 조회, 페이징 정렬 안정성, 인덱스 적용 가능성을 확인한다.
- FK, UNIQUE, CHECK, CASCADE 등 DB 제약조건과 애플리케이션 규칙의 일관성을 확보한다.
- 스키마 변경은 프로젝트의 기존 마이그레이션 도구와 방식을 따른다. Flyway를 사용한다면 해당 규칙에 맞춘다. 이미 공유 환경에 적용된 마이그레이션을 임의 수정하지 않는다.
- Soft Delete와 Hard Delete는 기존 관행만이 아니라 참조 데이터 보존 및 비즈니스 요구사항을 고려해 결정한다.

## 7. 예외 처리

- 도메인별 커스텀 예외는 프로젝트의 공통 비즈니스 예외 계층을 따른다. `BusinessException`이 있으면 이를 활용한다.
- 오류는 프로젝트의 오류 코드 체계와 전역 예외 응답 형식을 따른다. `ErrorCode`가 있으면 등록한다.
- 필수 엔티티 조회 실패는 `orElseThrow()` 등으로 명시적으로 처리한다.
- 예외를 무시하거나 성공 응답으로 위장하지 않는다.
- DB 제약 위반 등 동시성 경쟁에서 발생하는 예외가 적절한 API 결과로 이어지는지 확인한다.

## 8. 네이밍 및 코드 스타일

- `var` 및 불필요한 와일드카드 타입보다 명시적 타입을 우선한다.
- 클래스, 메서드, 변수의 이름은 역할과 도메인 의미가 드러나도록 작성한다.
- 기존 패키지 구조, import 형식, 메서드 이름, 공통 유틸리티 패턴을 우선 따른다.
- 설명 없이 반복되는 매직 넘버와 중복 로직을 지양한다.
- 불필요한 주석과 과도한 추상화를 만들지 않는다.
- 예외적인 설계 선택은 이유가 드러나도록 기록한다.

## 9. 검증 및 문서

- 변경 전후 API 계약, 권한 검증, 트랜잭션과 데이터 정합성 영향을 확인한다.
- 테스트 작성 규칙은 [`test-convention.md`](test-convention.md)를 따른다.
- PR 리뷰 절차는 `.agents/skills/review/SKILL.md`, 테스트 수행 절차는 `.agents/skills/test/SKILL.md`를 참조한다.
- 기존 구현과 이 문서가 충돌하면 팀 합의 여부를 확인하고, 규칙 변경이 필요하면 문서도 함께 갱신한다.
