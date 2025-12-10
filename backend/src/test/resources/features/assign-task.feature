Feature: Assigning tasks within a study group
  As a group administrator
  I want to assign an existing task to one or more members
  So that responsibilities inside the study group are clear

  Background:
    Given a group "Babagruppe"
      And the following members exist in the group:
        | name          | keycloakUUID                         | role    |
        | Dominik Admin | 00000000-0000-0000-0000-000000000001 | admin   |
        | Bob Member    | 00000000-0000-0000-0000-000000000002 | member  |
        | Carol Member  | 00000000-0000-0000-0000-000000000003 | member  |
  And the task "Prepare test report" exists for group "Babagruppe"
  And I am logged in as "Dominik Admin " with keycloakUUID "00000000-0000-0000-0000-000000000001"

  Scenario: Administrator assigns a task to multiple members
    When I assign members to the task:
      | member | keycloakUUID                         |
      | Bob Member   | 00000000-0000-0000-0000-000000000002 |
      | Carol Member | 00000000-0000-0000-0000-000000000003 |
    Then the task "Prepare test report" shows the following assignees:
      | member       | keycloakUUID                         |
      | Bob Member   | 00000000-0000-0000-0000-000000000002 |
      | Carol Member | 00000000-0000-0000-0000-000000000003 |
    And the assignees should receive a notification about the assignment

  Scenario: Non-admin tries to assign a task
    Given I am logged in as "Bob Member" with keycloakUUID "00000000-0000-0000-0000-000000000002"
    When I attempt to assign members to the task:
      | member |
      | Carol Member |
    Then I should see an authorization error

  Scenario: Administrator selects someone outside the group
    When I assign members to the task:
      | member     |
      | Dana Guest |
    Then I should see the message "Selected user is not a group member"
    And no assignees should change for the task

  Scenario Outline: Task assignment fails due to persistence error
    Given the persistence layer is temporarily unavailable
    When I assign members to the task:
      | member |
      | <member> |
    Then I should see the message "The assignment could not be saved"
    And the task should still show its previous assignees

    Examples:
      | member     |
      | Bob Member |
