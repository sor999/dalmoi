# 달모이 서버

Java 25와 Docker 엔진이 필요합니다. 아래 명령은 `backend/`에서 실행합니다.

## 로컬 실행

```bash
cp .env.example .env
set -a
source .env
set +a
docker compose up -d --wait postgres
./gradlew bootRun
```

Gradle과 실행 JAR는 `.env`를 자동으로 읽지 않으므로 셸이나 IDE 실행 설정에서 환경변수를 전달합니다. 기본값은 로컬 PostgreSQL 16 + PostGIS 3.5(`postgis/postgis:16-3.5`), DB `dalmoi_db`, 사용자 `postgres`입니다. 예제 비밀번호는 로컬 개발용이며 배포 환경에서는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 별도로 주입합니다.

로컬 DB 중지는 `docker compose down`으로 수행합니다. 데이터는 named volume에 보존됩니다.

## JPA와 스키마

- OSIV는 비활성화되어 있습니다. 연관 데이터 조회와 DTO 변환은 필요한 트랜잭션 안에서 수행합니다.
- `ddl-auto: validate`로 엔티티와 DB 스키마를 검증하며, Hibernate가 테이블을 자동 생성하거나 변경하지 않습니다.
- 초기 스키마는 `src/main/resources/db/migration/V1__create_mvp_tables.sql`에서 PostGIS 확장과 MVP 11개 테이블을 생성합니다. 이후 변경은 `V2__설명.sql`부터 추가합니다. 확장 설치에는 DB 사용자의 권한과 서버의 PostGIS 설치가 필요합니다.
- 엔티티는 `member`, `auth`, `course`, `crew`, `session`, `gps`, `record` 도메인 패키지에 있습니다. 연관관계는 단방향 LAZY이며, 연쇄 삭제를 설정하지 않습니다. 현재는 생성·저장 매핑 단계로 API와 서비스의 권한 검사·상태 전이·잠금 처리는 후속 구현 대상입니다.
- 공간 타입은 Hibernate Spatial/JTS의 `LineString`과 `Point`로 매핑합니다. SRID는 4326이고 좌표 순서는 경도(X), 위도(Y)입니다. 거리 정본은 미터이며, 세션에는 생성 시 코스·크루명 스냅샷을 보관합니다.
- 이미 적용된 마이그레이션은 수정하지 않고 새 버전을 추가합니다. 기존 DB를 자동 baseline 처리하지 않습니다.
- JDBC 시간대는 UTC이며, 연관관계 기본 batch fetch 크기는 100입니다.
- SQL 로그는 기본 비활성 수준입니다. 필요하면 `LOG_LEVEL_HIBERNATE_SQL=DEBUG`, `HIBERNATE_FORMAT_SQL=true`로 로컬에서 활성화합니다.

## PostgreSQL 통합 테스트

Docker 엔진을 실행한 상태에서 다음 명령을 사용합니다. 로컬 Compose DB는 필요하지 않습니다.

```bash
./gradlew test
```

통합 테스트에 `com.sor999.dalmoi.support.PostgresIntegrationTest`를 적용하면 `test` 프로필과 PostgreSQL 컨테이너 설정이 함께 적용됩니다. Spring이 컨테이너를 시작·종료하고 캐시된 동일 애플리케이션 컨텍스트에서는 컨테이너를 공유합니다. `@ServiceConnection`이 컨테이너의 동적 포트와 자격 증명을 연결하므로 로컬·배포 DB 연결 설정 대신 `dalmoi_test` DB를 사용합니다.

테스트에서도 Flyway와 `ddl-auto: validate`를 사용합니다. 일반 DB 통합 테스트는 필요에 따라 `@Transactional`로 롤백하고, 동시성 테스트는 독립된 트랜잭션과 별도 정리를 구성합니다. 테스트 클래스 간 DB 상태가 공유될 수 있으므로 데이터를 남기지 않습니다. Controller slice 테스트나 순수 단위 테스트에는 `@PostgresIntegrationTest`를 적용하지 않습니다. JPA slice 테스트에는 `@Import(PostgresTestContainerConfiguration.class)`, `@ActiveProfiles("test")`, `@AutoConfigureTestDatabase(replace = Replace.NONE)`을 조합합니다.

Docker가 없으면 통합 테스트를 건너뛰지 않고 실패합니다. 최초 실행에는 `postgis/postgis:16-3.5` 이미지 다운로드가 필요합니다.

엔티티 매핑과 마이그레이션 제약조건만 검증하려면 `./gradlew test --tests '*EntityMappingTest'`를 실행합니다. 전체 엔티티 저장·조회, 공간 좌표 복원, 공유 기본키, 계획 코스 스냅샷, 활성 가입·동시 주행·GPS 순번의 고유 제약 등을 확인합니다.

## 공통 예외 응답

Controller에서 처리되는 오류는 `global.exception.GlobalExceptionHandler`가 다음 형식으로 반환합니다.

```json
{
  "code": "INVALID_INVITE_CODE",
  "message": "유효하지 않은 초대코드입니다."
}
```

비즈니스 오류는 `ErrorCode`에 HTTP 상태, 문자열 코드와 메시지를 등록하고 다음과 같이 발생시킵니다.

```java
throw new BusinessException(ErrorCode.INVALID_INVITE_CODE);
```

잘못된 JSON, DTO 검증 실패, 필수 파라미터 누락 및 타입 불일치는 `400 INVALID_REQUEST`를 반환합니다. 없는 경로는 `404 RESOURCE_NOT_FOUND`, 지원하지 않는 HTTP 메서드는 `405 METHOD_NOT_ALLOWED`, 지원하지 않는 응답·요청 형식은 각각 `406 NOT_ACCEPTABLE`, `415 UNSUPPORTED_MEDIA_TYPE`으로 처리합니다. 예상하지 못한 오류는 `500 INTERNAL_SERVER_ERROR`와 고정 메시지를 반환하고 상세 예외는 서버 로그에만 기록합니다. Spring MVC가 제공하는 HTTP 상태와 `Allow` 등의 응답 헤더는 유지합니다.

DB 없이 예외 응답 계약을 검증하려면 다음 명령을 실행합니다.

```bash
./gradlew test --tests '*GlobalExceptionHandlerTest'
```

## 공통 성공 응답

성공 응답을 반환하는 컨트롤러에서 `ApiResponse.of(data)`를 사용하면 다음과 같이 `data` 필드로 감쌉니다. 자동으로 모든 응답을 감싸는 설정은 아닙니다.

```json
{
  "data": {
    "id": 1
  }
}
```

목록 조회의 `Slice<T>`는 `PageResponse.from(slice)`로 변환하고 `ApiResponse.of(...)`로 감쌀 수 있습니다. 페이지 번호는 0부터 시작하며 `size`는 요청한 페이지 크기입니다. 전체 개수 조회 없이 `hasNext`로 다음 페이지 유무를 제공합니다.

```json
{
  "data": {
    "items": [],
    "page": 0,
    "size": 20,
    "hasNext": false
  }
}
```

오류 응답은 기존 `code`, `message` 형식을 유지하며 `data`로 감싸지 않습니다.
