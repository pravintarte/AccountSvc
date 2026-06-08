package com.cs.accountsvc.config.tracing;

import java.util.UUID;

import org.slf4j.MDC;

/**
 * Minimal application correlation context used for explicit
 * Gateway-to-Account-Service propagation.
 */
public final class TraceContext {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_ID_KEY = "traceId";
    public static final String MDC_APP_TRACE_ID_KEY = "appTraceId";

    private TraceContext() {
    }

    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String traceIdFromMdc() {
        String traceId = MDC.get(MDC_APP_TRACE_ID_KEY);
        return traceId == null || traceId.isBlank() ? MDC.get(MDC_TRACE_ID_KEY) : traceId;
    }

    public static TraceScope startTrace(String traceId) {
        return new TraceScope(traceId);
    }

    public static final class TraceScope implements AutoCloseable {

        private final String previousTraceId;
        private final String previousAppTraceId;

        private TraceScope(String traceId) {
            this.previousTraceId = MDC.get(MDC_TRACE_ID_KEY);
            this.previousAppTraceId = MDC.get(MDC_APP_TRACE_ID_KEY);
            MDC.put(MDC_TRACE_ID_KEY, traceId);
            MDC.put(MDC_APP_TRACE_ID_KEY, traceId);
        }

        @Override
        public void close() {
            restore(MDC_TRACE_ID_KEY, previousTraceId);
            restore(MDC_APP_TRACE_ID_KEY, previousAppTraceId);
        }

        private void restore(String key, String previousValue) {
            if (previousValue == null || previousValue.isBlank()) {
                MDC.remove(key);
            } else {
                MDC.put(key, previousValue);
            }
        }
    }
}
