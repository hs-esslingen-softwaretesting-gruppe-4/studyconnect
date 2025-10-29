package de.softwaretesting.studyconnect.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import de.softwaretesting.studyconnect.models.Comment;
import de.softwaretesting.studyconnect.models.Group;
import de.softwaretesting.studyconnect.models.User;

@DataJpaTest
@ActiveProfiles("test")
public class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

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
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
    
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
        Group savedGroup = groupRepository.save(group);
    

        //Create & fill Comment object
        Comment comment = new Comment();
        comment.setCreatedBy(savedUser.getId());
        comment.setCreatedIn(savedGroup.getId());
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
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
        assertEquals(savedUser.getId(), saved.getCreatedBy());
        assertEquals(savedGroup.getId(), saved.getCreatedIn());
    }

    /*
     *  Test that saving a comment with a null createdBy
     */
    @Test
    void shouldFailToSaveCommentWhenCreatedByIsNull() {
        Comment comment = new Comment();
        comment.setCreatedIn(1L);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        assertThrows(DataIntegrityViolationException.class,
        () -> commentRepository.saveAndFlush(comment));
    }

    /*
     *  Test that saving a comment with a null createdIn
     */
    @Test
    void shouldFailToSaveCommentWhenCreatedInIsNull() {
        Comment comment = new Comment();
        comment.setCreatedBy(1L);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        assertThrows(DataIntegrityViolationException.class,
        () -> commentRepository.saveAndFlush(comment));
    }
    
    /**
     * Test that saving a group with null createdAt fails.
     */
    @Test
    void shouldFailToSaveCommentWhenCreatedAtIsNull() {
        Comment comment = new Comment();
        comment.setCreatedIn(1L);
        comment.setCreatedBy(1L);
        comment.setCreatedAt(null);

        assertThrows(DataIntegrityViolationException.class,
        () -> commentRepository.saveAndFlush(comment));        
    }

    /**
     * Test that saving a group with null updatedAt fails.
     */
    @Test
    void shouldFailToSaveCommentWhenUpdatedAtIsNull() {
        Comment comment = new Comment();
        comment.setCreatedIn(1L);
        comment.setCreatedBy(1L);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(null);

        assertThrows(DataIntegrityViolationException.class,
        () -> commentRepository.saveAndFlush(comment));
    }
}