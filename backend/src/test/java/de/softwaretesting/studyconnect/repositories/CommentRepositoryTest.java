package de.softwaretesting.studyconnect.repositories;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import de.softwaretesting.studyconnect.models.Comment;
import de.softwaretesting.studyconnect.models.Group;
import de.softwaretesting.studyconnect.models.User;

@DataJpaTest
@ActiveProfiles("test")
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;
    private User savedAdmin;
    private Group savedGroup;

    /**
     * Tests that a Comment object can be successfully created and saved.
     */
    
    @Test
    void shouldCreateAndSaveCommentWithValidData() {

        //Arange User and Group
        User user = new User();
        user.setEmail("test@example.com");
        user.setSurname("John");
        user.setLastname("Doe");

        savedUser = userRepository.save(user);
    
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
        savedGroup = groupRepository.save(group);
    

        //Create & fill Comment object
        Comment comment = new Comment();
        comment.setCreatedBy(savedUser);
        comment.setCreatedIn(savedGroup);
        comment.setContent("This is a text with 1 number and a special €haracter.");

        //Save Comment
        Comment saved = commentRepository.save(comment);

        // Assertion
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedBy());
        assertNotNull(saved.getCreatedIn());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals("This is a text with 1 number and a special €haracter.", saved.getContent());
        assertEquals(savedUser, saved.getCreatedBy());
        assertEquals(savedGroup, saved.getCreatedIn());
    }

    /*
     *  Test that saving a comment with a null createdBy
     */
    @Test
    void shouldFailToSaveCommentWhenCreatedByIsNull() {
        // Arrange: create and save an admin user for the group
        User admin = new User();
        admin.setEmail("admin-null-test@example.com");
        admin.setSurname("Admin");
        admin.setLastname("User");
        savedAdmin = userRepository.save(admin);

        // Arrange: create a group (Group requires createdBy)
        Group group = new Group();
        group.setName("Test Group for Null CreatedBy");
        group.setDescription("A group to test null createdBy in comments");
        group.setCreatedBy(savedAdmin);
        savedGroup = groupRepository.save(group);

        Comment comment = new Comment();
        comment.setCreatedIn(savedGroup);

        assertThrows(DataIntegrityViolationException.class,
        () -> commentRepository.saveAndFlush(comment));
    }

    /*
     *  Test that saving a comment with a null createdIn
     */
    @Test
    void shouldFailToSaveCommentWhenCreatedInIsNull() {
        // Arrange: create and save a user
        User user = new User();
        user.setEmail("user-null-test@example.com");
        user.setSurname("Test");
        user.setLastname("User");
        savedUser = userRepository.save(user);

        Comment comment = new Comment();
        comment.setCreatedBy(savedUser);

        assertThrows(DataIntegrityViolationException.class,
        () -> commentRepository.saveAndFlush(comment));
    }
}