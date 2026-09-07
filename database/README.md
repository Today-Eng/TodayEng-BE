# Database migration

애플리케이션이 적용하는 Flyway migration은 `src/main/resources/db/migration`에서 관리합니다.

`database/migrations`의 날짜 기반 SQL은 Flyway 도입 전에 운영 DB에 수동 적용한 이력입니다. 이 변경들은 현재 운영 스키마를 기준으로 만든 `V1__baseline.sql`에 포함되어 있으므로 Flyway migration으로 다시 실행하지 않습니다.

## 기존 운영 DB 최초 전환

1. 운영 DB의 schema-only dump와 `V1__baseline.sql`이 일치하는지 확인합니다.
2. 배포 전 백업 또는 복구 가능한 snapshot을 확보합니다.
3. 최초 배포에만 `FLYWAY_BASELINE_ON_MIGRATE=true`를 설정합니다.
4. 애플리케이션 시작 후 `flyway_schema_history`에 version `1`, type `BASELINE`이 기록됐는지 확인합니다.
5. 다음 배포부터 `FLYWAY_BASELINE_ON_MIGRATE`를 제거하거나 `false`로 되돌립니다.

빈 DB에서는 baseline 옵션을 켜지 않습니다. Flyway가 `V1__baseline.sql`을 실행한 뒤 Hibernate가 엔티티 매핑을 `validate`합니다.

## Migration 규칙

- 적용된 migration 파일은 수정하거나 삭제하지 않습니다.
- 새 파일은 `V<version>__<description>.sql` 형식을 사용합니다.
- Blue-Green 배포 중 구버전과 신버전이 동시에 동작할 수 있도록 Expand-and-Contract 순서를 지킵니다.
- 컬럼이나 테이블 삭제, rename, NOT NULL 강화는 구버전 제거와 데이터 backfill 이후 별도 버전에서 수행합니다.
- 운영에서 `flyway clean`을 사용하지 않습니다.
