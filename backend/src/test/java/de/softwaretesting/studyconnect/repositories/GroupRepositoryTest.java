package de.softwaretesting.studyconnect.repositories;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

import de.softwaretesting.studyconnect.models.Group;
import de.softwaretesting.studyconnect.models.User;
import de.softwaretesting.studyconnect.models.Task;
import de.softwaretesting.studyconnect.repositories.TaskRepository;
import java.util.List;

@DataJpaTest
@ActiveProfiles("test")
class GroupRepositoryTest {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    /**
     * Test creating and saving a group with valid data.
     * Checks that all fields are persisted correctly, 
     * including auto-generated timestamps and relationships.
     */
    @Test
    void shouldCreateAndSaveGroupWithValidData() {
        // Arrange: create and save an admin user
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setSurname("Admin");
        admin.setLastname("User");
        admin.setCreatedAt(LocalDateTime.now());
        User savedAdmin = userRepository.save(admin);

        // Arrange: create a new group
        Group group = new Group();
        group.setName("Study Group 1");
        group.setDescription("Test group for StudyConnect");
        group.setVisibility("PRIVATE");
        group.setMaxMembers(10);
        group.setCreatedBy(savedAdmin.getId());
        group.setAdmin(savedAdmin);

        // Add the admin as a member
        Set<User> members = new HashSet<>();
        members.add(savedAdmin);
        group.setMembers(members);

        // Act: save the group
        Group saved = groupRepository.save(group);

        // Assert: verify that the group was saved correctly
        assertNotNull(saved.getId());
        assertEquals("Study Group 1", saved.getName());
        assertEquals("PRIVATE", saved.getVisibility());
        assertEquals(savedAdmin.getId(), saved.getCreatedBy());
        assertNotNull(saved.getCreatedAt(), "createdAt should be auto-set");
        assertNotNull(saved.getUpdatedAt(), "updatedAt should be auto-set");
        assertEquals(savedAdmin.getId(), saved.getAdmin().getId());
        assertTrue(saved.getMembers().contains(savedAdmin));
    }

    /**
     * Test that adding a Task to a Group persists the task via cascade
     * and that removing the Task from the Group deletes it (orphanRemoval).
     */
    @Test
    void shouldAddAndRemoveTaskFromGroup() {
        // Arrange: create and save an admin user
        User admin = new User();
        admin.setEmail("taskadmin@example.com");
        admin.setSurname("Task");
        admin.setLastname("Admin");
        admin.setCreatedAt(LocalDateTime.now());
        User savedAdmin = userRepository.save(admin);

        // Arrange: create a new group
        Group group = new Group();
        group.setName("Task Group");
        group.setDescription("Group for task tests");
        group.setVisibility("PRIVATE");
        group.setMaxMembers(10);
        group.setCreatedBy(savedAdmin.getId());
        group.setAdmin(savedAdmin);

        // Arrange: create a new task and add to group (bi-directional helper)
        Task task = new Task();
        task.setTitle("Test Task");
        task.setDescription("A task to test add/remove behavior");
        // do not explicitly set group on task; use addTask helper
        group.addTask(task);

        // Act: save the group which should cascade-persist the task
        Group savedGroup = groupRepository.saveAndFlush(group);

    // After save, the task should have been persisted and linked to the saved group
    List<Task> groupTasks = taskRepository.findByGroupId(savedGroup.getId());
    assertFalse(groupTasks.isEmpty(), "There should be at least one task persisted for the group");

    // Get persisted task and verify linkage
    Task persisted = groupTasks.stream()
        .filter(t -> t.getTitle().equals("Test Task"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Persisted task not found in group tasks"));
    assertNotNull(persisted.getGroup(), "Persisted task should reference its group");
    assertEquals(savedGroup.getId(), persisted.getGroup().getId());

        // Act: remove the task from the group and flush
        savedGroup.removeTask(persisted);
        groupRepository.saveAndFlush(savedGroup);

    // Assert: because Group.tasks is configured with orphanRemoval=true,
    // the task should be deleted from the database
    assertTrue(taskRepository.findById(persisted.getId()).isEmpty(), "Task should be removed from DB after orphanRemoval");
    }

    /**
     * Test that saving a group without a name fails.
     * Database constraints should prevent null names.
     */
    @Test
    void shouldFailToSaveGroupWhenNameIsNull() {
        Group group = new Group();
        group.setDescription("No name group");
        group.setVisibility("PUBLIC");
        group.setCreatedBy(1L);

        assertThrows(Exception.class,
                () -> groupRepository.saveAndFlush(group));
    }

    /**
     * Test that saving a group without visibility fails.
     * Database constraints should prevent null visibility.
     */
    @Test
    void shouldFailToSaveGroupWhenVisibilityIsNull() {
        Group group = new Group();
        group.setName("Invalid Group");
        group.setCreatedBy(1L);

        assertThrows(Exception.class,
                () -> groupRepository.saveAndFlush(group));
    }

    /**
     * Test that createdAt and updatedAt timestamps are automatically set.
     * Uses @PrePersist and @PreUpdate in the entity.
     */
    @Test
    void shouldAutoSetCreatedAndUpdatedAt() {
        Group group = new Group();
        group.setName("AutoTimestamp Group");
        group.setVisibility("PRIVATE");
        group.setCreatedBy(1L);

        Group saved = groupRepository.saveAndFlush(group);

        assertNotNull(saved.getCreatedAt(), "createdAt should be auto-set");
        assertNotNull(saved.getUpdatedAt(), "updatedAt should be auto-set");
    }

    /**
     * Test saving members in a group beyond maxMembers.
     * The entity does not automatically enforce maxMembers;
     * this test ensures that all members are still persisted.
     */
    @Test
    void shouldSaveAllMembersRegardlessOfMaxMembers() {
        // Arrange: create and save an admin
        User admin = new User();
        admin.setEmail("admin2@example.com");
        admin.setSurname("Admin2");
        admin.setLastname("User");
        admin.setCreatedAt(LocalDateTime.now());
        User savedAdmin = userRepository.save(admin);

        // Arrange: create group
        Group group = new Group();
        group.setName("Limited Group");
        group.setVisibility("PRIVATE");
        group.setCreatedBy(savedAdmin.getId());
        group.setAdmin(savedAdmin);

        // Arrange: create more members than the maxMembers value
        Set<User> members = new HashSet<>();
        for (int i = 0; i < 25; i++) { // more than default maxMembers=20
            User user = new User();
            user.setEmail("user" + i + "@example.com");
            user.setSurname("User" + i);
            user.setLastname("Test");
            user.setCreatedAt(LocalDateTime.now());
            members.add(userRepository.save(user));
        }
        members.add(savedAdmin); // include the admin
        group.setMembers(members);

        // Act: save the group
        Group saved = groupRepository.save(group);

        // Assert: all members are persisted regardless of maxMembers
        assertEquals(26, saved.getMembers().size(), "All members including admin should be saved");
        assertTrue(saved.getMembers().contains(savedAdmin));
    }
}
