package ru.andrew.website;

import static ru.andrew.website.testing.TestAutoConfigurationExclusions.NO_DATABASE;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.andrew.website.leads.LeadAcceptanceTransaction;

@SpringBootTest(properties = NO_DATABASE)
@ActiveProfiles("test")
class AndrewWebsiteApplicationTest {
    @MockitoBean
    LeadAcceptanceTransaction transaction;

    @Test
    void contextLoads() {
    }
}
