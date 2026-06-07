Feature: Account Service acceptance behavior

  The Account Service must accept Event Gateway transaction applies,
  acknowledge duplicate idempotent requests without re-applying them,
  expose account balance reads, and provide public health endpoints.

  Scenario: Public health endpoint returns UP
    When I request the public health endpoint
    Then the response status is 200
    And the response code is "HEALTH_OK"

  Scenario: Healthcheck alias returns UP
    When I request the healthcheck endpoint
    Then the response status is 200
    And the response code is "HEALTH_OK"

  Scenario: Apply a valid credit transaction
    When I apply a CREDIT transaction "9b63f0d4-0f49-4f85-9447-fd45a0c5b3c2" for account "acct-123" with amount "150.00"
    Then the response status is 204
    And the account transaction count is 1

  Scenario: Ignore an exact duplicate transaction
    Given I applied a CREDIT transaction "5ecf9f54-2f32-41b0-9b48-e879842f3fb5" for account "acct-123" with amount "150.00"
    When I apply the same transaction again
    Then the response status is 204
    And the account transaction count is 1

  Scenario: Return validation error for an invalid transaction
    When I apply an invalid transaction "31515db2-c81d-47c2-9996-4dbf02a8e75a" for account "acct-123" with amount "-25.00"
    Then the response status is 400
    And the response code is "VALIDATION_ERROR"
    And the account transaction count is 0

  Scenario: Read balance after credit and debit transactions
    Given I applied a CREDIT transaction "0bd17867-d631-44f1-ae8f-a6a726761785" for account "acct-balanced" with amount "150.00"
    And I applied a DEBIT transaction "991f1f51-27cf-4f90-8ea9-2e867a044581" for account "acct-balanced" with amount "25.00"
    When I get the balance for account "acct-balanced"
    Then the response status is 200
    And the response contains balance "125.00"
