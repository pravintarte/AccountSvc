package com.cs.accountsvc.config.tracing;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Propagates the gateway trace id into logs, responses, and tracing headers.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceContextFilter extends OncePerRequestFilter {

    private static final String B3_TRACE_ID_HEADER = "X-B3-TraceId";
    private static final String B3_SPAN_ID_HEADER = "X-B3-SpanId";
    private static final String TRACEPARENT_HEADER = "traceparent";
    private static final Pattern ZIPKIN_TRACE_ID = Pattern.compile("^[a-f0-9]{16}([a-f0-9]{16})?$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ObjectProvider<Tracer> tracerProvider;

    public TraceContextFilter(ObjectProvider<Tracer> tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = firstPresent(
                request.getHeader(TraceContext.TRACE_ID_HEADER),
                currentSpanTraceId(),
                TraceContext.newTraceId()
        );
        HttpServletRequest propagatedRequest = tracingRequest(request, traceId);

        response.setHeader(TraceContext.TRACE_ID_HEADER, traceId);
        try (TraceContext.TraceScope ignored = TraceContext.startTrace(traceId)) {
            filterChain.doFilter(propagatedRequest, response);
        }
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return TraceContext.newTraceId();
    }

    private HttpServletRequest tracingRequest(HttpServletRequest request, String traceId) {
        String normalizedTraceId = traceId == null ? "" : traceId.trim().toLowerCase(Locale.ROOT);
        if (!ZIPKIN_TRACE_ID.matcher(normalizedTraceId).matches()) {
            return request;
        }
        String parentSpanId = newSpanId();
        return new TraceHeaderRequestWrapper(request, normalizedTraceId, parentSpanId);
    }

    private String currentSpanTraceId() {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer == null) {
            return null;
        }
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null || currentSpan.context() == null) {
            return null;
        }
        return currentSpan.context().traceId();
    }

    private String newSpanId() {
        long value;
        do {
            value = RANDOM.nextLong();
        } while (value == 0L);
        return String.format("%016x", value);
    }

    private static final class TraceHeaderRequestWrapper extends HttpServletRequestWrapper {

        private final String traceId;
        private final String parentSpanId;

        private TraceHeaderRequestWrapper(HttpServletRequest request, String traceId, String parentSpanId) {
            super(request);
            this.traceId = traceId;
            this.parentSpanId = parentSpanId;
        }

        @Override
        public String getHeader(String name) {
            if (B3_TRACE_ID_HEADER.equalsIgnoreCase(name)) {
                return traceId;
            }
            if (B3_SPAN_ID_HEADER.equalsIgnoreCase(name)) {
                return parentSpanId;
            }
            if (TRACEPARENT_HEADER.equalsIgnoreCase(name)) {
                return "00-" + traceId + "-" + parentSpanId + "-01";
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String header = getHeader(name);
            if (header != null && isTracingHeader(name)) {
                return Collections.enumeration(Set.of(header));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>();
            Enumeration<String> existingNames = super.getHeaderNames();
            while (existingNames != null && existingNames.hasMoreElements()) {
                names.add(existingNames.nextElement());
            }
            names.add(B3_TRACE_ID_HEADER);
            names.add(B3_SPAN_ID_HEADER);
            names.add(TRACEPARENT_HEADER);
            return Collections.enumeration(names);
        }

        private boolean isTracingHeader(String name) {
            return B3_TRACE_ID_HEADER.equalsIgnoreCase(name)
                    || B3_SPAN_ID_HEADER.equalsIgnoreCase(name)
                    || TRACEPARENT_HEADER.equalsIgnoreCase(name);
        }
    }
}
