# Database migration

애플리케이션이 적용하는 Flyway migration은 `src/main/resources/db/migration`에서 관리합니다.

`database/migrations`의 날짜 기반 SQL은 Flyway 도입 전에 운영 DB에 수동 적용한 이력입니다. 이 변경들은 현재 운영 스키마를 기준으로 만든 `V1__baseline.sql`에 포함되어 있으므로 Flyway migration으로 다시 실행하지 않습니다.

## 기존 운영 DB 최초 전환

1. 운영 DB에서 schema-only dump를 다시 생성하고 테이블, 컬럼, 인덱스, 제약조건이 `V1__baseline.sql`과 일치하는지 확인합니다.
2. `flyway_schema_history`가 아직 없는지 확인합니다.
3. 배포 전 백업 또는 복구 가능한 snapshot을 확보합니다.
4. 최초 배포 대상 VM의 `/opt/todayeng/.env`에만 `FLYWAY_BASELINE_ON_MIGRATE=true`를 설정합니다.
5. 애플리케이션 시작 후 `flyway_schema_history`에 version `1`, type `BASELINE`, success `1`이 기록됐는지 확인합니다.
6. CD는 배포 시작 시점에 `/opt/todayeng/.env`에 `FLYWAY_BASELINE_ON_MIGRATE`가 설정되어 있는지 먼저 확인합니다. 설정되어 있지 않으면(평소 배포) 재생성 단계를 건너뛰어 일반 배포와 동일하게 1회만 기동합니다.
7. 설정되어 있고 최초 health check가 성공한 경우에만, 컨테이너를 `FLYWAY_BASELINE_ON_MIGRATE=false`로 강제 재생성하고 `.env`의 일회성 변수를 제거한 뒤 두 번째 health check를 수행합니다. 이 단계 이전에 최초 health check 자체가 실패하면 `.env`의 변수는 지우지 않고 그대로 두어, 재시도 시 다시 수동으로 설정할 필요가 없도록 합니다.
8. 배포 완료 후 호스트 `.env`에 변수가 없고 실행 중인 컨테이너에는 `false`가 적용됐는지 확인합니다. 삭제 직전 상태는 같은 디렉터리의 `.env.bak`에 남아 있으므로 필요 시 복구할 수 있습니다.

   ```bash
   grep '^FLYWAY_BASELINE_ON_MIGRATE=' /opt/todayeng/.env
   docker inspect todayeng-app --format '{{range .Config.Env}}{{println .}}{{end}}' \
     | grep '^FLYWAY_BASELINE_ON_MIGRATE=false$'
   ```

빈 DB에서는 baseline 옵션을 켜지 않습니다. Flyway가 `V1__baseline.sql`을 실행한 뒤 Hibernate가 엔티티 매핑을 `validate`합니다.

## Migration 규칙

- 적용된 migration 파일은 수정하거나 삭제하지 않습니다. `V1__baseline.sql`도 예외가 아닙니다. 이 파일을 실제로 실행해 스키마를 만든 환경(로컬, CI)은 `validate-on-migrate`로 checksum이 검증되어 수정 시 즉시 실패하지만, 운영 DB는 baseline 이력으로 등록되어 있어 파일 변경을 자동으로 감지하지 못합니다. 스키마를 더 바꿔야 하면 V1을 고치지 말고 V2를 새로 추가합니다.
- 새 파일은 `V<version>__<description>.sql` 형식을 사용합니다.
- Blue-Green 배포 중 구버전과 신버전이 동시에 동작할 수 있도록 Expand-and-Contract 순서를 지킵니다.
- 컬럼이나 테이블 삭제, rename, NOT NULL 강화는 구버전 제거와 데이터 backfill 이후 별도 버전에서 수행합니다.
- 운영에서 `flyway clean`을 사용하지 않습니다.
