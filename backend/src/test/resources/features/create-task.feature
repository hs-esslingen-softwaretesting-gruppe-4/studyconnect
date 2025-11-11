# language: en
Feature: Create Task
  As a logged-in student
  I want to create new tasks with relevant details
  So that I can track my learning commitments in StudyConnect

  Background:
    Given the user is logged in as a student
    And the system is operational and connected to the database

  # Happy path - minimal required fields
  Scenario: Successfully create a personal task with only title
    Given the user navigates to the task creation page
    When the user enters "Complete assignment 3" as the task title
    And the user submits the task form
    Then the system creates a new task in the database
    And the system confirms the task creation with a success message
    And the user is redirected to the task overview page
    And the new task "Complete assignment 3" appears in the user's task list

  # Happy path - all optional fields provided
  Scenario: Successfully create a task with all details
    Given the user navigates to the task creation page
    When the user enters the following task details:
      | field       | value                          |
      | title       | Study for midterm exam         |
      | due date    | 2025-12-15                     |
      | priority    | High                           |
      | category    | Exam Prep                      |
      | notes       | Focus on chapters 5-8          |
    And the user submits the task form
    Then the system creates a new task in the database
    And the system confirms the task creation with a success message
    And the new task has the following properties:
      | property    | value                          |
      | title       | Study for midterm exam         |
      | due date    | 2025-12-15                     |
      | priority    | High                           |
      | category    | Exam Prep                      |
      | status      | Open                           |

  # Validation error - missing required field
  Scenario: Attempt to create a task without a title
    Given the user navigates to the task creation page
    When the user leaves the title field empty
    And the user enters "2025-12-20" as the due date
    And the user submits the task form
    Then the system displays an error message indicating "Title is required"
    And the system keeps the form open for correction
    And no new task is created in the database

  # Validation error - invalid date format
  Scenario: Attempt to create a task with invalid due date format
    Given the user navigates to the task creation page
    When the user enters "Write report" as the task title
    And the user enters "next Friday" as the due date
    And the user submits the task form
    Then the system displays an error message indicating "Invalid date format"
    And the system prompts the user to correct the due date
    And no new task is created in the database

  # Optional fields with default values
  Scenario: Create a task without specifying priority
    Given the user navigates to the task creation page
    When the user enters "Read chapter 10" as the task title
    And the user does not specify a priority
    And the user submits the task form
    Then the system creates a new task in the database
    And the new task has priority set to "Medium" by default
    And the new task has status set to "Open" by default

  # Group task creation (optional group assignment)
  Scenario: Create a task and assign it to a study group
    Given the user is a member of the study group "Software Testing Team"
    And the user navigates to the task creation page
    When the user enters "Prepare presentation slides" as the task title
    And the user selects "Software Testing Team" as the associated group
    And the user submits the task form
    Then the system creates a new task in the database
    And the task is linked to the group "Software Testing Team"
    And the task appears in the group's shared task list

  # Database error handling
  Scenario: System fails to save task due to database error
    Given the user navigates to the task creation page
    And the database connection is temporarily unavailable
    When the user enters "Complete lab exercise" as the task title
    And the user submits the task form
    Then the system logs the database error
    And the system displays an error message informing the user of a technical issue
    And the user is advised to try again later
    And no new task is created in the database

  # Edge case - very long notes field
  Scenario: Create a task with maximum length notes
    Given the user navigates to the task creation page
    When the user enters "Research project" as the task title
    And the user enters notes of 1000 characters
    And the user submits the task form
    Then the system creates a new task in the database
    And the full notes text is stored correctly

  # User permissions validation
  Scenario: User must have task management access rights
    Given the user is logged in but does not have task management permissions
    When the user attempts to navigate to the task creation page
    Then the system denies access
    And the system displays a message "You do not have permission to create tasks"
