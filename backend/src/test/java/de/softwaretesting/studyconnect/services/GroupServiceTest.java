package de.softwaretesting.studyconnect.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.softwaretesting.studyconnect.dtos.request.CreateGroupRequestDTO;
import de.softwaretesting.studyconnect.dtos.request.UpdateGroupRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.GroupResponseDTO;
import de.softwaretesting.studyconnect.exceptions.BadRequestException;
import de.softwaretesting.studyconnect.exceptions.InternalServerErrorException;
import de.softwaretesting.studyconnect.exceptions.NotFoundException;
import de.softwaretesting.studyconnect.mappers.request.CreateGroupRequestMapper;
import de.softwaretesting.studyconnect.mappers.response.GroupResponseMapper;
import de.softwaretesting.studyconnect.models.Group;
import de.softwaretesting.studyconnect.models.User;
import de.softwaretesting.studyconnect.repositories.GroupRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class GroupServiceTest {

  @Mock private GroupRepository groupRepository;

  @Mock private UserService userService;

  @Mock private CreateGroupRequestMapper groupRequestMapper;

  @Mock private GroupResponseMapper groupResponseMapper;

  @InjectMocks private GroupService groupService;

  private User user1;
  private User user2;
  private Group group;
  private GroupResponseDTO responseDto;
  private CreateGroupRequestDTO createDto;

  @BeforeEach
  void setUp() {
    user1 = new User();
    user1.setId(1L);
    user1.setEmail("user1@example.com");

    user2 = new User();
    user2.setId(2L);
    user2.setEmail("user2@example.com");

    group = new Group();
    group.setId(10L);
    group.setName("Test Group");
    group.setMaxMembers(5);
    group.setMemberCount(1);
    group.setCreatedBy(user1);
    group.setInviteCode("invite");
    group.getMembers().add(user1);
    group.getAdmins().add(user1);

    responseDto =
        new GroupResponseDTO(
            10L, "Test Group", "desc", false, 1L, null, null, 1, 5, "invite", Set.of(1L));

    createDto =
        new CreateGroupRequestDTO(
            "Test Group", "desc", true, 5, Set.of(1L, 2L), Set.of(1L), user1.getId());
  }

  @Test
  void getAllPublicGroups_returnsMappedList() {
    when(groupRepository.findByIsPublicTrue()).thenReturn(Optional.of(List.of(group)));
    when(groupResponseMapper.toDtoList(List.of(group))).thenReturn(List.of(responseDto));

    ResponseEntity<List<GroupResponseDTO>> result = groupService.getAllPublicGroups();

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(1, result.getBody().size());
    verify(groupRepository).findByIsPublicTrue();
    verify(groupResponseMapper).toDtoList(List.of(group));
  }

  @Test
  void getGroupById_existingGroup_returnsDto() {
    when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
    when(groupResponseMapper.toDto(group)).thenReturn(responseDto);

    ResponseEntity<GroupResponseDTO> result = groupService.getGroupById(10L);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(responseDto, result.getBody());
    verify(groupRepository).findById(10L);
    verify(groupResponseMapper).toDto(group);
  }

  @Test
  void getGroupById_missingGroup_throwsNotFound() {
    when(groupRepository.findById(10L)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> groupService.getGroupById(10L));
  }

  @Test
  void getGroupsByUserId_returnsMappedList() {
    when(groupRepository.findByMembersId(1L)).thenReturn(Optional.of(List.of(group)));
    when(groupResponseMapper.toDtoList(List.of(group))).thenReturn(List.of(responseDto));

    ResponseEntity<List<GroupResponseDTO>> result = groupService.getGroupsByUserId(1L);

    assertEquals(1, result.getBody().size());
    verify(groupRepository).findByMembersId(1L);
    verify(groupResponseMapper).toDtoList(List.of(group));
  }

  @Test
  void createGroup_happyPath_savesAndReturnsCreated() {
    Group mappedGroup = new Group();
    mappedGroup.setAdmins(new java.util.HashSet<>());
    mappedGroup.setMembers(new java.util.HashSet<>());

    when(groupRequestMapper.toEntity(createDto)).thenReturn(mappedGroup);
    when(userService.getUsersByIdMap(Set.of(1L, 2L))).thenReturn(Map.of(1L, user1, 2L, user2));
    when(groupRepository.saveAndFlush(mappedGroup)).thenReturn(group);
    when(groupResponseMapper.toDto(group)).thenReturn(responseDto);

    ResponseEntity<GroupResponseDTO> result = groupService.createGroup(createDto);

    assertEquals(HttpStatus.CREATED, result.getStatusCode());
    assertEquals(responseDto, result.getBody());
    verify(groupRepository).saveAndFlush(mappedGroup);
    assertEquals(Set.of(user1, user2), mappedGroup.getMembers());
    assertEquals(Set.of(user1), mappedGroup.getAdmins());
    assertEquals(user1, mappedGroup.getCreatedBy());
  }

  @Test
  void createGroup_missingUser_throwsBadRequest() {
    Group mappedGroup = new Group();
    when(groupRequestMapper.toEntity(createDto)).thenReturn(mappedGroup);
    when(userService.getUsersByIdMap(Set.of(1L, 2L))).thenReturn(Map.of(1L, user1));
    CreateGroupRequestDTO dto = createDto;

    assertThrows(BadRequestException.class, () -> groupService.createGroup(dto));
    verify(groupRepository, never()).saveAndFlush(any());
  }

  @Test
  void createGroup_inviteCodeConflictRetriesThenSucceeds() {
    Group mappedGroup = new Group();
    mappedGroup.setAdmins(new java.util.HashSet<>());
    mappedGroup.setMembers(new java.util.HashSet<>());
    DataIntegrityViolationException inviteConflict =
        new DataIntegrityViolationException(
            "constraint", new ConstraintViolationException("dup", null, "invite_code_unique"));

    when(groupRequestMapper.toEntity(createDto)).thenReturn(mappedGroup);
    when(userService.getUsersByIdMap(Set.of(1L, 2L))).thenReturn(Map.of(1L, user1, 2L, user2));
    when(groupRepository.saveAndFlush(mappedGroup)).thenThrow(inviteConflict).thenReturn(group);
    when(groupResponseMapper.toDto(group)).thenReturn(responseDto);

    ResponseEntity<GroupResponseDTO> result = groupService.createGroup(createDto);

    assertEquals(HttpStatus.CREATED, result.getStatusCode());
    verify(groupRepository, times(2)).saveAndFlush(mappedGroup);
  }

  @Test
  void createGroup_nonInviteConstraintViolation_throwsInternalServerError() {
    Group mappedGroup = new Group();
    mappedGroup.setAdmins(new java.util.HashSet<>());
    mappedGroup.setMembers(new java.util.HashSet<>());
    DataIntegrityViolationException nonInvite =
        new DataIntegrityViolationException("other constraint");

    when(groupRequestMapper.toEntity(createDto)).thenReturn(mappedGroup);
    when(userService.getUsersByIdMap(Set.of(1L, 2L))).thenReturn(Map.of(1L, user1, 2L, user2));
    when(groupRepository.saveAndFlush(mappedGroup)).thenThrow(nonInvite);

    assertThrows(InternalServerErrorException.class, () -> groupService.createGroup(createDto));
  }

  @Test
  void createGroup_inviteConflictExhaustsRetries_throwsInternalServerError() {
    Group mappedGroup = new Group();
    mappedGroup.setAdmins(new java.util.HashSet<>());
    mappedGroup.setMembers(new java.util.HashSet<>());
    DataIntegrityViolationException inviteConflict =
        new DataIntegrityViolationException(
            "constraint", new ConstraintViolationException("dup", null, "invite_code_unique"));

    when(groupRequestMapper.toEntity(createDto)).thenReturn(mappedGroup);
    when(userService.getUsersByIdMap(Set.of(1L, 2L))).thenReturn(Map.of(1L, user1, 2L, user2));
    when(groupRepository.saveAndFlush(mappedGroup)).thenThrow(inviteConflict);

    assertThrows(InternalServerErrorException.class, () -> groupService.createGroup(createDto));
    verify(groupRepository, times(2)).saveAndFlush(mappedGroup);
  }

  @Test
  void updateGroup_memberLimitReached_throwsBadRequest() {
    group.setMemberCount(5);
    group.setMaxMembers(5);
    UpdateGroupRequestDTO updateDto =
        new UpdateGroupRequestDTO("New", "desc", false, Set.of(3L), null);
    Long groupId = group.getId();
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

    assertThrows(BadRequestException.class, () -> groupService.updateGroup(groupId, updateDto));
  }

  @Test
  void updateGroup_memberAlreadyPresent_throwsBadRequest() {
    group.setMaxMembers(5);
    group.setMemberCount(1);
    UpdateGroupRequestDTO updateDto =
        new UpdateGroupRequestDTO("New", "desc", false, Set.of(1L), null);
    Long groupId = group.getId();
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

    assertThrows(BadRequestException.class, () -> groupService.updateGroup(groupId, updateDto));
  }

  @Test
  void updateGroup_adminAlreadyAdmin_throwsBadRequest() {
    UpdateGroupRequestDTO updateDto =
        new UpdateGroupRequestDTO("New", "desc", false, null, Set.of(1L));
    Long groupId = group.getId();
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

    assertThrows(BadRequestException.class, () -> groupService.updateGroup(groupId, updateDto));
  }

  @Test
  void updateGroup_adminNotMember_throwsBadRequest() {
    UpdateGroupRequestDTO updateDto =
        new UpdateGroupRequestDTO("New", "desc", false, null, Set.of(2L));
    Long groupId = group.getId();
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

    assertThrows(BadRequestException.class, () -> groupService.updateGroup(groupId, updateDto));
  }

  @Test
  void updateGroup_successfullyAppliesPatch() {
    UpdateGroupRequestDTO updateDto =
        new UpdateGroupRequestDTO("New Name", "New Desc", true, Set.of(2L), Set.of(2L));
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(userService.retrieveUserById(2L)).thenReturn(user2);
    when(groupRepository.save(group)).thenReturn(group);
    when(groupResponseMapper.toDto(group)).thenReturn(responseDto);

    ResponseEntity<GroupResponseDTO> result = groupService.updateGroup(group.getId(), updateDto);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    verify(groupRepository).save(group);
    assertEquals("New Name", group.getName());
    assertEquals("New Desc", group.getDescription());
    assertEquals(Set.of(user1, user2), group.getAdmins());
    assertEquals(Set.of(user1, user2), group.getMembers());
  }

  @Test
  void removeMemberFromGroup_notMember_throwsBadRequest() {
    User other = new User();
    other.setId(99L);
    Long groupId = group.getId();
    Long otherId = other.getId();
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
    when(userService.retrieveUserById(otherId)).thenReturn(other);

    assertThrows(
        BadRequestException.class, () -> groupService.removeMemberFromGroup(groupId, otherId));
    verify(groupRepository, never()).save(any(Group.class));
  }

  @Test
  void removeMemberFromGroup_successPersists() {
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(userService.retrieveUserById(user1.getId())).thenReturn(user1);

    ResponseEntity<Void> result = groupService.removeMemberFromGroup(group.getId(), user1.getId());

    assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    verify(groupRepository).save(group);
  }

  @Test
  void removeAdminFromGroup_notAdmin_throwsBadRequest() {
    Long groupId = group.getId();
    Long user2Id = user2.getId();
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
    when(userService.retrieveUserById(user2Id)).thenReturn(user2);

    assertThrows(
        BadRequestException.class, () -> groupService.removeAdminFromGroup(groupId, user2Id));
    verify(groupRepository, never()).save(any(Group.class));
  }

  @Test
  void removeAdminFromGroup_successPersists() {
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(userService.retrieveUserById(user1.getId())).thenReturn(user1);

    ResponseEntity<Void> result = groupService.removeAdminFromGroup(group.getId(), user1.getId());

    assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    verify(groupRepository).save(group);
  }

  @Test
  void deleteGroup_missing_throwsNotFound() {
    when(groupRepository.findById(11L)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> groupService.deleteGroup(11L));
  }

  @Test
  void deleteGroup_successDeletes() {
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

    ResponseEntity<Void> result = groupService.deleteGroup(group.getId());

    assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    verify(groupRepository).delete(group);
  }

  @Test
  void joinGroupByInvitationCode_groupFull_throwsBadRequest() {
    group.setMemberCount(group.getMaxMembers());
    when(groupRepository.findByInviteCode("code")).thenReturn(Optional.of(group));

    assertThrows(
        BadRequestException.class, () -> groupService.joinGroupByInvitationCode("code", 99L));
    verify(groupRepository, never()).save(any(Group.class));
  }

  @Test
  void joinGroupByInvitationCode_alreadyMember_throwsBadRequest() {
    when(groupRepository.findByInviteCode("code")).thenReturn(Optional.of(group));
    when(userService.retrieveUserById(user1.getId())).thenReturn(user1);

    assertThrows(
        BadRequestException.class, () -> groupService.joinGroupByInvitationCode("code", 1L));
  }

  @Test
  void joinGroupByInvitationCode_successAddsMember() {
    Group joinableGroup = new Group();
    joinableGroup.setMaxMembers(5);
    joinableGroup.setMemberCount(0);
    when(groupRepository.findByInviteCode("code")).thenReturn(Optional.of(joinableGroup));
    when(userService.retrieveUserById(user2.getId())).thenReturn(user2);
    when(groupRepository.save(joinableGroup)).thenReturn(joinableGroup);

    ResponseEntity<Void> result = groupService.joinGroupByInvitationCode("code", user2.getId());

    assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    verify(groupRepository).save(joinableGroup);
    assertNotNull(joinableGroup.getMembers());
    assertEquals(Set.of(user2), joinableGroup.getMembers());
  }
}
