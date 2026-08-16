package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class TelegramRestClientGatewayTest {
    private static final String BOT_PATH_VALUE =
            "000000000" + ":" + "test-only-bot-token-not-a-secret";
    private static final String ENCODED_BOT_PATH_VALUE =
            BOT_PATH_VALUE.replace(":", "%3A");
    private static final String BOT_ENDPOINT =
            "https://api.telegram.org/bot" + ENCODED_BOT_PATH_VALUE + "/sendMessage";
    private static final String CHAT = "test-only-chat-not-a-destination";
    private static final Instant NOW =
            Instant.parse("2026-01-30T00:00:00Z");
    private static final Instant DELIVERY_DEADLINE =
            NOW.plusSeconds(30);
    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private TelegramRestClientGateway gateway;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = gateway(builder);
    }

    @Test
    void sendsOnlyRequiredPlainTextJsonFieldsToFixedTelegramMethod() {
        server.expect(once(), requestTo(BOT_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.chat_id").value(CHAT))
                .andExpect(jsonPath("$.text").value(
                        new TelegramMessageFormatter().format(message())))
                .andExpect(jsonPath("$.parse_mode").doesNotExist())
                .andRespond(withStatus(HttpStatus.OK));

        assertThat(gateway.send(message(), DELIVERY_DEADLINE))
                .isEqualTo(new TelegramDeliveryResult.Delivered());
        server.verify();
    }

    @ParameterizedTest
    @MethodSource("statusClassifications")
    void classifiesHttpStatuses(
            HttpStatus status, TelegramDeliveryResult expected) {
        server.expect(once(), requestTo(BOT_ENDPOINT))
                .andRespond(withStatus(status));

        assertThat(gateway.send(message(), DELIVERY_DEADLINE))
                .isEqualTo(expected);
        server.verify();
    }

    static Stream<Arguments> statusClassifications() {
        return Stream.of(
                Arguments.of(
                        HttpStatus.NO_CONTENT,
                        new TelegramDeliveryResult.Delivered()),
                Arguments.of(
                        HttpStatus.BAD_REQUEST,
                        new TelegramDeliveryResult.PermanentFailure(
                                "telegram_permanent_400")),
                Arguments.of(
                        HttpStatus.UNAUTHORIZED,
                        new TelegramDeliveryResult.PermanentFailure(
                                "telegram_permanent_401")),
                Arguments.of(
                        HttpStatus.FORBIDDEN,
                        new TelegramDeliveryResult.PermanentFailure(
                                "telegram_permanent_403")),
                Arguments.of(
                        HttpStatus.NOT_FOUND,
                        new TelegramDeliveryResult.PermanentFailure(
                                "telegram_permanent_404")),
                Arguments.of(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        new TelegramDeliveryResult.Retryable("telegram_5xx", null)),
                Arguments.of(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        new TelegramDeliveryResult.Retryable("telegram_5xx", null)),
                Arguments.of(
                        HttpStatus.FOUND,
                        new TelegramDeliveryResult.Retryable(
                                "telegram_unexpected", null)));
    }

    @ParameterizedTest
    @MethodSource("retryAfterBodies")
    void parsesOnlyBoundedPositiveIntegralRetryAfter(
            String body, Duration expected) {
        server.expect(once(), requestTo(BOT_ENDPOINT))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body));

        assertThat(gateway.send(message(), DELIVERY_DEADLINE))
                .isEqualTo(new TelegramDeliveryResult.Retryable(
                        "telegram_429", expected));
        server.verify();
    }

    static Stream<Arguments> retryAfterBodies() {
        return Stream.of(
                Arguments.of(
                        "{\"ok\":false,\"parameters\":{\"retry_after\":120}}",
                        Duration.ofSeconds(120)),
                Arguments.of("{\"ok\":false}", null),
                Arguments.of(
                        "{\"parameters\":{\"retry_after\":-1}}", null),
                Arguments.of(
                        "{\"parameters\":{\"retry_after\":0}}", null),
                Arguments.of(
                        "{\"parameters\":{\"retry_after\":\"120\"}}", null),
                Arguments.of(
                        "{\"parameters\":{\"retry_after\":9223372036854775808}}",
                        null),
                Arguments.of(
                        "{\"parameters\":{\"retry_after\":21601}}",
                        Duration.ofHours(6)),
                Arguments.of("not-json", null),
                Arguments.of(" ".repeat(4_097), null));
    }

    @ParameterizedTest
    @MethodSource("networkFailures")
    void networkFailuresAreRetryableAndNeverLeakExceptionText(
            IOException failure, CapturedOutput output) {
        server.expect(once(), requestTo(BOT_ENDPOINT))
                .andRespond(withException(failure));

        assertThat(gateway.send(message(), DELIVERY_DEADLINE))
                .isEqualTo(new TelegramDeliveryResult.Retryable("network", null));
        assertThat(output.getAll()).doesNotContain(
                failure.getMessage(), BOT_PATH_VALUE, ENCODED_BOT_PATH_VALUE, CHAT,
                "Иван", "79991234567", "Не охлаждает", "/service/",
                "11111111-1111-4111-8111-111111111111");
    }

    @Test
    void debugLoggingRedactsRequestBody(CapturedOutput output) {
        Logger restClientLogger = (Logger) LoggerFactory.getLogger(
                "org.springframework.web.client.DefaultRestClient");
        Level previousLevel = restClientLogger.getLevel();
        server.expect(once(), requestTo(BOT_ENDPOINT))
                .andRespond(withStatus(HttpStatus.OK));

        try {
            restClientLogger.setLevel(Level.DEBUG);
            gateway.send(message(), DELIVERY_DEADLINE);
        } finally {
            restClientLogger.setLevel(previousLevel);
        }

        assertThat(output.getAll())
                .contains("content=<redacted>")
                .doesNotContain(
                        BOT_PATH_VALUE,
                        ENCODED_BOT_PATH_VALUE,
                        CHAT,
                        "Иван",
                        "79991234567",
                        "Не охлаждает",
                        "/service/",
                        "11111111-1111-4111-8111-111111111111");
        server.verify();
    }

    static Stream<IOException> networkFailures() {
        return Stream.of(
                new IOException("fictional-network-failure"),
                new SocketTimeoutException("fictional-timeout"));
    }

    @Test
    void httpClientMetricUsesUriTemplateAndContainsNoSecretOrPiiTags() {
        var meters = new SimpleMeterRegistry();
        var observations = ObservationRegistry.create();
        var observedHttpUrl = new AtomicReference<String>();
        observations.observationConfig()
                .observationHandler(new DefaultMeterObservationHandler(meters))
                .observationHandler(new ObservationHandler<Observation.Context>() {
                    @Override
                    public void onStop(Observation.Context context) {
                        var httpUrl = context.getHighCardinalityKeyValue("http.url");
                        if (httpUrl != null) {
                            observedHttpUrl.set(httpUrl.getValue());
                        }
                    }

                    @Override
                    public boolean supportsContext(Observation.Context context) {
                        return true;
                    }
                });
        RestClient.Builder observedBuilder =
                RestClient.builder().observationRegistry(observations);
        MockRestServiceServer observedServer =
                MockRestServiceServer.bindTo(observedBuilder).build();
        TelegramRestClientGateway observedGateway =
                gateway(observedBuilder, observations);
        observedServer.expect(once(), requestTo(BOT_ENDPOINT))
                .andRespond(withStatus(HttpStatus.OK));

        observedGateway.send(message(), DELIVERY_DEADLINE);

        assertThat(meters.find("http.client.requests").meters()).isEmpty();
        assertThat(meters.find(TelegramClientTelemetry.OBSERVATION_NAME).meters())
                .isNotEmpty();
        assertThat(observedHttpUrl)
                .hasValue(TelegramClientTelemetry.SAFE_ROUTE);
        assertThat(meters.find(TelegramClientTelemetry.OBSERVATION_NAME).meters())
                .allSatisfy(meter -> {
                    assertThat(meter.getId().getTag("uri"))
                            .isEqualTo(TelegramClientTelemetry.SAFE_ROUTE);
                    assertThat(meter.getId().getTag("outcome"))
                            .isEqualTo("delivered");
                    assertThat(meter.getId().getTags())
                            .allSatisfy(tag -> assertThat(tag.getValue())
                                    .doesNotContain(
                                            BOT_PATH_VALUE,
                                            ENCODED_BOT_PATH_VALUE,
                                            CHAT,
                                            "Иван",
                                            "79991234567",
                                            "11111111-1111-4111-8111-111111111111"));
                });
        observedServer.verify();
    }

    @Test
    void stripsPreconfiguredInterceptorsFromCredentialBearingClient() {
        var interceptorInvoked = new AtomicBoolean();
        RestClient.Builder instrumentedBuilder = RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    interceptorInvoked.set(true);
                    return execution.execute(request, body);
                });
        MockRestServiceServer instrumentedServer =
                MockRestServiceServer.bindTo(instrumentedBuilder).build();
        TelegramRestClientGateway instrumentedGateway =
                gateway(instrumentedBuilder);
        instrumentedServer.expect(once(), requestTo(BOT_ENDPOINT))
                .andRespond(withStatus(HttpStatus.OK));

        assertThat(instrumentedGateway.send(message(), DELIVERY_DEADLINE))
                .isEqualTo(new TelegramDeliveryResult.Delivered());

        assertThat(interceptorInvoked).isFalse();
        instrumentedServer.verify();
    }

    @Test
    void networkFailureDoesNotReachObservationHandlersAsTokenBearingError() {
        var meters = new SimpleMeterRegistry();
        var observations = ObservationRegistry.create();
        var errorCallbackInvoked = new AtomicBoolean();
        var observedError = new AtomicReference<Throwable>();
        observations.observationConfig()
                .observationHandler(new DefaultMeterObservationHandler(meters))
                .observationHandler(new ObservationHandler<Observation.Context>() {
                    @Override
                    public void onError(Observation.Context context) {
                        errorCallbackInvoked.set(true);
                        observedError.set(context.getError());
                    }

                    @Override
                    public void onStop(Observation.Context context) {
                        observedError.set(context.getError());
                    }

                    @Override
                    public boolean supportsContext(Observation.Context context) {
                        return true;
                    }
                });
        RestClient.Builder observedBuilder =
                RestClient.builder().observationRegistry(observations);
        MockRestServiceServer observedServer =
                MockRestServiceServer.bindTo(observedBuilder).build();
        TelegramRestClientGateway observedGateway =
                gateway(observedBuilder, observations);
        observedServer.expect(once(), requestTo(BOT_ENDPOINT))
                .andRespond(withException(
                        new IOException("fictional-network-failure")));

        assertThat(observedGateway.send(
                        message(), DELIVERY_DEADLINE))
                .isEqualTo(new TelegramDeliveryResult.Retryable(
                        "network", null));

        assertThat(errorCallbackInvoked).isFalse();
        assertThat(observedError.get()).isNull();
        assertThat(meters.find("http.client.requests").meters()).isEmpty();
        assertThat(meters.find(TelegramClientTelemetry.OBSERVATION_NAME).meters())
                .singleElement()
                .satisfies(meter -> {
                    assertThat(meter.getId().getTag("outcome"))
                            .isEqualTo("retryable");
                    assertThat(meter.getId().getTags())
                            .allSatisfy(tag -> assertThat(tag.getValue())
                                    .doesNotContain(
                                            BOT_PATH_VALUE,
                                            ENCODED_BOT_PATH_VALUE,
                                            CHAT,
                                            "Иван",
                                            "79991234567",
                                            "11111111-1111-4111-8111-111111111111",
                                            "fictional-network-failure"));
                });
        observedServer.verify();
    }

    @Test
    void expiredDeadlinePreventsTheHttpCall() {
        assertThatThrownBy(() -> gateway.send(message(), NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Telegram delivery deadline expired before HTTP call");

        server.verify();
    }

    private TelegramRestClientGateway gateway(RestClient.Builder restClientBuilder) {
        return gateway(restClientBuilder, ObservationRegistry.NOOP);
    }

    private TelegramRestClientGateway gateway(
            RestClient.Builder restClientBuilder,
            ObservationRegistry observationRegistry) {
        return new TelegramRestClientGateway(
                restClientBuilder,
                new TelegramClientProperties(
                        BOT_PATH_VALUE,
                        CHAT,
                        URI.create("https://api.telegram.org")),
                new TelegramMessageFormatter(),
                new TelegramRetryAfterParser(JsonMapper.builder().build()),
                new TelegramClientTelemetry(observationRegistry),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static TelegramLeadMessage message() {
        return new TelegramLeadMessage(
                7L,
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                "Иван",
                "79991234567",
                "Не охлаждает",
                "/service/",
                "repair",
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}
