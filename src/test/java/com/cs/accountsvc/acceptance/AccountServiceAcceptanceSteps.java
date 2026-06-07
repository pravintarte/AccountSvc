package com.cs.accountsvc.acceptance;

import com.cs.accountsvc.repository.AccountTransactionRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Cucumber steps for Account Service acceptance behavior.
 *
 * <p>These steps exercise the service through Spring MVC {@link MockMvc} so the
 * acceptance suite verifies routing, validation, controller response envelopes,
 * service logic, JPA persistence, and JSON serialization together without
 * opening a network port.</p>
 */
@Slf4j
public class AccountServiceAcceptanceSteps {

    private static final String INTERNAL_CALLER_HEADER = "X-Internal-Caller";
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String EVENT_GATEWAY_CALLER = "event-gateway-api";
    private static final String LOCAL_DEV_TOKEN = "local-dev-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountTransactionRepository repository;

    private MvcResult lastResponse;
    private String lastRequestBody;
    private String lastAccountId;
    private String lastEventId;

    /**
     * Resets shared Cucumber scenario state before each scenario.
     */
    @Before
    public void resetState() {
        log.info("Resetting Cucumber scenario state and clearing account transaction repository");
        repository.deleteAll();
        lastResponse = null;
        lastRequestBody = null;
        lastAccountId = null;
        lastEventId = null;
    }

    /**
     * Sends a request to the public {@code /health} endpoint.
     *
     * @throws Exception when MockMvc request execution fails
     */
    @When("I request the public health endpoint")
    public void requestPublicHealthEndpoint() throws Exception {
        log.info("Executing Cucumber step: request public health endpoint");
        lastResponse = mockMvc.perform(get("/health"))
                .andReturn();
        log.info("Completed public health endpoint request status={}", lastResponse.getResponse().getStatus());
    }

    /**
     * Sends a request to the public {@code /healthcheck} alias endpoint.
     *
     * @throws Exception when MockMvc request execution fails
     */
    @When("I request the healthcheck endpoint")
    public void requestHealthcheckEndpoint() throws Exception {
        log.info("Executing Cucumber step: request healthcheck endpoint");
        lastResponse = mockMvc.perform(get("/healthcheck"))
                .andReturn();
        log.info("Completed healthcheck endpoint request status={}", lastResponse.getResponse().getStatus());
    }

