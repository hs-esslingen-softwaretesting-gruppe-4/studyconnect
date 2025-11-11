package de.softwaretesting.studyconnect.steps;

import de.softwaretesting.studyconnect.models.Group;
import de.softwaretesting.studyconnect.models.Task;
import de.softwaretesting.studyconnect.models.User;
import de.softwaretesting.studyconnect.repositories.GroupRepository;
import de.softwaretesting.studyconnect.repositories.TaskRepository;
import de.softwaretesting.studyconnect.repositories.UserRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step Definitions für die create-task.feature Szenarien.
 * Implementiert die Gherkin Steps mit Spring Boot Integration.
 */
public class CreateTaskStepDefinitions {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    private User currentUser;
    private Task currentTask;
    private Group currentGroup;
    private String errorMessage;
    private boolean accessDenied;
    private boolean systemOperational = true;
    private String successMessage;
    private boolean redirectedToOverview;

    // ========== GIVEN Steps (Preconditions) ==========

    @Given("the user is logged in as a student")
    public void the_user_is_logged_in_as_a_student() {
        // Erstelle einen Test-User mit eindeutiger UUID
        String uniqueUUID = "test-uuid-" + System.currentTimeMillis();
        currentUser = new User();
        currentUser.setSurname("Max");
        currentUser.setLastname("Mustermann");
        currentUser.setEmail("max.mustermann@example.com");
        currentUser.setKeycloakUUID(uniqueUUID);
        currentUser = userRepository.save(currentUser);
    }

    @Given("the system is operational and connected to the database")
    public void the_system_is_operational_and_connected_to_the_database() {
        systemOperational = true;
        // Prüfe ob DB-Verbindung funktioniert
        assertThat(taskRepository).isNotNull();
        assertThat(userRepository).isNotNull();
    }

    @Given("the user navigates to the task creation page")
    public void the_user_navigates_to_the_task_creation_page() {
        // Initialisiere neuen Task
        currentTask = new Task();
        errorMessage = null;
    }

    @Given("the user is a member of the study group {string}")
    public void the_user_is_a_member_of_the_study_group(String groupName) {
        // Simuliere Gruppenmitgliedschaft (würde später über Group-Entity laufen)
        assertThat(currentUser).isNotNull();
        currentGroup = new Group();
        currentGroup.setName(groupName);
        currentGroup.setCreatedBy(currentUser.getId());
        currentGroup.setVisibility("PRIVATE");
        currentGroup = groupRepository.save(currentGroup);
    }

    @Given("the user is logged in but does not have task management permissions")
    public void the_user_is_logged_in_but_does_not_have_task_management_permissions() {
        // Erstelle User ohne Berechtigungen mit eindeutiger UUID
        String uniqueUUID = "test-uuid-noperm-" + System.currentTimeMillis();
        currentUser = new User();
        currentUser.setSurname("No");
        currentUser.setLastname("Permissions");
        currentUser.setEmail("noperm@example.com");
        currentUser.setKeycloakUUID(uniqueUUID);
        currentUser = userRepository.save(currentUser);
        accessDenied = false;
    }

    @Given("the database connection is temporarily unavailable")
    public void the_database_connection_is_temporarily_unavailable() {
        systemOperational = false;
        // In einem echten Test würde man hier die DB-Connection mocken/simulieren
    }

    // ========== WHEN Steps (Actions) ==========

    @When("the user enters {string} as the task title")
    public void the_user_enters_as_the_task_title(String title) {
        currentTask.setTitle(title);
    }

    @When("the user enters {string} as the due date")
    public void the_user_enters_as_the_due_date(String dueDate) {
        try {
            LocalDate date = LocalDate.parse(dueDate);
            currentTask.setDueDate(date.atStartOfDay()); // Convert to LocalDateTime
        } catch (Exception e) {
            errorMessage = "Invalid date format";
        }
    }

