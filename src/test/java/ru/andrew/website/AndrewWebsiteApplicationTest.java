package ru.andrew.website;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.andrew.website.testing.TestAutoConfigurationExclusions.NO_DATABASE;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.http.client.autoconfigure.HttpClientsProperties;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import ru.andrew.website.leads.LeadAcceptanceTransaction;

@SpringBootTest(properties = NO_DATABASE)
@ActiveProfiles("test")
class AndrewWebsiteApplicationTest {
    @MockitoBean
    LeadAcceptanceTransaction transaction;

    @Autowired
    RestClient.Builder restClientBuilder;

    @Autowired
    HttpClientsProperties httpClientsProperties;

    @Test
    void contextLoads() {
    }

    @Test
    void bootManagesRestClientWithFixedNetworkPolicy() {
        assertThat(restClientBuilder).isNotNull();
        assertThat(httpClientsProperties.getConnectTimeout())
                .isEqualTo(Duration.ofSeconds(3));
        assertThat(httpClientsProperties.getReadTimeout())
                .isEqualTo(Duration.ofSeconds(10));
        assertThat(httpClientsProperties.getRedirects())
                .isEqualTo(HttpRedirects.DONT_FOLLOW);
    }
}
