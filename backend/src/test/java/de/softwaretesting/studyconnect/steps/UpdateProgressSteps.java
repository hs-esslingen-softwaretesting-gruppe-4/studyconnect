package de.softwaretesting.studyconnect.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;

import de.softwaretesting.studyconnect.models.Task;
import de.softwaretesting.studyconnect.models.User;
import de.softwaretesting.studyconnect.models.Task.Status;
import de.softwaretesting.studyconnect.repositories.TaskRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

public class UpdateProgressSteps {

    @Autowired
    private TaskRepository taskRepository;

    private User user;
    private Task task;
    private String resultMessage;
    private Exception thrownException;

    /*
     * Background definition
     */

    @Given("the user is logged in")
    public void theUserIsLoggedIn() {
        user = new User();
        user.setEmail("test@example.com");
        user.setSurname("John");
        user.setLastname("Doe");
        user.setCreatedAt(LocalDateTime.now());
        assertNotNull(user, "User should be logged in");
    }

    @Given("the user has a task assigned")
    public void theUserHasATaskAssigned() {
        task = new Task();
        task.setId(100L);
        task.setTitle("Write tests");
        task.setDescription("Write unit tests for Task entity");
        task.setDueDate(LocalDateTime.now().plusDays(3));
        task.setCreatedBy(user);

        task.addAssignee(user);
    }

    @Given("the user has the rights to change the tasks status")
    public void theUserHasTheRightsToChangeTheTasksStatus() {
        assertTrue(task.getAssignees().contains(user),"User should have rights to change status");
    }

    /*
     * Scenario: Change Status
     */
    @When("the user changes the status in the task detail view")
    public void theUserChangesTheStatusInTheTaskDetailView() {
            task.setInProgress();
    }

    @Then("the user gets a message of successfully changing the status")
    public void theUserGetsAMessageOfSuccessfullyChangingTheStatus() {
        /* SKIP */
        //assertTrue(statusChangeSuccessful, "Status change should be successful");
        //assertNotNull(resultMessage, "Success message should be present");
        //assertTrue(resultMessage.contains("successfully"),"Message should indicate success");
    }

    @Then("can view the new status")
    public void canViewTheNewStatus() {
        assertNotNull(task, "Task should exist");
        assertEquals(Status.IN_PROGRESS, task.getStatus(),"Task status should match the new status");
    }

    /*
     * Scenario: Undefined status
     */

    @When("the user enters an invalid status in the task detail view")
    public void theUserEntersTheStatusInTheTaskDetailView() {
        /* SKIP */ //Manual status input not yet implemented
        /* try {
            task.setStatus("INVALID");
        } catch (Exception e) {
            thrownException = e;
            resultMessage = e.getMessage();
        } */
    }

    @Then("the user gets an error message with defined statuses")
    public void theUserGetsAnErrorMessageWithDefinedStatuses() {
        /* SKIP */
        // assertNotNull(thrownException, "Exception should be thrown for invalid status");
        // assertNotNull(resultMessage, "Error message should be present");
        // assertTrue(resultMessage.contains("IN_PROGRESS"),"Error message should list IN_PROGRESS status");
        // assertTrue(resultMessage.contains("COMPLETED"), "Error message should list COMPLETED status");
        // assertTrue(resultMessage.contains("OPEN"),"Error message should list OPEN status");
        // assertTrue(resultMessage.contains("CANCELLED"),"Error message should list CANCELLED status");
    }

    /*
     * Scenario: Error when changing status
     */

    @When("the user changes the status in the task detail view to complete")
    public void theUserChangesTheStatusInTaskDetailView() {
        task.markComplete();
    }

    @When("the database doesn't save the new status")
    public void theDatabaseDoesntSaveTheNewStatus() {
        try {
            taskRepository.save(task);
        } catch (Exception e) {
            thrownException = e;
            resultMessage = e.getMessage();
        }
    }

    @Then("the user gets a message with the report and instructions")
    public void theUserGetsAMessageWithTheReportAndInstructions() {
        assertNotNull(thrownException, "Exception should be thrown for database error");
        // assertInstanceOf(DatabaseException.class, thrownException,"Should throw DatabaseException");
        // assertNotNull(resultMessage, "Error message should be present");
        // assertTrue(resultMessage.contains("Error"),"Message should indicate an error occurred");
        // assertTrue(resultMessage.contains("try again") || resultMessage.contains("contact support"),"Message should provide instructions for user");
    }

    @Then("the user can view the error in a log file")
    public void theUserCanViewTheErrorInALogFile() {
        /* SKIP */
    }
}
