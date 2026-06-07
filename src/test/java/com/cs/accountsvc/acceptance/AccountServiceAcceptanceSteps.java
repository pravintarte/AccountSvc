package com.cs.accountsvc.acceptance;

import com.cs.accountsvc.repository.AccountTransactionRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Cucumber steps for Account Service acceptance behavior.
 */
public class AccountServiceAcceptanceSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountTransactionRepository repository;

    private MvcResult lastResponse;
    private String lastRequestBody;
    private String lastAccountId;
    private String lastEventId;

    @Before
    public void resetState() {
        repository.deleteAll();
        lastResponse = null;
        lastRequestBody = null;
        lastAccountId = null;
        lastEventId = null;
    }

    @When("I request the public health endpoint")
    public void requestPublicHealthEndpoint() throws Exception {
        lastResponse = mockMvc.perform(get("/health"))
                .andReturn();
    }

    @When("I request the healthcheck endpoint")
    public void requestHealthcheckEndpoint() throws Exception {
        lastResponse = mockMvc.perform(get("/healthcheck"))
                .andReturn();
    }

    @When("I apply a {word} transaction {string} for account {string} with amount {string}")
    public void applyTransaction(String type, String eventId, String accountId, String amount) throws Exception {
        lastEventId = eventId;
        lastAccountId = accountId;
        lastRequestBody = transactionJson(eventId, type, amount);
        lastResponse = mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                        .header("Idempotency-Key", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lastRequestBody))
                .andReturn();
    }

    @Given("I applied a {word} transaction {string} for account {string} with amount {string}")
    public void appliedTransaction(String type, String eventId, String accountId, String amount) throws Exception {
        applyTransaction(type, eventId, accountId, amount);
        assertThat(lastResponse.getResponse().getStatus()).isEqualTo(201);
    }

    @When("I apply the same transaction again")
    public void applySameTransactionAgain() throws Exception {
        lastResponse = mockMvc.perform(post("/accounts/{accountId}/transactions", lastAccountId)
                        .header("Idempotency-Key", lastEventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lastRequestBody))
                .andReturn();
    }

    @When("I apply an invalid transaction {string} for account {string} with amount {string}")
    public void applyInvalidTransaction(String eventId, String accountId, String amount) throws Exception {
        lastResponse = mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                        .header("Idempotency-Key", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionJson(eventId, "CREDIT", amount)))
                .andReturn();
    }

    @When("I get the balance for account {string}")
    public void getBalance(String accountId) throws Exception {
        lastResponse = mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andReturn();
    }

    @Then("the response status is {int}")
    public void responseStatusIs(int status) {
        assertThat(lastResponse.getResponse().getStatus()).isEqualTo(status);
    }

    @Then("the response code is {string}")
    public void responseCodeIs(String code) throws Exception {
        assertThat(lastResponse.getResponse().getContentAsString()).contains("\"code\":\"" + code + "\"");
    }

    @Then("the response data field {string} is {string}")
    public void responseDataFieldIs(String field, String value) throws Exception {
        assertThat(lastResponse.getResponse().getContentAsString()).contains("\"" + field + "\":" + value);
    }

    @Then("the account transaction count is {long}")
    public void accountTransactionCountIs(long count) {
        assertThat(repository.count()).isEqualTo(count);
    }

    @Then("the response contains balance {string}")
    public void responseContainsBalance(String balance) throws Exception {
        assertThat(lastResponse.getResponse().getContentAsString()).contains("\"balance\":" + balance);
    }

    private String transactionJson(String eventId, String type, String amount) {
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
