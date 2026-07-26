package ru.andrew.website.observability;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.ErrorHandler;
import ru.andrew.website.testing.PostgresTestConfiguration;

@SpringBootTest(properties = {
        "LEAD_FINGERPRINT_HMAC_KEY="
                + "fictional-production-fingerprint-key-material-0001",
        "TELEGRAM_BOT_TOKEN=fictional-telegram-token",
        "TELEGRAM_CHAT_ID=fictional-telegram-chat",
        "OTLP_METRICS_URL=https://collector.invalid/v1/metrics",
        "OTLP_AUTHORIZATION=Bearer fictional-otlp-authorization",
        "logging.level.org.testcontainers=OFF",
        "logging.level.tc=OFF"
})
@ActiveProfiles("prod")
@Import({
        PostgresTestConfiguration.class,
        ProductionTelemetryIntegrationTest.CaptureConfiguration.class
})
@ExtendWith(OutputCaptureExtension.class)
@Tag("database")
class ProductionDatabaseLoggingIntegrationTest {
    @Autowired
    JdbcClient jdbc;

    @Autowired
    DataSource dataSource;

    @Autowired
    @Qualifier("scheduledTaskErrorHandler")
    ErrorHandler scheduledTaskErrorHandler;

    @Test
    void migratedDatabaseUrlNeverEntersProductionEcs(
            CapturedOutput output) throws Exception {
        assertThat(jdbc.sql("select 1")
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        String jdbcUrl;
        try (var connection = dataSource.getConnection()) {
            jdbcUrl = connection.getMetaData().getURL();
        }
        scheduledTaskErrorHandler.handleError(
                new IllegalStateException(
                        "fictional-database-exception-detail"));

        assertThat(output.getAll())
                .doesNotContain(
                        jdbcUrl,
                        "fictional-database-exception-detail");
        assertThat(output.getAll().lines()
                        .filter(line -> line.startsWith("{"))
                        .map(ProductionTelemetryIntegrationTest::parseJson)
                        .anyMatch(node ->
                                node.path("ecs").has("version")))
                .isTrue();
    }
}