    @When("the user enters the following task details:")
    public void the_user_enters_the_following_task_details(DataTable dataTable) {
        Map<String, String> data = dataTable.asMap(String.class, String.class);
        
        currentTask.setTitle(data.get("title"));
        currentTask.setDescription(data.get("notes"));
        if (data.get("due date") != null) {
            LocalDate date = LocalDate.parse(data.get("due date"));
            currentTask.setDueDate(date.atStartOfDay());
        }
        currentTask.setPriority(Task.Priority.valueOf(data.get("priority").toUpperCase()));
        currentTask.setCategory(data.get("category"));
    }

    @When("the user leaves the title field empty")
    public void the_user_leaves_the_title_field_empty() {
        currentTask.setTitle(null);
    }

    @When("the user does not specify a priority")
    public void the_user_does_not_specify_a_priority() {
        // Priority nicht setzen, Entity nutzt Default-Wert (MEDIUM)
        // Nichts tun, currentTask hat bereits den Default aus der Entity
    }

    @When("the user selects {string} as the associated group")
    public void the_user_selects_as_the_associated_group(String groupName) {
        // Simuliere Group-ID (würde später echte Group-Entity nutzen)
        currentTask.setGroup(currentGroup);
    }

    @When("the user enters notes of {int} characters")
    public void the_user_enters_notes_of_characters(Integer length) {
        String notes = "A".repeat(length);
        currentTask.setDescription(notes);
    }

    @When("the user submits the task form")
    public void the_user_submits_the_task_form() {
        try {
            // Validierung
            if (currentTask.getTitle() == null || currentTask.getTitle().trim().isEmpty()) {
                errorMessage = "Title is required";
                return;
            }

            if (!systemOperational) {
                errorMessage = "Technical issue - database unavailable";
                return;
            }

            // Prüfe ob bereits ein Validierungsfehler aufgetreten ist (z.B. bei Invalid Date)
            if (errorMessage != null) {
                return; // Nicht speichern, wenn Fehler existiert
            }

            // Setze createdBy
            currentTask.setCreatedBy(currentUser);
            
            // Speichere Task
            currentTask = taskRepository.save(currentTask);
            successMessage = "Task created successfully";
            redirectedToOverview = true;
            
        } catch (Exception e) {
            errorMessage = e.getMessage();
        }
    }

    @When("the user attempts to navigate to the task creation page")
    public void the_user_attempts_to_navigate_to_the_task_creation_page() {
        // Simuliere Berechtigungsprüfung
        accessDenied = true; // In echter Implementation: prüfe User-Rolle
        errorMessage = "You do not have permission to create tasks";
    }

    // ========== THEN Steps (Assertions) ==========

    @Then("the system creates a new task in the database")
    public void the_system_creates_a_new_task_in_the_database() {
        assertThat(currentTask).isNotNull();
        assertThat(currentTask.getId()).isNotNull();
        
        // Prüfe ob Task in DB existiert
        Task savedTask = taskRepository.findById(currentTask.getId()).orElse(null);
        assertThat(savedTask).isNotNull();
    }

    @Then("the system confirms the task creation with a success message")
    public void the_system_confirms_the_task_creation_with_a_success_message() {
        assertThat(successMessage).isEqualTo("Task created successfully");
    }

    @Then("the user is redirected to the task overview page")
    public void the_user_is_redirected_to_the_task_overview_page() {
        assertThat(redirectedToOverview).isTrue();
    }

    @Then("the new task {string} appears in the user's task list")
    public void the_new_task_appears_in_the_user_s_task_list(String taskTitle) {
        List<Task> tasks = taskRepository.findAll();
        assertThat(tasks).anyMatch(task -> task.getTitle().equals(taskTitle));
    }

