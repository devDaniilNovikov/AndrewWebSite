package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import ru.andrew.website.testing.PostgresTestConfiguration;

@Tag("database")
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class LeadOutboxMigrationTest {
    private static final String UPGRADE_SCHEMA = "sec03_upgrade";

    @Autowired
    JdbcClient jdbc;

    @Autowired
    Flyway flyway;

    @Autowired
    DataSource dataSource;

    @Test
    void appliesValidatedMigrationsToPostgres18AndRemainsIdempotent() {
        int serverVersion = Integer.parseInt(
                jdbc.sql("show server_version_num").query(String.class).single());
        assertThat(serverVersion / 10_000).isEqualTo(18);

        assertThat(migrationHistory()).containsExactly(
                new MigrationHistory("1", "lead outbox baseline", "SQL", true, true),
                new MigrationHistory("2", "privacy identity hardening", "SQL", true, true));

        flyway.validate();
        flyway.migrate();

        assertThat(migrationHistory()).containsExactly(
                new MigrationHistory("1", "lead outbox baseline", "SQL", true, true),
                new MigrationHistory("2", "privacy identity hardening", "SQL", true, true));
    }

    @Test
    void createsExactLeadColumns() {
        assertThat(columns("leads")).containsExactly(
                identityColumn("id", "bigint", null, false, null),
                column("request_id", "uuid", null, false, false, null),
                column("payload_fingerprint", "bytea", null, true, false, null),
                column("name", "character varying", 100, true, false, null),
                column("phone", "character varying", 15, true, false, null),
                column("comment", "character varying", 1000, true, false, null),
                column("source_path", "character varying", 2048, false, false, null),
                column("intent", "character varying", 16, false, false, null),
                column("consented_at", "timestamp with time zone", null, false, false, null),
                column("created_at", "timestamp with time zone", null, false, false, null),
                column("anonymized_at", "timestamp with time zone", null, true, false, null));
    }

    @Test
    void createsExactOutboxColumns() {
        assertThat(columns("telegram_outbox")).containsExactly(
                identityColumn("id", "bigint", null, false, null),
                column("lead_id", "bigint", null, false, false, null),
                column("state", "character varying", 16, false, false, null),
                column("attempt_count", "integer", null, false, false, "0"),
                column("next_attempt_at", "timestamp with time zone", null, false, false, null),
                column("lease_token", "uuid", null, true, false, null),
                column("lease_until", "timestamp with time zone", null, true, false, null),
                column("last_error_code", "character varying", 64, true, false, null),
                column("created_at", "timestamp with time zone", null, false, false, null),
                column("updated_at", "timestamp with time zone", null, false, false, null),
                column("delivered_at", "timestamp with time zone", null, true, false, null));
    }

    @Test
    void createsNamedConstraintsAndExactPartialIndexes() {
        assertThat(constraintNames("leads")).containsExactlyInAnyOrder(
                "leads_pkey",
                "uk_leads_request_id",
                "ck_leads_intent",
                "ck_leads_fingerprint",
                "ck_leads_phone",
                "ck_leads_privacy",
                "ck_leads_request_id_v4");
        assertThat(constraintNames("telegram_outbox")).containsExactlyInAnyOrder(
                "telegram_outbox_pkey",
                "uk_telegram_outbox_lead_id",
                "fk_telegram_outbox_lead",
                "ck_telegram_outbox_state",
                "ck_telegram_outbox_attempt_count",
                "ck_telegram_outbox_shape");

        String foreignKey = jdbc.sql("""
                        select pg_get_constraintdef(oid)
                        from pg_constraint
                        where conname = 'fk_telegram_outbox_lead'
                        """)
                .query(String.class)
                .single();
        assertThat(foreignKey).contains(
                "FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE");
        assertThat(constraintDefinition("ck_leads_privacy"))
                .contains("source_path")
                .contains("'/'");
        assertThat(constraintDefinition("ck_leads_request_id_v4"))
                .contains("uuid_extract_version(request_id)")
                .contains("distinct from 4")
                .contains("not valid");
        assertThat(constraintValidated("ck_leads_request_id_v4")).isFalse();

        Map<String, String> indexes = indexes();
        assertThat(indexes).containsOnlyKeys(
                "idx_leads_retention",
                "idx_leads_anonymized_cleanup",
                "idx_telegram_outbox_claim",
                "idx_telegram_outbox_expired_lease");
        assertThat(indexes.get("idx_leads_retention"))
                .contains("on public.leads using btree (created_at, id)")
                .contains("where (anonymized_at is null)");
        assertThat(indexes.get("idx_leads_anonymized_cleanup"))
                .contains("on public.leads using btree (anonymized_at, id)")
                .contains("where (anonymized_at is not null)");
        assertThat(indexes.get("idx_telegram_outbox_claim"))
                .contains("on public.telegram_outbox using btree (next_attempt_at, id)")
                .contains("where")
                .contains("'pending'")
                .contains("'retry'");
        assertThat(indexes.get("idx_telegram_outbox_expired_lease"))
                .contains("on public.telegram_outbox using btree (lease_until, id)")
                .contains("where")
                .contains("'processing'");
    }

    @Test
    void upgradesLegacyRowsWithoutBlockingTheirAnonymization() {
        jdbc.sql("drop schema if exists " + UPGRADE_SCHEMA + " cascade").update();
        jdbc.sql("create schema " + UPGRADE_SCHEMA).update();
        try {
            Flyway v1 = isolatedFlyway(MigrationVersion.fromVersion("1"));
            v1.migrate();

            UUID legacyActive =
                    UUID.fromString("11111111-1111-1111-8111-111111111111");
            UUID legacyAnonymized = UUID.randomUUID();
            insertUpgradeLead(legacyActive, false, "/legacy/active/");
            insertUpgradeLead(
                    legacyAnonymized,
                    true,
                    "/legacy/anonymized?phone=70000000000");

            isolatedFlyway(MigrationVersion.LATEST).migrate();

            assertThat(upgradeSourcePath(legacyAnonymized)).isEqualTo("/");
            assertThat(upgradeSourcePath(legacyActive))
                    .isEqualTo("/legacy/active/");

            jdbc.sql("""
                            update sec03_upgrade.leads
                            set payload_fingerprint = null,
                                name = null,
                                phone = null,
                                comment = null,
                                source_path = '/',
                                anonymized_at = now()
                            where request_id = :requestId
                            """)
                    .param("requestId", legacyActive)
                    .update();

            assertThatThrownBy(() -> insertUpgradeLead(
                            UUID.fromString(
                                    "22222222-2222-1222-8222-222222222222"),
                            false,
                            "/new/v1/"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> insertUpgradeLead(
                            UUID.fromString(
                                    "33333333-3333-4333-0333-333333333333"),
                            false,
                            "/new/non-rfc/"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbc.sql("drop schema if exists " + UPGRADE_SCHEMA + " cascade")
                    .update();
        }
    }

    private Flyway isolatedFlyway(MigrationVersion target) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(UPGRADE_SCHEMA)
                .defaultSchema(UPGRADE_SCHEMA)
                .target(target)
                .locations("classpath:db/migration")
                .load();
    }

    private void insertUpgradeLead(
            UUID requestId, boolean anonymized, String sourcePath) {
        OffsetDateTime now = OffsetDateTime.now();
        jdbc.sql("""
                        insert into sec03_upgrade.leads(
                            request_id, payload_fingerprint, name, phone,
                            comment, source_path, intent, consented_at,
                            created_at, anonymized_at
                        )
                        values (
                            :requestId, :fingerprint, :name, :phone,
                            :comment, :sourcePath, 'repair', :now, :now,
                            :anonymizedAt
                        )
                        """)
                .param("requestId", requestId)
                .param(
                        "fingerprint",
                        anonymized ? null : new byte[32],
                        java.sql.Types.BINARY)
                .param(
                        "name",
                        anonymized ? null : "Fictional Legacy User",
                        java.sql.Types.VARCHAR)
                .param(
                        "phone",
                        anonymized ? null : "70000000000",
                        java.sql.Types.VARCHAR)
                .param(
                        "comment",
                        anonymized ? null : "fictional legacy comment",
                        java.sql.Types.VARCHAR)
                .param("sourcePath", sourcePath)
                .param("now", now)
                .param(
                        "anonymizedAt",
                        anonymized ? now : null,
                        java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
                .update();
    }

    private String upgradeSourcePath(UUID requestId) {
        return jdbc.sql("""
                        select source_path
                        from sec03_upgrade.leads
                        where request_id = :requestId
                        """)
                .param("requestId", requestId)
                .query(String.class)
                .single();
    }

    private List<MigrationHistory> migrationHistory() {
        return jdbc.sql("""
                        select version, description, type, success, checksum is not null as checksummed
                        from flyway_schema_history
                        where version is not null
                        order by installed_rank
                        """)
                .query((result, rowNumber) -> new MigrationHistory(
                        result.getString("version"),
                        result.getString("description"),
                        result.getString("type"),
                        result.getBoolean("success"),
                        result.getBoolean("checksummed")))
                .list();
    }

    private List<ColumnContract> columns(String table) {
        return jdbc.sql("""
                        select
                            column_name,
                            data_type,
                            character_maximum_length,
                            is_nullable,
                            is_identity,
                            identity_generation,
                            column_default
                        from information_schema.columns
                        where table_schema = 'public' and table_name = :table
                        order by ordinal_position
                        """)
                .param("table", table)
                .query((result, rowNumber) -> new ColumnContract(
                        result.getString("column_name"),
                        result.getString("data_type"),
                        result.getObject("character_maximum_length", Integer.class),
                        "YES".equals(result.getString("is_nullable")),
                        "YES".equals(result.getString("is_identity")),
                        result.getString("identity_generation"),
                        result.getString("column_default")))
                .list();
    }

    private List<String> constraintNames(String table) {
        return jdbc.sql("""
                        select conname
                        from pg_constraint
                        where conrelid = (:table)::regclass
                          and contype in ('p', 'u', 'f', 'c')
                        """)
                .param("table", "public." + table)
                .query(String.class)
                .list();
    }

    private String constraintDefinition(String constraint) {
        return normalize(jdbc.sql("""
                        select pg_get_constraintdef(oid)
                        from pg_constraint
                        where conname = :constraint
                        """)
                .param("constraint", constraint)
                .query(String.class)
                .single());
    }

    private boolean constraintValidated(String constraint) {
        return jdbc.sql("""
                        select convalidated
                        from pg_constraint
                        where conname = :constraint
                        """)
                .param("constraint", constraint)
                .query(Boolean.class)
                .single();
    }

    private Map<String, String> indexes() {
        return jdbc.sql("""
                        select indexname, indexdef
                        from pg_indexes
                        where schemaname = 'public' and indexname like 'idx_%'
                        order by indexname
                        """)
                .query((result, rowNumber) -> Map.entry(
                        result.getString("indexname"),
                        normalize(result.getString("indexdef"))))
                .list()
                .stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static String normalize(String definition) {
        return definition.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static ColumnContract column(
            String name,
            String dataType,
            Integer maximumLength,
            boolean nullable,
            boolean identity,
            String defaultValue) {
        return new ColumnContract(
                name, dataType, maximumLength, nullable, identity, null, defaultValue);
    }

    private static ColumnContract identityColumn(
            String name,
            String dataType,
            Integer maximumLength,
            boolean nullable,
            String defaultValue) {
        return new ColumnContract(
                name, dataType, maximumLength, nullable, true, "BY DEFAULT", defaultValue);
    }

    private record MigrationHistory(
            String version, String description, String type, boolean success, boolean checksummed) {
    }

    private record ColumnContract(
            String name,
            String dataType,
            Integer maximumLength,
            boolean nullable,
            boolean identity,
            String identityGeneration,
            String defaultValue) {
    }
}
