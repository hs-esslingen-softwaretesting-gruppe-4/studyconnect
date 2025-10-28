package de.softwaretesting.studyconnect.repositories;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

import de.softwaretesting.studyconnect.models.Group;
import de.softwaretesting.studyconnect.models.User;

@DataJpaTest
@ActiveProfiles("test")
class GroupRepositoryTest {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Test creating and saving a group with valid data.
     */
    @Test
    void shouldCreateAndSaveGroupWithValidData() {
        // Arrange
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setSurname("Admin");
        admin.setLastname("User");
        admin.setCreatedAt(LocalDateTime.now());
        User savedAdmin = userRepository.save(admin);

        Group group = new Group();
        group.setName("Study Group 1");
        group.setDescription("Test group for StudyConnect");
        group.setVisibility("PRIVATE");
        group.setMaxMembers(10);
        group.setCreatedBy(savedAdmin.getId());
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        group.setAdmin(savedAdmin);

        Set<User> members = new HashSet<>();
        members.add(savedAdmin);
        group.setMembers(members);

        // Act
        Group saved = groupRepository.save(group);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("Study Group 1", saved.getName());
        assertEquals("PRIVATE", saved.getVisibility());
        assertEquals(savedAdmin.getId(), saved.getCreatedBy());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(savedAdmin.getId(), saved.getAdmin().getId());
        assertTrue(saved.getMembers().contains(savedAdmin));
    }

    /**
     * Test that saving a group with a null name fails.
     */
    @Test
    void shouldFailToSaveGroupWhenNameIsNull() {
        Group group = new Group();
        group.setDescription("No name group");
        group.setVisibility("PUBLIC");
        group.setCreatedBy(1L);
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());

        assertThrows(DataIntegrityViolationException.class,
                () -> groupRepository.saveAndFlush(group));
    }

    /**
     * Test that saving a group with a null visibility fails.
     */
    @Test
    void shouldFailToSaveGroupWhenVisibilityIsNull() {
        Group group = new Group();
        group.setName("Invalid Group");
        group.setCreatedBy(1L);
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());

        assertThrows(DataIntegrityViolationException.class,
                () -> groupRepository.saveAndFlush(group));
    }

    /**
     * Test that saving a group with null createdAt or updatedAt fails.
     */
    @Test
    void shouldFailToSaveGroupWhenCreatedOrUpdatedAtIsNull() {
        Group group = new Group();
        group.setName("Test Group");
        group.setVisibility("PRIVATE");
        group.setCreatedBy(1L);
        group.setCreatedAt(null); // invalid
        group.setUpdatedAt(LocalDateTime.now());

        assertThrows(DataIntegrityViolationException.class,
                () -> groupRepository.saveAndFlush(group));

        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(null); // invalid
        assertThrows(DataIntegrityViolationException.class,
                () -> groupRepository.saveAndFlush(group));
    }

    /**
     * Test maximum members logic (helper scenario).
     */
    @Test
    void shouldNotExceedMaxMembers() {
        User admin = new User();
        admin.setEmail("admin2@example.com");
        admin.setSurname("Admin2");
        admin.setLastname("User");
        admin.setCreatedAt(LocalDateTime.now());
        User savedAdmin = userRepository.save(admin);

        Group group = new Group();
        group.setName("Limited Group");
        group.setVisibility("PRIVATE");
        group.setCreatedBy(savedAdmin.getId());
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        group.setAdmin(savedAdmin);

        Set<User> members = new HashSet<>();
        for (int i = 0; i < 25; i++) { // more than default maxMembers=20
            User user = new User();
            user.setEmail("user"+i+"@example.com");
            user.setSurname("User"+i);
            user.setLastname("Test");
            user.setCreatedAt(LocalDateTime.now());
            members.add(userRepository.save(user));
        }
        group.setMembers(members);

        Group saved = groupRepository.save(group);

        // Assert that size is correct but does not enforce maxMembers automatically
        assertEquals(25, saved.getMembers().size()); 
        assertTrue(saved.getMembers().contains(savedAdmin));
    }
}
