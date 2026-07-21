package ru.andrew.website.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

final class RequestBodyLimitFilter extends OncePerRequestFilter {
    private static final String LEAD_PATH = "/api/leads";

    private final int maxRequestBytes;
    private final ProblemResponseWriter problems;

    RequestBodyLimitFilter(WebProperties properties, ProblemResponseWriter problems) {
        this.maxRequestBytes = properties.maxRequestBytes();
        this.problems = problems;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || !LEAD_PATH.equals(path(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        if (request.getContentLengthLong() > maxRequestBytes) {
            writePayloadTooLarge(response, path(request));
            return;
        }
        if (!isApplicationJson(request.getContentType())) {
            writeUnsupportedMediaType(response, path(request));
            return;
        }

        byte[] body = request.getInputStream().readNBytes(maxRequestBytes + 1);
        if (body.length > maxRequestBytes) {
            writePayloadTooLarge(response, path(request));
            return;
        }
        chain.doFilter(new CachedBodyRequest(request, body), response);
    }

    private boolean isApplicationJson(String rawContentType) {
        if (rawContentType == null) {
            return false;
        }
        try {
            MediaType contentType = MediaType.parseMediaType(rawContentType);
            return "application".equalsIgnoreCase(contentType.getType())
                    && "json".equalsIgnoreCase(contentType.getSubtype());
        } catch (InvalidMediaTypeException exception) {
            return false;
        }
    }

    private void writePayloadTooLarge(HttpServletResponse response, String instance) throws IOException {
        problems.write(response, problems.problem(
                HttpStatus.CONTENT_TOO_LARGE,
                "urn:andrew:problem:payload-too-large",
                "Payload too large",
                "The request body exceeds the allowed size.",
                instance));
    }

    private void writeUnsupportedMediaType(HttpServletResponse response, String instance) throws IOException {
        problems.write(response, problems.problem(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "urn:andrew:problem:unsupported-media-type",
                "Unsupported media type",
                "This endpoint accepts application/json only.",
                instance));
    }

    private static String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body.clone();
        }

        @Override
        public ServletInputStream getInputStream() {
            return new ByteArrayServletInputStream(body);
        }

        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }

    private static final class ByteArrayServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream input;

        private ByteArrayServletInputStream(byte[] body) {
            this.input = new ByteArrayInputStream(body);
        }

        @Override
        public int read() {
            return input.read();
        }

        @Override
        public int read(byte[] bytes, int offset, int length) {
            return input.read(bytes, offset, length);
        }

        @Override
        public boolean isFinished() {
            return input.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            if (readListener == null) {
                throw new IllegalArgumentException("readListener must not be null");
            }
            try {
                if (isFinished()) {
                    readListener.onAllDataRead();
                } else {
                    readListener.onDataAvailable();
                }
            } catch (IOException exception) {
                readListener.onError(exception);
            }
        }
    }
}
