package ru.andrew.website;

import static ru.andrew.website.testing.TestAutoConfigurationExclusions.NO_DATABASE;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = NO_DATABASE)
@ActiveProfiles("test")
class AndrewWebsiteApplicationTest {
    @Test
    void contextLoads() {
    }
}
