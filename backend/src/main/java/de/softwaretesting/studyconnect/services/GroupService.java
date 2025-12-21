package de.softwaretesting.studyconnect.services;

import de.softwaretesting.studyconnect.dtos.request.CreateGroupRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.GroupResponseDTO;
import de.softwaretesting.studyconnect.exceptions.BadRequestException;
import de.softwaretesting.studyconnect.exceptions.InternalServerErrorException;
import de.softwaretesting.studyconnect.exceptions.NotFoundException;
import de.softwaretesting.studyconnect.mappers.request.CreateGroupRequestMapper;
import de.softwaretesting.studyconnect.mappers.response.GroupResponseMapper;
import de.softwaretesting.studyconnect.models.Group;
import de.softwaretesting.studyconnect.models.User;
import de.softwaretesting.studyconnect.repositories.GroupRepository;
import java.beans.JavaBean;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@JavaBean
public class GroupService {

  private static final int INVITE_CODE_SAVE_RETRY_ATTEMPTS = 2;
  private static final Logger LOGGER = LoggerFactory.getLogger(GroupService.class);

  private final GroupRepository groupRepository;
  private final UserService userService;
  private final CreateGroupRequestMapper groupRequestMapper;
  private final GroupResponseMapper groupResponseMapper;

  /**
   * Retrieves all public groups.
   *
   * @return a ResponseEntity containing a list of GroupResponseDTOs for all public groups
   */
  public ResponseEntity<List<GroupResponseDTO>> getAllPublicGroups() {
    Optional<List<Group>> publicGroups = groupRepository.findByIsPublicTrue();
    List<GroupResponseDTO> dtoList = groupResponseMapper.toDtoList(publicGroups.orElse(List.of()));
    return ResponseEntity.ok(dtoList);
  }

