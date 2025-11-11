Feature: Updating task progress

    Background:
        Given the user is logged in
        And the user has a task assigned
        And the user has the rights to change the tasks status

    Scenario: Change status
        When the user chagnges the status in the task detail view
        Then the user gets a message of successfully changing the status
        And can view the new status

    Scenario: Undefined status
        When the user enters an invalid status in the task detail view
        Then the user gets an error message with defined statuses

    Scenario: Error when changing status
        When the user changes the status in the task detail view
        And the database doesn't save the new status
        Then the user gets a message with the report and instructions
        And the user can view the error in a log file

 