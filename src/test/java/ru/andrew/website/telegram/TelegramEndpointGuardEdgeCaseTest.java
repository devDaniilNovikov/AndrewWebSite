package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class TelegramEndpointGuardEdgeCaseTest {
    private static final String FICTIONAL_TOKEN =
            "test-only-token-not-a-secret";
    private static final String FICTIONAL_CHAT =
            "test-only-chat-not-a-destination";

    @Test
    void localProfileRejectsPortOutsideUriPortRange() {
        TelegramEndpointGuard guard = guard("http://localhost:65536");

        assertThatThrownBy(guard::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(TelegramEndpointGuard.LOCAL_ENDPOINT_MESSAGE);
    }

    @Test
    void localProfileRejectsSupportedSchemeWithoutHost() {
        TelegramEndpointGuard guard = guard("http:fictional-endpoint");

        assertThatThrownBy(guard::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(TelegramEndpointGuard.LOCAL_ENDPOINT_MESSAGE);
    }

    @Test
    void defensiveLocalPredicateRejectsUriWithoutScheme() {
        assertThat(invokePrivateStatic(
                        "isSafeLocalEndpoint",
                        new Class<?>[] {URI.class},
                        URI.create("//localhost:18081")))
                .isEqualTo(false);
    }

    @Test
    void defensiveEmptyPathPredicateAcceptsNullRepresentation() {
        assertThat(invokePrivateStatic(
                        "isEmpty",
                        new Class<?>[] {String.class},
                        new Object[] {null}))
                .isEqualTo(true);
    }

    private static TelegramEndpointGuard guard(String origin) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        TelegramClientProperties properties = new TelegramClientProperties(
                FICTIONAL_TOKEN, FICTIONAL_CHAT, URI.create(origin));
        return new TelegramEndpointGuard(properties, environment);
    }

    private static Object invokePrivateStatic(
            String methodName, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Method method = TelegramEndpointGuard.class.getDeclaredMethod(
                    methodName, parameterTypes);
            method.setAccessible(true);
            return method.invoke(null, arguments);
        } catch (NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException reflectionFailure) {
            throw new AssertionError(reflectionFailure);
        }
    }
}