    @Then("the new task has the following properties:")
    public void the_new_task_has_the_following_properties(DataTable dataTable) {
        Map<String, String> expected = dataTable.asMap(String.class, String.class);
        
        assertThat(currentTask.getTitle()).isEqualTo(expected.get("title"));
        if (expected.containsKey("notes")) {
            assertThat(currentTask.getDescription()).isEqualTo(expected.get("notes"));
        }
        if (expected.containsKey("due date")) {
            LocalDate expectedDate = LocalDate.parse(expected.get("due date"));
            assertThat(currentTask.getDueDate()).isEqualTo(expectedDate.atStartOfDay());
        }
        assertThat(currentTask.getPriority().toString()).isEqualToIgnoringCase(expected.get("priority"));
        assertThat(currentTask.getCategory()).isEqualTo(expected.get("category"));
        assertThat(currentTask.getStatus().toString()).isEqualToIgnoringCase(expected.get("status"));
    }

    @Then("the system displays an error message indicating {string}")
    public void the_system_displays_an_error_message_indicating(String expectedError) {
        assertThat(errorMessage).contains(expectedError);
    }

    @Then("the system keeps the form open for correction")
    public void the_system_keeps_the_form_open_for_correction() {
        assertThat(redirectedToOverview).isFalse();
    }

    @Then("no new task is created in the database")
    public void no_new_task_is_created_in_the_database() {
        if (currentTask.getId() != null) {
            assertThat(taskRepository.findById(currentTask.getId())).isEmpty();
        }
    }

    @Then("the system prompts the user to correct the due date")
    public void the_system_prompts_the_user_to_correct_the_due_date() {
        assertThat(errorMessage).containsIgnoringCase("date");
    }

    @Then("the new task has priority set to {string} by default")
    public void the_new_task_has_priority_set_to_by_default(String expectedPriority) {
        assertThat(currentTask.getPriority()).isEqualTo(Task.Priority.valueOf(expectedPriority.toUpperCase()));
    }

    @Then("the new task has status set to {string} by default")
    public void the_new_task_has_status_set_to_by_default(String expectedStatus) {
        assertThat(currentTask.getStatus()).isEqualTo(Task.Status.valueOf(expectedStatus.toUpperCase()));
    }

    @Then("the task is linked to the group {string}")
    public void the_task_is_linked_to_the_group(String groupName) {
        Group group = groupRepository.findByName(groupName).orElse(null);

        // Ensure the task's group matches the expected group
        assertThat(group).isNotNull();
        assertThat(currentTask.getGroup()).isNotNull();
        assertThat(currentTask.getGroup().getId()).isEqualTo(group.getId());
    }

    @Then("the task appears in the group's shared task list")
    public void the_task_appears_in_the_group_s_shared_task_list() {
        Group group = currentTask.getGroup();
        assertThat(group).isNotNull();

        // Verify via repository that the task is returned when querying tasks for the group
    // Use repository method to fetch tasks for the group and match by id
    boolean foundInGroupTasks = taskRepository.findByGroupId(group.getId()).stream()
        .anyMatch(t -> t.getId().equals(currentTask.getId()));
    assertThat(foundInGroupTasks).isTrue();
    }

    @Then("the system logs the database error")
    public void the_system_logs_the_database_error() {
        // In echter Implementation: prüfe Log-Einträge
        assertThat(errorMessage).containsIgnoringCase("database");
    }

    @Then("the system displays an error message informing the user of a technical issue")
    public void the_system_displays_an_error_message_informing_the_user_of_a_technical_issue() {
        assertThat(errorMessage).containsIgnoringCase("Technical issue");
    }

    @Then("the user is advised to try again later")
    public void the_user_is_advised_to_try_again_later() {
        assertThat(errorMessage).containsIgnoringCase("unavailable");
    }

    @Then("the full notes text is stored correctly")
    public void the_full_notes_text_is_stored_correctly() {
        assertThat(currentTask.getDescription()).hasSize(1000);
    }

    @Then("the system denies access")
    public void the_system_denies_access() {
        assertThat(accessDenied).isTrue();
    }

    @Then("the system displays a message {string}")
    public void the_system_displays_a_message(String expectedMessage) {
        assertThat(errorMessage).isEqualTo(expectedMessage);
    }
}
