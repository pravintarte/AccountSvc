package com.cs.accountsvc.controller;

import com.cs.accountsvc.repository.AccountTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the POC internal-access guard on Account Service account endpoints.
 *
 * <p>These tests prove direct calls to {@code /accounts/**} are denied unless
 * they carry the shared Event Gateway caller identity and token headers. They
 * also verify public health remains open for local smoke checks.</p>
 */
@Slf4j
@AutoConfigureMockMvc
@SpringBootTest(properties = "eureka.client.enabled=false")
@DisplayName("Account Service internal access interceptor behavior")
class AccountInternalAccessInterceptorTest {

    private static final String INTERNAL_CALLER_HEADER = "X-Internal-Caller";
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String EVENT_GATEWAY_CALLER = "event-gateway-api";
    private static final String LOCAL_DEV_TOKEN = "local-dev-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountTransactionRepository repository;

    /**
     * Clears persisted ledger rows before each security scenario.
     */
    @BeforeEach
    void setUp() {
        log.info("Resetting account transaction repository before internal access test");
        repository.deleteAll();
    }

    /**
     * Verifies direct account endpoint calls without internal headers are denied.
     *
     * @throws Exception when MockMvc request execution fails
     */
    @Test
    @DisplayName("Account endpoint request without internal caller headers is denied with HTTP 403")
    void accountEndpoint_whenInternalHeadersAreMissing_returnsForbidden() throws Exception {
        log.info("Testing account endpoint denial when internal access headers are missing");

        mockMvc.perform(get("/accounts/acct-123/balance"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INTERNAL_ACCESS_DENIED"))
                .andExpect(content().string(containsString("Event Gateway")));
    }

    /**
     * Verifies callers with the wrong shared token are denied.
     *
     * @throws Exception when MockMvc request execution fails
     */
    @Test
    @DisplayName("Account endpoint request with an invalid internal token is denied with HTTP 403")
    void accountEndpoint_whenInternalTokenIsWrong_returnsForbidden() throws Exception {
        log.info("Testing account endpoint denial when internal token is invalid");

        mockMvc.perform(get("/accounts/acct-123/balance")
                        .header(INTERNAL_CALLER_HEADER, EVENT_GATEWAY_CALLER)
                        .header(INTERNAL_TOKEN_HEADER, "wrong-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INTERNAL_ACCESS_DENIED"));
    }

    /**
     * Verifies valid Event Gateway internal headers allow account endpoint traffic.
     *
     * @throws Exception when MockMvc request execution fails
     */
    @Test
    @DisplayName("Account endpoint request with Event Gateway internal headers reaches the controller")
    void accountEndpoint_whenEventGatewayInternalHeadersAreValid_reachesController() throws Exception {
        log.info("Testing account endpoint access when valid Event Gateway internal headers are present");

        mockMvc.perform(post("/accounts/acct-123/transactions")
                        .header(INTERNAL_CALLER_HEADER, EVENT_GATEWAY_CALLER)
                        .header(INTERNAL_TOKEN_HEADER, LOCAL_DEV_TOKEN)
                        .header("Idempotency-Key", "9b63f0d4-0f49-4f85-9447-fd45a0c5b3c2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "9b63f0d4-0f49-4f85-9447-fd45a0c5b3c2",
                                  "type": "CREDIT",
                                  "amount": 150.00,
                                  "currency": "USD",
                                  "eventTimestamp": "2026-05-15T14:02:11Z",
                                  "metadata": {
                                    "source": "event-gateway"
                                  }
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    /**
     * Verifies public health remains callable without internal headers.
     *
     * @throws Exception when MockMvc request execution fails
     */
    @Test
    @DisplayName("Public health endpoint remains open without Event Gateway internal headers")
    void healthEndpoint_whenInternalHeadersAreMissing_returnsOk() throws Exception {
        log.info("Testing public health endpoint remains open without internal headers");

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("HEALTH_OK"));
    }
}
