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
import org.springframework.dao.DataIntegrityViolationException;
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

    // ========== BOUNDARY VALUE ANALYSIS TESTS ==========

    /**
     * Tests group name at maximum length (100 characters).
     */
    @Test
    void shouldHandleNameAtMaxLength() {
        // Arrange: create and save an admin user
        User admin = new User();
        admin.setEmail("admin-maxname@example.com");
        admin.setSurname("Admin");
        admin.setLastname("MaxName");
        savedAdmin = userRepository.save(admin);

        // Arrange: create group with 100-character name
        String maxName = "A".repeat(100);
        Group group = new Group();
        group.setName(maxName);
        group.setCreatedBy(savedAdmin);

        // Act
        Group saved = groupRepository.saveAndFlush(group);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(100, saved.getName().length());
    }

    /**
     * Tests group name at minimum length (1 character).
     */
    @Test
    void shouldHandleNameAtMinLength() {
        // Arrange: create and save an admin user
        User admin = new User();
        admin.setEmail("admin-minname@example.com");
        admin.setSurname("Admin");
        admin.setLastname("MinName");
        savedAdmin = userRepository.save(admin);

        // Arrange: create group with 1-character name
        Group group = new Group();
        group.setName("A");
        group.setCreatedBy(savedAdmin);

        // Act
        Group saved = groupRepository.saveAndFlush(group);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(1, saved.getName().length());
    }

    /**
     * Tests group description at maximum length (500 characters).
     */
    @Test
    void shouldHandleDescriptionAtMaxLength() {
        // Arrange: create and save an admin user
        User admin = new User();
        admin.setEmail("admin-maxdesc@example.com");
        admin.setSurname("Admin");
        admin.setLastname("MaxDesc");
        savedAdmin = userRepository.save(admin);

        // Arrange: create group with 500-character description
        String maxDesc = "B".repeat(500);
        Group group = new Group();
        group.setName("Description Test");
        group.setDescription(maxDesc);
        group.setCreatedBy(savedAdmin);

        // Act
        Group saved = groupRepository.saveAndFlush(group);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(500, saved.getDescription().length());
    }

    /**
     * Tests maxMembers at boundary value 0.
     */
    @Test
    void shouldHandleMaxMembersZero() {
        // Arrange: create and save an admin user
        User admin = new User();
        admin.setEmail("admin-zeromembers@example.com");
        admin.setSurname("Admin");
        admin.setLastname("ZeroMembers");
        savedAdmin = userRepository.save(admin);

        // Arrange: create group with maxMembers = 0
        Group group = new Group();
        group.setName("Zero Members Group");
        group.setMaxMembers(0);
        group.setCreatedBy(savedAdmin);

        // Act
        Group saved = groupRepository.saveAndFlush(group);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(0, saved.getMaxMembers());

        // Arrange: try to add a member
        User member = new User();
        member.setEmail("nomember@example.com");
        member.setSurname("No");
        member.setLastname("Member");
        member = userRepository.save(member);

        // Act: try to add member
        boolean added = saved.addMember(member);

        // Assert: should not be added
        assertFalse(added, "Member should not be added when maxMembers is 0");
        assertEquals(0, saved.getMembers().size());
    }

    /**
     * Tests maxMembers with very large value.
     */
    @Test
    void shouldHandleMaxMembersVeryLarge() {
        // Arrange: create and save an admin user
        User admin = new User();
        admin.setEmail("admin-largemax@example.com");
        admin.setSurname("Admin");
        admin.setLastname("LargeMax");
        savedAdmin = userRepository.save(admin);

        // Arrange: create group with maxMembers = 1000
        Group group = new Group();
        group.setName("Large Members Group");
        group.setMaxMembers(1000);
        group.setCreatedBy(savedAdmin);

        // Act
        Group saved = groupRepository.saveAndFlush(group);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(1000, saved.getMaxMembers());
    }

    // ========== EQUIVALENCE CLASS PARTITIONING TESTS ==========

    /**
     * Tests that NULL description is allowed (optional field).
     */
    @Test
    void shouldAllowNullDescription() {
        // Arrange: create and save an admin user
        User admin = new User();
        admin.setEmail("admin-nulldesc@example.com");
        admin.setSurname("Admin");
        admin.setLastname("NullDesc");
        savedAdmin = userRepository.save(admin);

        // Arrange: create group with NULL description
        Group group = new Group();
        group.setName("No Description Group");
        group.setDescription(null);
        group.setCreatedBy(savedAdmin);

        // Act
        Group saved = groupRepository.saveAndFlush(group);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(null, saved.getDescription());
    }

    /**
     * Tests that empty description is allowed.
     */
    @Test
    void shouldAllowEmptyDescription() {
        // Arrange: create and save an admin user
        User admin = new User();
        admin.setEmail("admin-emptydesc@example.com");
        admin.setSurname("Admin");
        admin.setLastname("EmptyDesc");
        savedAdmin = userRepository.save(admin);

        // Arrange: create group with empty description
        Group group = new Group();
        group.setName("Empty Description Group");
        group.setDescription("");
        group.setCreatedBy(savedAdmin);

        // Act
        Group saved = groupRepository.saveAndFlush(group);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("", saved.getDescription());
    }

    /**
     * Tests that NULL maxMembers uses default value (20).
     */
    @Test
    void shouldUseDefaultMaxMembersWhenNull() {
        // Arrange: create and save an admin user
        User admin = new User();
        admin.setEmail("admin-defaultmax@example.com");
        admin.setSurname("Admin");
        admin.setLastname("DefaultMax");
        savedAdmin = userRepository.save(admin);

        // Arrange: create group without setting maxMembers
        Group group = new Group();
        group.setName("Default Max Members Group");
        group.setCreatedBy(savedAdmin);
        // Not setting maxMembers

        // Act
        Group saved = groupRepository.saveAndFlush(group);

        // Assert
        assertNotNull(saved.getId());
        // Should use default value
        assertNotNull(saved.getMaxMembers());
    }

    // ========== EDGE CASE TESTS ==========

    /**
     * Tests that attempting to add the same member twice doesn't increase count.
     */
    @Test
    void shouldNotAddDuplicateMember() {
        // Arrange: create and save an admin user
        User admin = new User();
        admin.setEmail("admin-duplicate@example.com");
        admin.setSurname("Admin");
        admin.setLastname("Duplicate");
        savedAdmin = userRepository.save(admin);

        // Arrange: create user and group
        User member = new User();
        member.setEmail("duplicate@example.com");
        member.setSurname("Dup");
        member.setLastname("Member");
        member = userRepository.save(member);

        Group group = new Group();
        group.setName("Duplicate Member Test");
        group.setCreatedBy(savedAdmin);
        Group saved = groupRepository.save(group);

        // Act: add member twice
        boolean added1 = saved.addMember(member);
        boolean added2 = saved.addMember(member);

        // Assert
        assertTrue(added1, "First add should succeed");
        assertFalse(added2, "Second add should fail (duplicate)");
        assertEquals(1, saved.getMembers().size());
    }

    /**
     * Tests group name with special characters and unicode.
     */
    @Test
    void shouldHandleNameWithSpecialCharacters() {
        // Arrange: create and save an admin user
        User admin = new User();
        admin.setEmail("admin-specialchars@example.com");
        admin.setSurname("Admin");
        admin.setLastname("SpecialChars");
        savedAdmin = userRepository.save(admin);

        // Arrange: create group with special characters
        Group group = new Group();
        group.setName("Café & Restaurant 中文 😀");
        group.setCreatedBy(savedAdmin);

        // Act
        Group saved = groupRepository.saveAndFlush(group);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("Café & Restaurant 中文 😀", saved.getName());
    }

    /**
     * Tests that createdBy is required (NOT NULL constraint).
     */
    @Test
    void shouldFailToSaveGroupWithNullCreatedBy() {
        // Arrange: create group without createdBy
        Group group = new Group();
        group.setName("No Creator Group");
        // Not setting createdBy

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class,
                () -> groupRepository.saveAndFlush(group));
    }
}
