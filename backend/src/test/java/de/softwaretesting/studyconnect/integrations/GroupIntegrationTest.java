package de.softwaretesting.studyconnect.integrations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.softwaretesting.studyconnect.mappers.request.CreateGroupRequestMapper;
import de.softwaretesting.studyconnect.mappers.response.GroupResponseMapper;
import de.softwaretesting.studyconnect.models.Group;
import de.softwaretesting.studyconnect.models.User;
import de.softwaretesting.studyconnect.repositories.GroupRepository;
import de.softwaretesting.studyconnect.repositories.UserRepository;
import de.softwaretesting.studyconnect.services.GroupService;
import de.softwaretesting.studyconnect.services.TaskService;
import de.softwaretesting.studyconnect.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class GroupIntegrationTest {

  @Autowired private GroupRepository groupRepository;

  @Autowired private UserRepository userRepository;

  private GroupService groupService;
  private UserService userService;
  private User user1;
  private User user2;
  private Group group;

  @BeforeEach
  void setUp() {
    userService = mock(UserService.class);
    groupService =
        new GroupService(
            groupRepository,
            userService,
            mock(TaskService.class),
            mock(CreateGroupRequestMapper.class),
            mock(GroupResponseMapper.class));

    user1 = new User();
    user1.setEmail("creator@example.com");
    user1.setFirstname("Creator");
    user1.setLastname("User");
    userRepository.save(user1);

    user2 = new User();
    user2.setEmail("member@example.com");
    user2.setFirstname("Member");
    user2.setLastname("User");
    userRepository.save(user2);

    group = new Group();
    group.setName("Integration Group");
    group.setCreatedBy(user1);
    group.setMaxMembers(5);
    group.addMember(user1);
    groupRepository.save(group);
  }

  @Test
  void joinGroupById_persistsMember() {
    when(userService.retrieveUserById(user2.getId())).thenReturn(user2);

    ResponseEntity<Void> result = groupService.joinGroupById(group.getId(), user2.getId());

    assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    Group updated = groupRepository.findById(group.getId()).orElseThrow();
    assertTrue(updated.getMembers().contains(user2));
    assertEquals(2, updated.getMemberCount());
  }
}
