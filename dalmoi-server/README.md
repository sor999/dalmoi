# 달모이 서버

Java 25와 Docker 엔진이 필요합니다. 아래 명령은 `dalmoi-server/`에서 실행합니다.

## 로컬 실행

```bash
cp .env.example .env
set -a
source .env
set +a
docker compose up -d --wait postgres
./gradlew bootRun
```

Gradle과 실행 JAR는 `.env`를 자동으로 읽지 않으므로 셸이나 IDE 실행 설정에서 환경변수를 전달합니다. 기본값은 로컬 PostgreSQL 16, DB `dalmoi_db`, 사용자 `postgres`입니다. 예제 비밀번호는 로컬 개발용이며 배포 환경에서는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 별도로 주입합니다.

로컬 DB 중지는 `docker compose down`으로 수행합니다. 데이터는 named volume에 보존됩니다.

## JPA와 스키마

- OSIV는 비활성화되어 있습니다. 연관 데이터 조회와 DTO 변환은 필요한 트랜잭션 안에서 수행합니다.
- `ddl-auto: validate`로 엔티티와 DB 스키마를 검증하며, Hibernate가 테이블을 자동 생성하거나 변경하지 않습니다.
- 스키마 변경은 `src/main/resources/db/migration/V1__설명.sql`부터 Flyway SQL로 추가합니다. 현재 도메인 엔티티가 없어 초기 테이블 및 마이그레이션은 없습니다. 첫 엔티티 추가 시 해당 테이블 생성 마이그레이션도 함께 추가합니다.
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

Docker가 없으면 통합 테스트를 건너뛰지 않고 실패합니다. 최초 실행에는 `postgres:16` 이미지 다운로드가 필요합니다.
