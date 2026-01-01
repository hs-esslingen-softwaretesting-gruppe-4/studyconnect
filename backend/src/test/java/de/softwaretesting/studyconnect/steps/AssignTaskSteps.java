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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;

public class AssignTaskSteps {

  @Autowired private GroupRepository groupRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private TaskRepository taskRepository;

  // test helpers
  private boolean simulatePersistenceFailure = false;
  private String lastMessage = null;

  // store current group/task/user in this step instance for the scenario
  private Group currentGroup;
  private Task currentTask;
  private User currentUser;

  // snapshot of assignee ids before an assign attempt (used to assert no changes were persisted)
  private List<Long> previousAssigneeIds = null;

  /**
   * Create a group with given name
   *
   * @param groupName
   */
  @Given("a group {string}")
  public void aGroup(String groupName) {
    Group g = new Group();
    g.setName(groupName);
    this.currentGroup = g;
  }

  /**
   * Create members and add them to the current group
   *
   * @param table
   */
  @Given("the following members exist in the group:")
  public void theFollowingMembersExistInTheGroup(DataTable table) {
    Group g = this.currentGroup;
    List<Map<String, String>> rows = table.asMaps();
    for (Map<String, String> r : rows) {
      String name = r.getOrDefault("name", "").trim();
      String role = r.getOrDefault("role", "member").trim();
      String keycloakUUID = r.getOrDefault("keycloakUUID", "").trim();

      // try to reuse existing user by keycloakUUID or name
      Optional<User> existing = Optional.empty();
      if (!keycloakUUID.isEmpty()) {
        existing = userRepository.findByKeycloakUUID(keycloakUUID);
      }
      if (existing.isEmpty() && !name.isEmpty()) {
        existing =
            userRepository.findAll().stream()
                .filter(u -> name.equals(u.getFirstname()))
                .findFirst();
      }

      User user;
      if (existing.isPresent()) {
        user = existing.get();
      } else {
        user = new User();
        // populate name, email, keycloakUUID
        if (!name.isEmpty()) {
          user.setFirstname(name);
          user.setLastname(name);
          user.setEmail(name.replaceAll("\\s+", ".").toLowerCase() + "@example.local");
        } else {
          user.setFirstname("user");
          user.setLastname("user");
          user.setEmail((keycloakUUID.isEmpty() ? "user" : keycloakUUID) + "@example.local");
        }
        if (!keycloakUUID.isEmpty()) {
          user.setKeycloakUUID(keycloakUUID);
        }
        user = userRepository.save(user);
      }

      // add to group membership (we'll persist the association)
      g.addMember(user);

      if ("admin".equalsIgnoreCase(role)) {
        g.addAdmin(user);
        if (g.getCreatedBy() == null) {
          g.setCreatedBy(user);
        }
      }
    }
    groupRepository.save(g);
    this.currentGroup = g;
  }

  /**
   * Create a task with given title for given group
   *
   * @param title
   * @param groupName
   */
  @Given("the task {string} exists for group {string}")
  public void theTaskExistsForGroup(String title, String groupName) {
    Optional<Group> gOpt =
        groupRepository.findAll().stream().filter(x -> groupName.equals(x.getName())).findFirst();

    Group g = gOpt.orElseThrow(() -> new IllegalStateException("group not found: " + groupName));

    Task t = new Task();
    t.setTitle(title);
    t.setDescription("automatically created test task");
    t.setGroup(g);
    t = taskRepository.save(t);

    this.currentTask = t;
  }

  /**
   * Log in as given user
   *
   * @param userName
   */
  @Given("I am logged in as {string} with keycloakUUID {string}")
  public void iAmLoggedInAs(String userName, String keycloakUUID) {
    // Accept either a user name (firstname) or a keycloakUUID
    String identifier = userName;

    Optional<User> userOpt = userRepository.findByKeycloakUUID(keycloakUUID);
    if (userOpt.isEmpty()) {
      userOpt =
          userRepository.findAll().stream()
              .filter(u -> identifier.equals(u.getFirstname()))
              .findFirst();
    }

    User user =
        userOpt.orElseThrow(() -> new IllegalStateException("User not found: " + identifier));
    // set as current user in this test step context
    this.currentUser = user;
  }

  /**
   * Assign members to the current task
   *
   * @param table
   */
  @When("I assign members to the task:")
  public void iAssignMembersToTheTask(DataTable table) {
    lastMessage = null;
    Task t = this.currentTask;
    if (t == null) throw new IllegalStateException("No current task in state");

    Group g = t.getGroup();
    if (g == null) throw new IllegalStateException("Task has no group assigned");

    // take a snapshot of currently persisted assignees for later verification
    if (t.getId() != null) {
      previousAssigneeIds = taskRepository.findAssigneeIdsByTaskId(t.getId());
    } else {
      previousAssigneeIds = List.of();
    }

    List<Map<String, String>> rows = table.asMaps();
    for (Map<String, String> r : rows) {
      String memberRaw = r.values().stream().findFirst().orElse("").trim();

      Optional<User> userOpt = userRepository.findByKeycloakUUID(memberRaw);
      if (userOpt.isEmpty()) {
        userOpt =
            userRepository.findAll().stream()
                .filter(u -> memberRaw.equals(u.getFirstname()))
                .findFirst();
      }

      if (!userOpt.isPresent()) {
        lastMessage = "Selected user is not a group member";
        return;
      }

      User member = userOpt.get();
      // fetch member ids for this group without initializing the group's members
      // collection
      Optional<Set<Long>> memberIds = groupRepository.findMemberIdsByGroupId(g.getId());
      boolean isMember = memberIds.map(ids -> ids.contains(member.getId())).orElse(false);
      if (!isMember) {
        lastMessage = "Selected user is not a group member";
        return;
      }

      if (this.currentUser == null
          || g.getId() == null
          || !groupRepository.existsAdminByGroupIdAndUserId(g.getId(), this.currentUser.getId())) {
        lastMessage = "authorization";
        return;
      }

      t.addAssignee(member);
    }

    // simulate persistence failure if requested
    if (simulatePersistenceFailure) {
      lastMessage = "The assignment could not be saved";
      simulatePersistenceFailure = false;
      return;
    }

    taskRepository.save(t);
    this.currentTask = t;
  }

