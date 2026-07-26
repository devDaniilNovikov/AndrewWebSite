package ru.andrew.website.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ru.andrew.website.leads.LeadMetrics;
import tools.jackson.databind.json.JsonMapper;

class RequestBodyLimitFilterTest {
    private static final byte[] JSON = "{\"name\":\"Иван\"}".getBytes(StandardCharsets.UTF_8);

    private final ProblemResponseWriter problems =
            new ProblemResponseWriter(JsonMapper.builder().build());
    private final RequestBodyLimitFilter filter =
            new RequestBodyLimitFilter(
                    properties(),
                    problems,
                    new LeadMetrics(new SimpleMeterRegistry()));

    @Test
    void postOutsideTheLeadEndpointBypassesBodyInspection() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/other");
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) -> invoked.set(true));

        assertThat(invoked).isTrue();
    }

    @Test
    void malformedContentTypeIsRejectedAsUnsupported() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/leads");
        request.setContentType("application/json; charset=\"");
        request.setContent(JSON);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            throw new AssertionError("malformed content type must stop the chain");
        });

        assertThat(response.getStatus()).isEqualTo(415);
        assertThat(response.getContentType())
                .startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    }

    @Test
    void cachedRequestExposesUtf8ReaderLengthsAndIndependentInputStreams() throws Exception {
        HttpServletRequest cached = cachedRequest(JSON, null);

        assertThat(cached.getContentLength()).isEqualTo(JSON.length);
        assertThat(cached.getContentLengthLong()).isEqualTo(JSON.length);
        assertThat(cached.getReader().readLine()).isEqualTo("{\"name\":\"Иван\"}");

        ServletInputStream input = cached.getInputStream();
        assertThat(input.isReady()).isTrue();
        assertThat(input.isFinished()).isFalse();
        assertThat(input.read()).isEqualTo(JSON[0]);
        byte[] remainder = new byte[JSON.length - 1];
        assertThat(input.read(remainder, 0, remainder.length)).isEqualTo(remainder.length);
        assertThat(input.isFinished()).isTrue();
        assertThat(input.read()).isEqualTo(-1);
    }

    @Test
    void cachedRequestHonorsAnExplicitCharacterEncoding() throws Exception {
        byte[] body = "{\"letter\":\"é\"}".getBytes(StandardCharsets.ISO_8859_1);
        HttpServletRequest cached = cachedRequest(body, StandardCharsets.ISO_8859_1.name());

        assertThat(cached.getReader().readLine()).isEqualTo("{\"letter\":\"é\"}");
    }

    @Test
    void servletInputStreamNotifiesAvailabilityAndCompletion() throws Exception {
        HttpServletRequest cached = cachedRequest(JSON, null);
        AtomicBoolean available = new AtomicBoolean();
        AtomicBoolean complete = new AtomicBoolean();
        AtomicReference<Throwable> error = new AtomicReference<>();

        cached.getInputStream().setReadListener(listener(available, complete, error, false));
        ServletInputStream exhausted = cached.getInputStream();
        exhausted.readAllBytes();
        exhausted.setReadListener(listener(available, complete, error, false));

        assertThat(available).isTrue();
        assertThat(complete).isTrue();
        assertThat(error).hasNullValue();
    }

    @Test
    void servletInputStreamRoutesCallbackIoFailuresToOnError() throws Exception {
        HttpServletRequest cached = cachedRequest(JSON, null);
        AtomicReference<Throwable> error = new AtomicReference<>();

        cached.getInputStream().setReadListener(
                listener(new AtomicBoolean(), new AtomicBoolean(), error, true));

        assertThat(error.get())
                .isInstanceOf(IOException.class)
                .hasMessage("listener failure");
    }

    @Test
    void servletInputStreamRejectsANullReadListener() throws Exception {
        HttpServletRequest cached = cachedRequest(JSON, null);

        assertThatThrownBy(() -> cached.getInputStream().setReadListener(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("readListener must not be null");
    }

    private HttpServletRequest cachedRequest(byte[] body, String encoding) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/leads");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (encoding != null) {
            request.setCharacterEncoding(encoding);
        }
        request.setContent(body);
        AtomicReference<HttpServletRequest> cached = new AtomicReference<>();

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (wrappedRequest, ignoredResponse) ->
                        cached.set((HttpServletRequest) wrappedRequest));

        return cached.get();
    }

    private static ReadListener listener(
            AtomicBoolean available,
            AtomicBoolean complete,
            AtomicReference<Throwable> error,
            boolean failAvailability) {
        return new ReadListener() {
            @Override
            public void onDataAvailable() throws IOException {
                available.set(true);
                if (failAvailability) {
                    throw new IOException("listener failure");
                }
            }

            @Override
            public void onAllDataRead() {
                complete.set(true);
            }

            @Override
            public void onError(Throwable failure) {
                error.set(failure);
            }
        };
    }

    private static WebProperties properties() {
        return new WebProperties(
                16_384,
                new WebProperties.RateLimit(
                        false,
                        10_000,
                        Duration.ofHours(1),
                        60,
                        Duration.ofMinutes(1),
                        5,
                        Duration.ofMinutes(1)),
                List.of());
    }
}
