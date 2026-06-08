package com.cs.accountsvc.config.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.cs.accountsvc.config.tracing.TraceContext;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;
import org.springframework.core.env.Environment;

/**
 * Adds service-standard fields to every structured log line.
 *
 * <p>Spring Boot emits Logstash-style JSON logs for this service. This
 * customizer adds stable fields that operations teams can query across Account
 * Service and Event Gateway logs: {@code serviceName}, {@code traceId},
 * {@code spanId}, and {@code appTraceId}. {@code traceId} and
 * {@code appTraceId} both mirror the gateway-facing {@code X-Trace-Id}
 * correlation header when it is present.</p>
 */
public class AccountServiceStructuredLoggingJsonCustomizer
        implements StructuredLoggingJsonMembersCustomizer<ILoggingEvent> {

    private static final String DEFAULT_SERVICE_NAME = "account-service";

    private final String serviceName;

    /**
     * Creates the logging customizer and resolves the service name from Spring
     * configuration.
     *
     * @param environment Spring environment used to read {@code spring.application.name}
     */
    public AccountServiceStructuredLoggingJsonCustomizer(Environment environment) {
        this.serviceName = environment.getProperty("spring.application.name", DEFAULT_SERVICE_NAME);
    }

    /**
     * Registers additional JSON members that should be written on every log
     * event.
     *
     * @param members mutable structured-log member registry supplied by Spring Boot
     */
    @Override
    public void customize(JsonWriter.Members<ILoggingEvent> members) {
        members.add("serviceName", serviceName);
        members.add("traceId", this::traceId);
        members.add("spanId", (event) -> mdcValue(event, "spanId"));
        members.add("appTraceId", (event) -> mdcValue(event, TraceContext.MDC_APP_TRACE_ID_KEY));
    }

    private String traceId(ILoggingEvent event) {
        String appTraceId = mdcValue(event, TraceContext.MDC_APP_TRACE_ID_KEY);
        return appTraceId.isBlank() ? mdcValue(event, TraceContext.MDC_TRACE_ID_KEY) : appTraceId;
    }

    /**
     * Safely reads a value from the logging MDC.
     *
     * @param event Logback event carrying the MDC map
     * @param key MDC key to read
     * @return MDC value, or an empty string when absent
     */
    private String mdcValue(ILoggingEvent event, String key) {
        if (event == null || event.getMDCPropertyMap() == null) {
            return "";
        }
        String value = event.getMDCPropertyMap().get(key);
        return value == null ? "" : value;
    }
}
