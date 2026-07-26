package ru.andrew.website;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.andrew.website.testing.TestAutoConfigurationExclusions.NO_DATABASE;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.http.client.autoconfigure.HttpClientsProperties;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.andrew.website.common.ProductionStartupFailureReporter;
import ru.andrew.website.leads.LeadAcceptanceTransaction;

@SpringBootTest(properties = NO_DATABASE)
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
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
    void mainStartsSpringApplicationWithUnchangedArguments() {
        String[] arguments = {"--spring.profiles.active=test"};

        try (MockedConstruction<SpringApplication> construction =
                     mockConstruction(SpringApplication.class)) {
            AndrewWebsiteApplication.main(arguments);

            assertThat(construction.constructed()).hasSize(1);
            SpringApplication application =
                    construction.constructed().getFirst();
            verify(application).addListeners(
                    org.mockito.ArgumentMatchers.any(
                            ProductionStartupFailureReporter.class));
            verify(application).run(arguments);
        }
    }

    @Test
    void productionStartupFailureIsGenericAndUncaughtDetailsAreSuppressed(
            CapturedOutput output) {
        SpringApplication application =
                mock(SpringApplication.class);
        String[] arguments = {"--spring.profiles.active=prod"};
        RuntimeException failure = new IllegalStateException(
                "fictional-private-startup-detail");
        when(application.run(arguments)).thenThrow(failure);
        ProductionStartupFailureReporter failureReporter =
                productionEnvironment("prod");
        Thread thread = Thread.currentThread();
        Thread.UncaughtExceptionHandler previous =
                thread.getUncaughtExceptionHandler();

        try {
            assertThat(catchThrowable(() ->
                    AndrewWebsiteApplication.run(
                            application,
                            failureReporter,
                            arguments)))
                    .isSameAs(failure);
            Thread.UncaughtExceptionHandler safe =
                    thread.getUncaughtExceptionHandler();
            assertThat(safe).isNotSameAs(previous);
            safe.uncaughtException(thread, failure);
            assertThat(output.getAll())
                    .contains("Application startup failed")
                    .doesNotContain(
                            "fictional-private-startup-detail");
        } finally {
            thread.setUncaughtExceptionHandler(previous);
        }
    }

    @Test
    void nonProductionStartupFailurePreservesTheExistingUncaughtHandler() {
        SpringApplication application =
                mock(SpringApplication.class);
        String[] arguments = {"--spring.profiles.active=test"};
        Error failure = new AssertionError("fictional-local-detail");
        when(application.run(arguments)).thenThrow(failure);
        ProductionStartupFailureReporter failureReporter =
                productionEnvironment("test");
        Thread.UncaughtExceptionHandler previous =
                Thread.currentThread().getUncaughtExceptionHandler();

        assertThat(catchThrowable(() ->
                AndrewWebsiteApplication.run(
                        application,
                        failureReporter,
                        arguments)))
                .isSameAs(failure);
        assertThat(Thread.currentThread().getUncaughtExceptionHandler())
                .isSameAs(previous);
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

    private static ProductionStartupFailureReporter
            productionEnvironment(String profile) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        ApplicationEnvironmentPreparedEvent event =
                mock(ApplicationEnvironmentPreparedEvent.class);
        when(event.getEnvironment()).thenReturn(environment);
        ProductionStartupFailureReporter failureReporter =
                new ProductionStartupFailureReporter();
        failureReporter.onApplicationEvent(event);
        return failureReporter;
    }
}
