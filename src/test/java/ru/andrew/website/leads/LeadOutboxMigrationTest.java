package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import ru.andrew.website.testing.PostgresTestConfiguration;

@Tag("database")
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class LeadOutboxMigrationTest {
    @Autowired
    JdbcClient jdbc;

    @Autowired
    Flyway flyway;

    @Test
    void appliesOneValidatedMigrationToPostgres18AndRemainsIdempotent() {
        int serverVersion = Integer.parseInt(
                jdbc.sql("show server_version_num").query(String.class).single());
        assertThat(serverVersion / 10_000).isEqualTo(18);

        assertThat(migrationHistory()).containsExactly(
                new MigrationHistory("1", "lead outbox baseline", "SQL", true, true));

        flyway.validate();
        flyway.migrate();

        assertThat(migrationHistory()).containsExactly(
                new MigrationHistory("1", "lead outbox baseline", "SQL", true, true));
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
                "ck_leads_privacy");
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
