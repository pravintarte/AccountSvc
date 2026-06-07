package com.cs.accountsvc.config.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;
import org.springframework.core.env.Environment;

/**
 * Adds service-standard fields to every structured log line.
 */
public class AccountServiceStructuredLoggingJsonCustomizer
        implements StructuredLoggingJsonMembersCustomizer<ILoggingEvent> {

    private static final String DEFAULT_SERVICE_NAME = "account-service";

    private final String serviceName;

    public AccountServiceStructuredLoggingJsonCustomizer(Environment environment) {
        this.serviceName = environment.getProperty("spring.application.name", DEFAULT_SERVICE_NAME);
    }

    @Override
    public void customize(JsonWriter.Members<ILoggingEvent> members) {
        members.add("serviceName", serviceName);
        members.add("traceId", (event) -> mdcValue(event, "traceId"));
        members.add("spanId", (event) -> mdcValue(event, "spanId"));
    }

    private String mdcValue(ILoggingEvent event, String key) {
        if (event == null || event.getMDCPropertyMap() == null) {
            return "";
        }
        String value = event.getMDCPropertyMap().get(key);
        return value == null ? "" : value;
    }
}