  /**
   * Retrieves a group by its ID.
   *
   * @param groupId the ID of the group to retrieve
   * @return a ResponseEntity containing the GroupResponseDTO for the specified group
   * @throws NotFoundException if the group with the specified ID does not exist
   */
  public ResponseEntity<GroupResponseDTO> getGroupById(Long groupId) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new NotFoundException("Group not found with id: " + groupId));
    GroupResponseDTO dto = groupResponseMapper.toDto(group);
    return ResponseEntity.ok(dto);
  }

  /**
   * Retrieves all groups that a user is a member of.
   *
   * @param userId the ID of the user
   * @return a ResponseEntity containing a list of GroupResponseDTOs for groups the user is a member
   *     of
   */
  public ResponseEntity<List<GroupResponseDTO>> getGroupsByUserId(Long userId) {
    Optional<List<Group>> groups = groupRepository.findByMembersId(userId);
    List<GroupResponseDTO> dtoList = groupResponseMapper.toDtoList(groups.orElse(List.of()));
    return ResponseEntity.ok(dtoList);
  }

  /**
   * Creates a new group with the specified details.
   *
   * @param dto the data transfer object containing group details
   * @return a ResponseEntity containing the created group's response DTO
   * @throws
   */
  public ResponseEntity<GroupResponseDTO> createGroup(CreateGroupRequestDTO dto) {

    Group group = groupRequestMapper.toEntity(dto);

    Set<Long> allReferencedUserIds = new HashSet<>();
    allReferencedUserIds.addAll(dto.getAdminIds());
    allReferencedUserIds.addAll(dto.getMemberIds());
    allReferencedUserIds.add(dto.getCreatedById());
    Map<Long, User> usersById = userService.getUsersByIdMap(allReferencedUserIds);

    Set<Long> missing = new HashSet<>(allReferencedUserIds);
    missing.removeAll(usersById.keySet());
    if (!missing.isEmpty()) {
      throw new BadRequestException("One or more users not found: " + missing);
    }

    for (Long adminId : dto.getAdminIds()) {
      group.getAdmins().add(usersById.get(adminId));
    }
    for (Long memberId : dto.getMemberIds()) {
      group.getMembers().add(usersById.get(memberId));
    }

    group.setCreatedBy(usersById.get(dto.getCreatedById()));
    try {
      Group savedGroup = saveWithInviteCodeRetry(group);
      return new ResponseEntity<>(groupResponseMapper.toDto(savedGroup), HttpStatus.CREATED);
    } catch (DataIntegrityViolationException e) {
      throw new InternalServerErrorException("Error saving group due to database constraint");
    } catch (IllegalStateException e) {
      throw new InternalServerErrorException("Error saving group after multiple attempts");
    }
  }

  public ResponseEntity<GroupResponseDTO> updateGroup(
      Long groupId, de.softwaretesting.studyconnect.dtos.request.UpdateGroupRequestDTO dto) {

    Group patchGroup =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new NotFoundException("Group not found with id: " + groupId));
    // First, check if group can be updated as a whole
    if (dto.getMemberIds() != null) {
      if (patchGroup.getMemberCount() >= patchGroup.getMaxMembers()) {
        throw new BadRequestException("Group has reached its maximum member limit");
      }
      // Ensure member is not already in group
      for (Long memberId : dto.getMemberIds()) {
        boolean isAlreadyMember =
            patchGroup.getMembers().stream().anyMatch(user -> user.getId().equals(memberId));
        if (isAlreadyMember) {
          throw new BadRequestException(
              "User with id " + memberId + " is already a member of the group");
        }
      }
    }
    if (dto.getAdminIds() != null) {
      Set<Long> prospectiveMemberIds = new HashSet<>();
      for (User member : patchGroup.getMembers()) {
        prospectiveMemberIds.add(member.getId());
      }
      if (dto.getMemberIds() != null) {
        prospectiveMemberIds.addAll(dto.getMemberIds());
      }
      // Ensure admin is not already an admin
      for (Long adminId : dto.getAdminIds()) {
        boolean isAlreadyAdmin =
            patchGroup.getAdmins().stream().anyMatch(user -> user.getId().equals(adminId));
        if (isAlreadyAdmin) {
          throw new BadRequestException(
              "User with id " + adminId + " is already an admin of the group");
        }
      }
      // Ensure admin is already a member
      for (Long adminId : dto.getAdminIds()) {
        boolean isMember = prospectiveMemberIds.contains(adminId);
        if (!isMember) {
          throw new BadRequestException(
              "User with id " + adminId + " must be a member to be an admin of the group");
        }
      }
    }

    // Now apply the patch
    if (dto.getName() != null) {
      patchGroup.setName(dto.getName());
    }
    if (dto.getDescription() != null) {
      patchGroup.setDescription(dto.getDescription());
    }
    if (dto.getIsPublic() != null) {
      patchGroup.setPublic(dto.getIsPublic());
    }
    if (dto.getMemberIds() != null) {
      for (Long memberId : dto.getMemberIds()) {
        User member = userService.retrieveUserById(memberId);
        patchGroup.getMembers().add(member);
      }
    }
    if (dto.getAdminIds() != null) {
      for (Long adminId : dto.getAdminIds()) {
        User admin = userService.retrieveUserById(adminId);
        patchGroup.getAdmins().add(admin);
      }
    }

    Group updatedGroup = groupRepository.save(patchGroup);
    GroupResponseDTO responseDTO = groupResponseMapper.toDto(updatedGroup);
    return ResponseEntity.ok(responseDTO);
  }

  /**
   * Removes a member from a group.
   *
   * @param groupId the ID of the group
   * @param userId the ID of the user to remove
   * @return a ResponseEntity with no content
   * @throws NotFoundException if the group or user does not exist
   * @throws BadRequestException if the user is not a member of the group
   */
  public ResponseEntity<Void> removeMemberFromGroup(Long groupId, Long userId) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new NotFoundException("Group not found with id: " + groupId));
    User user = userService.retrieveUserById(userId);
    boolean removed = group.removeMember(user);
    if (!removed) {
      throw new BadRequestException("User with id " + userId + " is not a member of the group");
    }
    groupRepository.save(group);
    return ResponseEntity.noContent().build();
  }

  /**
   * Removes an admin from a group.
   *
   * @param groupId the ID of the group
   * @param userId the ID of the user to remove as admin
   * @return a ResponseEntity with no content
   * @throws NotFoundException if the group or user does not exist
   * @throws BadRequestException if the user is not an admin of the group
   */
  public ResponseEntity<Void> removeAdminFromGroup(Long groupId, Long userId) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new NotFoundException("Group not found with id: " + groupId));
    User user = userService.retrieveUserById(userId);
    boolean removed = group.removeAdmin(user);
    if (!removed) {
      throw new BadRequestException("User with id " + userId + " is not an admin of the group");
    }
    groupRepository.save(group);
    return ResponseEntity.noContent().build();
  }

  /**
   * Deletes a group by its ID.
   *
   * @param groupId the ID of the group to delete
   * @return a ResponseEntity with no content
   * @throws NotFoundException if the group with the specified ID does not exist
   */
  public ResponseEntity<Void> deleteGroup(Long groupId) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new NotFoundException("Group not found with id: " + groupId));
    groupRepository.delete(group);
    return ResponseEntity.noContent().build();
  }

  /**
   * Allows a user to join a group using an invitation code.
   *
   * @param inviteCode the invitation code of the group
   * @param userId the ID of the user attempting to join
   * @return a ResponseEntity with no content
   * @throws NotFoundException if the group with the specified invite code does not exist
   * @throws BadRequestException if the group is full or the user is already a member
   */
  public ResponseEntity<Void> joinGroupByInvitationCode(String inviteCode, Long userId) {
    Group group =
        groupRepository
            .findByInviteCode(inviteCode)
            .orElseThrow(
                () -> new NotFoundException("Group not found with invite code: " + inviteCode));

    if (group.getMemberCount() >= group.getMaxMembers()) {
      throw new BadRequestException("Group has reached its maximum member limit");
    }

    User user = userService.retrieveUserById(userId);
    boolean added = group.addMember(user);
    if (!added) {
      throw new BadRequestException("User with id " + userId + " is already a member of the group");
    }

    groupRepository.save(group);
    return ResponseEntity.noContent().build();
  }

  /**
   * Attempts to save the group, retrying with a new invite code if a unique constraint violation
   * occurs on the invite_code column.
   *
   * @param group the group to save
   * @return the saved group
   * @throws DataIntegrityViolationException if a non-invite-code-related database error occurs
   * @throws IllegalStateException if unable to save after the configured number o attempts
   */
  private Group saveWithInviteCodeRetry(Group group) {
    if (group == null) {
      throw new IllegalArgumentException("group must not be null");
    }

    for (int attempt = 1; attempt <= INVITE_CODE_SAVE_RETRY_ATTEMPTS; attempt++) {
      try {
        return groupRepository.saveAndFlush(group);
      } catch (DataIntegrityViolationException ex) {
        boolean inviteCodeViolation = looksLikeInviteCodeUniqueViolation(ex);
        boolean isLastAttempt = attempt == INVITE_CODE_SAVE_RETRY_ATTEMPTS;
        if (!inviteCodeViolation) {
          throw ex;
        }
        if (isLastAttempt) {
          break;
        }

        LOGGER.warn(
            "Attempt {} to save group failed due to invite code conflict. Retrying...", attempt);
        group.regenerateInviteCode();
      }
    }

    // Defensive: loop always returns/throws.
    LOGGER.error(
        "Failed to save group after {} attempts due to invite code conflicts",
        INVITE_CODE_SAVE_RETRY_ATTEMPTS);
    throw new IllegalStateException("Failed to save group after retries");
  }

  /**
   * Analyzes a DataIntegrityViolationException to determine if it was caused by a unique constraint
   * violation on the invite_code column.
   *
   * @param ex the DataIntegrityViolationException to analyze
   * @return true if the exception appears to be due to an invite_code unique constraint violation,
   *     false otherwise
   */
  private static boolean looksLikeInviteCodeUniqueViolation(DataIntegrityViolationException ex) {
    Throwable current = ex;
    while (current != null) {
      if (current instanceof org.hibernate.exception.ConstraintViolationException cve) {
        String constraintName = cve.getConstraintName();
        if (constraintName != null && constraintName.toLowerCase().contains("invite")) {
          return true;
        }
      }

      String message = current.getMessage();
      if (message != null && message.toLowerCase().contains("invite_code")) {
        return true;
      }

      current = current.getCause();
    }
    return false;
  }
}