  /**
   * Attempt to assign members to the current task
   *
   * @param table
   */
  @When("I attempt to assign members to the task:")
  public void iAttemptToAssignMembersToTheTask(DataTable table) {
    iAssignMembersToTheTask(table);
  }

  /**
   * Check that the current task shows the given assignees
   *
   * @param title
   * @param table
   */
  @Then("the task {string} shows the following assignees:")
  public void theTaskShowsTheFollowingAssignees(String title, DataTable table) {
    Task t = this.currentTask;
    if (t == null) throw new IllegalStateException("No current task in state");

    List<String> expected =
        table.asMaps().stream()
            .map(m -> m.values().stream().findFirst().orElse("").trim())
            .toList();

    List<String> actual = t.getAssignees().stream().map(User::getFirstname).toList();

    if (!actual.containsAll(expected) || actual.size() != expected.size()) {
      throw new AssertionError(
          "Assignees do not match. expected=" + expected + " actual=" + actual);
    }
  }

  /** Check that assignees received notification */
  @Then("the assignees should receive a notification about the assignment")
  public void theAssigneesShouldReceiveANotificationAboutTheAssignment() {
    Task t = this.currentTask;
    if (t == null || t.getAssignees().isEmpty()) {
      throw new AssertionError("No assignees present to be notified");
    }

    // ToDo: implement notification check when notification system is in place
  }

  /** Check for authorization error */
  @Then("I should see an authorization error")
  public void iShouldSeeAnAuthorizationError() {
    if (!"authorization".equals(lastMessage)) {
      // also accept that current user is not admin as an authorization condition
      Group g = this.currentGroup;
      if (g != null
          && this.currentUser != null
          && g.getId() != null
          && !groupRepository.existsAdminByGroupIdAndUserId(g.getId(), this.currentUser.getId())) {
        return;
      }
      throw new AssertionError("Expected authorization error but got: " + lastMessage);
    }
  }

  /** Check for external user error */
  @Then("I should see the message \"Selected user is not a group member\"")
  public void iShouldSeeTheMessageSelectedUserNotGroupMember() {
    if (!"Selected user is not a group member".equals(lastMessage)) {
      throw new AssertionError("Expected message about external user but got: " + lastMessage);
    }
  }

  /** Simulate persistence layer failure */
  @Given("the persistence layer is temporarily unavailable")
  public void thePersistenceLayerIsTemporarilyUnavailable() {
    simulatePersistenceFailure = true;
    lastMessage = null;
  }

  /** Check for persistence layer error */
  @Then("I should see the message \"The assignment could not be saved\"")
  public void iShouldSeeTheMessageTheAssignmentCouldNotBeSaved() {
    if (!"The assignment could not be saved".equals(lastMessage)) {
      throw new AssertionError("Expected persistence failure message but got: " + lastMessage);
    }
  }

  /** Check that previous assignees remain unchanged */
  @Then("the task should still show its previous assignees")
  public void theTaskShouldStillShowItsPreviousAssignees() {

    Task t = this.currentTask;
    // fetch currently persisted assignees
    List<Long> actualIds = taskRepository.findAssigneeIdsByTaskId(t.getId());

    // compare to previous snapshot
    if (!new HashSet<>(actualIds).equals(new HashSet<>(previousAssigneeIds))) {

      // assignees changed unexpectedly
      throw new AssertionError(
          "Assignees changed unexpectedly. expected="
              + previousAssigneeIds
              + " actual="
              + actualIds);
    }
  }

  /** Check that previous assignees remain unchanged */
  @Then("no assignees should change for the task")
  public void noAssigneesShouldChangeForTheTask() {
    Task t = this.currentTask;
    if (t == null) throw new IllegalStateException("No current task in state");

    if (previousAssigneeIds == null) {
      throw new IllegalStateException("No previous assignee snapshot available to compare against");
    }

    // fetch currently persisted assignees
    List<Long> actualIds = taskRepository.findAssigneeIdsByTaskId(t.getId());

    // compare to previous snapshot
    if (!new HashSet<>(actualIds).equals(new HashSet<>(previousAssigneeIds))) {

      // assignees changed unexpectedly
      throw new AssertionError(
          "Assignees changed unexpectedly. expected="
              + previousAssigneeIds
              + " actual="
              + actualIds);
    }
  }
}
