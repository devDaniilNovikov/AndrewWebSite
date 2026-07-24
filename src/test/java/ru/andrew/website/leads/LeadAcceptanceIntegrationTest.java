package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.andrew.website.testing.PostgresTestConfiguration;

@Tag("database")
@SpringBootTest(properties = "app.web.rate-limit.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class LeadAcceptanceIntegrationTest {
    private static final int RACE_ADVISORY_LOCK = 424_242;

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    DataSource dataSource;

    @BeforeEach
    void cleanTables() {
        dropTestTriggers();
        jdbc.sql("delete from telegram_outbox").update();
        jdbc.sql("delete from leads").update();
    }

    @AfterEach
    void removeTestTriggers() {
        dropTestTriggers();
    }

    @Test
    void firstAndEquivalentReplayCreateExactlyOnePair() throws Exception {
        UUID requestId = UUID.fromString("11111111-1111-4111-8111-111111111111");

        assertThat(submit(requestId, "Не охлаждает витрина")).isEqualTo(202);
        assertThat(submit(requestId, "  Не охлаждает витрина  ")).isEqualTo(202);

        assertCounts(1, 1);
    }

    @Test
    void submissionWithoutCommentPersistsNullAndCreatesOneOutboxRow() throws Exception {
        UUID requestId = UUID.fromString("19191919-1919-4919-8919-191919191919");

        assertThat(submit(requestId, null)).isEqualTo(202);

        assertThat(jdbc.sql("""
                                select comment
                                from leads
                                where request_id = :requestId
                                """)
                        .param("requestId", requestId)
                        .query(String.class)
                        .optional())
                .isEmpty();
        assertCounts(1, 1);
    }

    @Test
    void websiteOnlyHoneypotReturnsEmptyAcceptanceWithoutRows() throws Exception {
        var response = mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"website\":\"filled-by-bot\"}"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(202);
        assertThat(response.getContentAsByteArray()).isEmpty();
        assertCounts(0, 0);
    }

    @Test
    void changedReplayConflictsWhileRetainedReplayIsAcceptedWithoutInsert()
            throws Exception {
        UUID requestId = UUID.fromString("22222222-2222-4222-8222-222222222222");

        assertThat(submit(requestId, "Первое описание")).isEqualTo(202);
        assertThat(submit(requestId, "Другое описание")).isEqualTo(409);
        jdbc.sql("""
                        update leads
                        set payload_fingerprint = null,
                            name = null,
                            phone = null,
                            comment = null,
                            anonymized_at = now()
                        where request_id = :requestId
                        """)
                .param("requestId", requestId)
                .update();
        assertThat(submit(requestId, "Изменено после хранения")).isEqualTo(202);

        assertCounts(1, 1);
    }

    @Test
    void outboxFailureRollsBackLeadAndNeverReturnsAcceptance() throws Exception {
        jdbc.sql("""
                        create function test_fail_outbox() returns trigger language plpgsql as $$
                        begin
                            raise exception 'test-only forced outbox failure';
                        end
                        $$
                        """)
                .update();
        jdbc.sql("""
                        create trigger test_fail_outbox
                        before insert on telegram_outbox
                        for each row execute function test_fail_outbox()
                        """)
                .update();

        UUID requestId = UUID.fromString("44444444-4444-4444-8444-444444444444");
        assertThat(submit(requestId, "Откат транзакции")).isEqualTo(503);
        assertCounts(0, 0);
    }

    @Test
    void simultaneousEquivalentRequestsCreateOnePair() throws Exception {
        UUID requestId = UUID.fromString("55555555-5555-4555-8555-555555555555");

        assertThat(concurrentlyBehindBlockedCommit(
                        () -> submit(requestId, "Одинаково"),
                        () -> submit(requestId, "Одинаково")))
                .containsExactlyInAnyOrder(202, 202);
        assertCounts(1, 1);
    }

    @Test
    void simultaneousDifferentRequestsYieldAcceptedAndConflict() throws Exception {
        UUID requestId = UUID.fromString("66666666-6666-4666-8666-666666666666");

        assertThat(concurrentlyBehindBlockedCommit(
                        () -> submit(requestId, "Вариант один"),
                        () -> submit(requestId, "Вариант два")))
                .containsExactlyInAnyOrder(202, 409);
        assertCounts(1, 1);
    }

    private List<Integer> concurrentlyBehindBlockedCommit(
            ThrowingRequest first, ThrowingRequest second) throws Exception {
        installOutboxBlocker();
        CountDownLatch start = new CountDownLatch(1);
        try (Connection blocker = dataSource.getConnection();
                var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            acquireRaceLock(blocker);
            var firstResult = executor.submit(() -> {
                start.await();
                return first.run();
            });
            var secondResult = executor.submit(() -> {
                start.await();
                return second.run();
            });
            start.countDown();
            try {
                await().atMost(Duration.ofSeconds(10))
                        .untilAsserted(() ->
                                assertThat(waitingApplicationLockCount())
                                        .isGreaterThanOrEqualTo(2));
            } finally {
                releaseRaceLock(blocker);
            }
            return List.of(firstResult.get(), secondResult.get());
        }
    }

    private void installOutboxBlocker() {
        jdbc.sql("""
                        create function test_block_outbox() returns trigger language plpgsql as $$
                        begin
                            perform pg_advisory_xact_lock(424242);
                            return new;
                        end
                        $$
                        """)
                .update();
        jdbc.sql("""
                        create trigger test_block_outbox
                        before insert on telegram_outbox
                        for each row execute function test_block_outbox()
                        """)
                .update();
    }

    private static void acquireRaceLock(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("select pg_advisory_lock(" + RACE_ADVISORY_LOCK + ")");
        }
    }

    private static void releaseRaceLock(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "select pg_advisory_unlock(" + RACE_ADVISORY_LOCK + ")")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getBoolean(1)).isTrue();
        }
    }

    private int waitingApplicationLockCount() {
        return jdbc.sql("""
                        select count(*)
                        from pg_locks
                        where not granted
                          and locktype in ('advisory', 'transactionid', 'spectoken')
                        """)
                .query(Integer.class)
                .single();
    }

    private int submit(UUID requestId, String comment) throws Exception {
        return mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(requestId, comment)))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private static String body(UUID requestId, String comment) {
        String commentProperty =
                comment == null ? "" : ",\"comment\":\"" + comment + "\"";
        return """
                {"requestId":"%s","name":"Иван","phone":"+7 999 123-45-67",
                 "sourcePath":"/service/"%s,"intent":"repair",
                 "consent":true,"website":""}
                """.formatted(requestId, commentProperty);
    }

    private void assertCounts(int leads, int outbox) {
        assertThat(jdbc.sql("select count(*) from leads").query(Integer.class).single())
                .isEqualTo(leads);
        assertThat(jdbc.sql("select count(*) from telegram_outbox").query(Integer.class).single())
                .isEqualTo(outbox);
    }

    private void dropTestTriggers() {
        jdbc.sql("drop trigger if exists test_fail_outbox on telegram_outbox").update();
        jdbc.sql("drop function if exists test_fail_outbox()").update();
        jdbc.sql("drop trigger if exists test_block_outbox on telegram_outbox").update();
        jdbc.sql("drop function if exists test_block_outbox()").update();
    }

    @FunctionalInterface
    private interface ThrowingRequest {
        int run() throws Exception;
    }
}
