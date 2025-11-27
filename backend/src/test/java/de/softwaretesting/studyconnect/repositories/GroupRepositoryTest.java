package de.softwaretesting.studyconnect.repositories;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import de.softwaretesting.studyconnect.models.Group;
import de.softwaretesting.studyconnect.models.Task;
import de.softwaretesting.studyconnect.models.User;

@DataJpaTest
@ActiveProfiles("test")
class GroupRepositoryTest {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    private User savedAdmin;


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
        savedAdmin = userRepository.save(admin);

        // Arrange: create a new group
        Group group = new Group();
        group.setName("Study Group 1");
        group.setDescription("Test group for StudyConnect");
        group.setPublic(true);
        group.setMaxMembers(10);
        group.setCreatedBy(savedAdmin);
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
        assertTrue(saved.isPublic());
        assertEquals(savedAdmin.getId(), saved.getCreatedBy().getId());
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
        savedAdmin = userRepository.save(admin);

        // Arrange: create a new group
        Group group = new Group();
        group.setName("Task Group");
        group.setDescription("Group for task tests");
        group.setMaxMembers(10);
        group.setCreatedBy(savedAdmin);
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
        // Arrange: create and save an admin user
        User admin = new User();
        admin.setEmail("nullname@example.com");
        admin.setSurname("NullName");
        admin.setLastname("User");
        savedAdmin = userRepository.save(admin);

        Group group = new Group();
        group.setDescription("No name group");
        group.setPublic(true);
        group.setCreatedBy(savedAdmin);

        assertThrows(Exception.class,
                () -> groupRepository.saveAndFlush(group));
    }

    /**
     * Test that createdAt and updatedAt timestamps are automatically set.
     * Uses @PrePersist and @PreUpdate in the entity.
     */
    @Test
    void shouldAutoSetCreatedAndUpdatedAt() {
        // Arrange: create and save an admin user
        User admin = new User();
        admin.setEmail("timestamp@example.com");
        admin.setSurname("Timestamp");
        admin.setLastname("User");
        savedAdmin = userRepository.save(admin);

        Group group = new Group();
        group.setName("AutoTimestamp Group");
        group.setCreatedBy(savedAdmin);

        Group saved = groupRepository.saveAndFlush(group);

        assertNotNull(saved.getCreatedAt(), "createdAt should be auto-set");
        assertNotNull(saved.getUpdatedAt(), "updatedAt should be auto-set");
    }

    /**
     * Test that the isPublic field defaults to false when not explicitly set.
     */
    @Test
    void shouldDefaultIsPublicToFalse() {
        // Arrange: create and save an admin user
        User admin = new User();
        admin.setEmail("publicdefault@example.com");
        admin.setSurname("PublicDefault");
        admin.setLastname("User");
        savedAdmin = userRepository.save(admin);

        Group group = new Group();
        group.setName("Default Public Group");
        group.setCreatedBy(savedAdmin);
        group.setAdmin(savedAdmin);

        Group saved = groupRepository.saveAndFlush(group);

        assertFalse(saved.isPublic(), "isPublic should default to false");
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
        savedAdmin = userRepository.save(admin);

        // Arrange: create group
        Group group = new Group();
        group.setName("Limited Group");
        group.setCreatedBy(savedAdmin);
        group.setAdmin(savedAdmin);

        // Arrange: create more members than the maxMembers value
        Set<User> members = new HashSet<>();
        for (int i = 0; i < 25; i++) { // more than default maxMembers=20
            User user = new User();
            user.setEmail("user" + i + "@example.com");
            user.setSurname("User" + i);
            user.setLastname("Test");
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