    /**
     * Applies a transaction through the public Account Service API.
     *
     * @param type transaction direction text from the feature file
     * @param eventId upstream event id used as request idempotency key
     * @param accountId account id path variable
     * @param amount transaction amount text
     * @throws Exception when MockMvc request execution fails
     */
    @When("I apply a {word} transaction {string} for account {string} with amount {string}")
    public void applyTransaction(String type, String eventId, String accountId, String amount) throws Exception {
        log.info("Executing Cucumber step: apply transaction type={} eventId={} accountId={} amount={}",
                type, eventId, accountId, amount);
        lastEventId = eventId;
        lastAccountId = accountId;
        lastRequestBody = transactionJson(eventId, type, amount);
        lastResponse = mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                        .header("Idempotency-Key", eventId)
                        .header(INTERNAL_CALLER_HEADER, EVENT_GATEWAY_CALLER)
                        .header(INTERNAL_TOKEN_HEADER, LOCAL_DEV_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lastRequestBody))
                .andReturn();
        log.info("Completed transaction apply request eventId={} status={}",
                eventId, lastResponse.getResponse().getStatus());
    }

    /**
     * Applies a transaction as scenario setup and asserts that setup succeeded.
     *
     * @param type transaction direction text from the feature file
     * @param eventId upstream event id used as request idempotency key
     * @param accountId account id path variable
     * @param amount transaction amount text
     * @throws Exception when MockMvc request execution fails
     */
    @Given("I applied a {word} transaction {string} for account {string} with amount {string}")
    public void appliedTransaction(String type, String eventId, String accountId, String amount) throws Exception {
        log.info("Executing Cucumber setup step: apply prerequisite transaction eventId={}", eventId);
        applyTransaction(type, eventId, accountId, amount);
        assertThat(lastResponse.getResponse().getStatus()).isEqualTo(201);
        log.info("Verified prerequisite transaction was applied eventId={}", eventId);
    }

    /**
     * Replays the last transaction request to verify idempotency behavior.
     *
     * @throws Exception when MockMvc request execution fails
     */
    @When("I apply the same transaction again")
    public void applySameTransactionAgain() throws Exception {
        log.info("Executing Cucumber step: reapply same transaction eventId={} accountId={}",
                lastEventId, lastAccountId);
        lastResponse = mockMvc.perform(post("/accounts/{accountId}/transactions", lastAccountId)
                        .header("Idempotency-Key", lastEventId)
                        .header(INTERNAL_CALLER_HEADER, EVENT_GATEWAY_CALLER)
                        .header(INTERNAL_TOKEN_HEADER, LOCAL_DEV_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lastRequestBody))
                .andReturn();
        log.info("Completed duplicate transaction apply request eventId={} status={}",
                lastEventId, lastResponse.getResponse().getStatus());
    }

    /**
     * Sends an intentionally invalid transaction request for validation testing.
     *
     * @param eventId upstream event id used as request idempotency key
     * @param accountId account id path variable
     * @param amount invalid amount text from the feature file
     * @throws Exception when MockMvc request execution fails
     */
    @When("I apply an invalid transaction {string} for account {string} with amount {string}")
    public void applyInvalidTransaction(String eventId, String accountId, String amount) throws Exception {
        log.info("Executing Cucumber step: apply invalid transaction eventId={} accountId={} amount={}",
                eventId, accountId, amount);
        lastResponse = mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                        .header("Idempotency-Key", eventId)
                        .header(INTERNAL_CALLER_HEADER, EVENT_GATEWAY_CALLER)
                        .header(INTERNAL_TOKEN_HEADER, LOCAL_DEV_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionJson(eventId, "CREDIT", amount)))
                .andReturn();
        log.info("Completed invalid transaction request eventId={} status={}",
                eventId, lastResponse.getResponse().getStatus());
    }

    /**
     * Requests the account balance endpoint.
     *
     * @param accountId account id path variable
     * @throws Exception when MockMvc request execution fails
     */
    @When("I get the balance for account {string}")
    public void getBalance(String accountId) throws Exception {
        log.info("Executing Cucumber step: get account balance accountId={}", accountId);
        lastResponse = mockMvc.perform(get("/accounts/{accountId}/balance", accountId)
                        .header(INTERNAL_CALLER_HEADER, EVENT_GATEWAY_CALLER)
                        .header(INTERNAL_TOKEN_HEADER, LOCAL_DEV_TOKEN))
                .andReturn();
        log.info("Completed account balance request accountId={} status={}",
                accountId, lastResponse.getResponse().getStatus());
    }

    /**
     * Asserts the HTTP status of the last response.
     *
     * @param status expected HTTP status code
     */
    @Then("the response status is {int}")
    public void responseStatusIs(int status) {
        log.info("Asserting response status expected={} actual={}", status, lastResponse.getResponse().getStatus());
        assertThat(lastResponse.getResponse().getStatus()).isEqualTo(status);
    }

    /**
     * Asserts the stable Account Service response code in the last response body.
     *
     * @param code expected application response code
     * @throws Exception when response content cannot be read
     */
    @Then("the response code is {string}")
    public void responseCodeIs(String code) throws Exception {
        log.info("Asserting response code expected={}", code);
        assertThat(lastResponse.getResponse().getContentAsString()).contains("\"code\":\"" + code + "\"");
    }

    /**
     * Asserts a primitive field value under the last response {@code data}
     * payload by matching the serialized JSON fragment.
     *
     * @param field JSON field name
     * @param value expected raw JSON value text
     * @throws Exception when response content cannot be read
     */
    @Then("the response data field {string} is {string}")
    public void responseDataFieldIs(String field, String value) throws Exception {
        log.info("Asserting response data field field={} expectedValue={}", field, value);
        assertThat(lastResponse.getResponse().getContentAsString()).contains("\"" + field + "\":" + value);
    }

    /**
     * Asserts the number of persisted account transaction rows.
     *
     * @param count expected repository row count
     */
    @Then("the account transaction count is {long}")
    public void accountTransactionCountIs(long count) {
        log.info("Asserting account transaction repository count expected={} actual={}", count, repository.count());
        assertThat(repository.count()).isEqualTo(count);
    }

    /**
     * Asserts the serialized balance field in the last response body.
     *
     * @param balance expected balance text
     * @throws Exception when response content cannot be read
     */
    @Then("the response contains balance {string}")
    public void responseContainsBalance(String balance) throws Exception {
        log.info("Asserting response contains balance expected={}", balance);
        assertThat(lastResponse.getResponse().getContentAsString()).contains("\"balance\":" + balance);
    }

    /**
     * Builds the JSON request body used by transaction apply steps.
     *
     * @param eventId upstream event id
     * @param type transaction direction
     * @param amount transaction amount text
     * @return JSON request body
     */
    private String transactionJson(String eventId, String type, String amount) {
        log.debug("Building Cucumber transaction JSON eventId={} type={} amount={}", eventId, type, amount);
        return """
                {
                  "eventId": "%s",
                  "type": "%s",
                  "amount": %s,
                  "currency": "USD",
                  "eventTimestamp": "2026-05-15T14:02:11Z",
                  "metadata": {
                    "source": "event-gateway"
                  }
                }
                """.formatted(eventId, type, amount);
    }
}
