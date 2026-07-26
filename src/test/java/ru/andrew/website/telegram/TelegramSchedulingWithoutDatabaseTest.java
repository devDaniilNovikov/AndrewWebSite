package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.andrew.website.testing.TestAutoConfigurationExclusions.NO_DATABASE;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.andrew.website.leads.LeadAcceptanceTransaction;

@SpringBootTest(properties = {
        NO_DATABASE,
        "app.leads.fingerprint-key=local-scheduling-key-material-for-tests-0001",
        "app.telegram.bot-token=test-only-bot-token-not-a-secret",
        "app.telegram.chat-id=test-only-chat-not-a-destination",
        "app.telegram.base-url=http://127.0.0.1:18081"
})
@ActiveProfiles("local")
class TelegramSchedulingWithoutDatabaseTest {
    @MockitoBean
    LeadAcceptanceTransaction transaction;

    @Autowired
    ScheduledAnnotationBeanPostProcessor scheduling;

    @Test
    void localContextWithoutDatabaseStartsWithoutRegisteringWorkerTask() {
        assertThat(scheduling.getScheduledTasks()).isEmpty();
    }
}
