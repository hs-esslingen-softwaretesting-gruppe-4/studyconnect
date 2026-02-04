package de.softwaretesting.studyconnect.controllers;

import de.softwaretesting.studyconnect.dtos.request.CreateGroupRequestDTO;
import de.softwaretesting.studyconnect.dtos.request.UpdateGroupRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.GroupResponseDTO;
import de.softwaretesting.studyconnect.dtos.response.UserResponseDTO;
import de.softwaretesting.studyconnect.exceptions.BadRequestException;
import de.softwaretesting.studyconnect.services.GroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
@Validated
public class GroupController {

  private final GroupService groupService;

  @GetMapping
  ResponseEntity<List<GroupResponseDTO>> getAllPublicGroups() {
    return groupService.getAllPublicGroups();
  }

  /**
   * Searches for public groups by name.
   *
   * @param query the search query string
   * @return a ResponseEntity containing a list of GroupResponseDTOs that match the search criteria
   */
  @GetMapping("/search")
  ResponseEntity<List<GroupResponseDTO>> searchPublicGroups(
      @RequestParam @NotBlank(message = "Query must not be blank") String query) {
    if (query == null || query.isBlank()) {
      throw new BadRequestException("Query must not be blank");
    }
    return groupService.searchPublicGroups(query);
  }

  @PostMapping
  ResponseEntity<GroupResponseDTO> createGroup(@RequestBody @Valid CreateGroupRequestDTO dto) {
    return this.groupService.createGroup(dto);
  }

  @GetMapping("/{groupId}")
  ResponseEntity<GroupResponseDTO> getGroupById(@PathVariable Long groupId) {
    return groupService.getGroupById(groupId);
  }

  @GetMapping("/membership/{userId}")
  ResponseEntity<List<GroupResponseDTO>> getGroupsByUserId(@PathVariable Long userId) {
    return groupService.getGroupsByUserId(userId);
  }

  /**
   * Updates an existing group. This endpoint allows adding members and admins, but not removing
   * them
   *
   * @param groupId the ID of the group to update
   * @param dto the UpdateGroupRequestDTO containing the updated group data
   * @return a ResponseEntity containing the updated GroupResponseDTO
   */
  @PatchMapping("/{groupId}")
  ResponseEntity<GroupResponseDTO> updateGroup(
      @PathVariable Long groupId, @RequestBody @Valid UpdateGroupRequestDTO dto) {
    return groupService.updateGroup(groupId, dto);
  }

  /**
   * Deletes a group by its ID.
   *
   * @param groupId the ID of the group to delete
   * @return a ResponseEntity with no content
   */
  @DeleteMapping("/{groupId}")
  ResponseEntity<Void> deleteGroup(@PathVariable Long groupId) {
    return groupService.deleteGroup(groupId);
  }

  /**
   * Get members of a group by group ID.
   *
   * @param groupId the ID of the group
   * @return a ResponseEntity containing a set of UserResponseDTOs representing the members of the
   *     group
   */
  @GetMapping("/{groupId}/members")
  ResponseEntity<Set<UserResponseDTO>> getMembersByGroupId(@PathVariable Long groupId) {
    return groupService.getMembersByGroupId(groupId);
  }

  /**
   * Get admins of a group by group ID.
   *
   * @param groupId the ID of the group
   * @return a ResponseEntity containing a set of UserResponseDTOs representing the admins of the
   *     group
   */
  @GetMapping("/{groupId}/admins")
  ResponseEntity<Set<UserResponseDTO>> getAdminsByGroupId(@PathVariable Long groupId) {
    return groupService.getAdminsByGroupId(groupId);
  }

  @DeleteMapping("/{groupId}/members/{userId}")
  ResponseEntity<Void> removeMemberFromGroup(
      @PathVariable Long groupId, @PathVariable Long userId) {
    return groupService.removeMemberFromGroup(groupId, userId);
  }

  @DeleteMapping("/{groupId}/admins/{userId}")
  ResponseEntity<Void> removeAdminFromGroup(@PathVariable Long groupId, @PathVariable Long userId) {
    return groupService.removeAdminFromGroup(groupId, userId);
  }

  @PostMapping("/{groupId}/join")
  ResponseEntity<Void> joinGroupById(@PathVariable Long groupId, @RequestParam Long userId) {
    return groupService.joinGroupById(groupId, userId);
  }

  @PostMapping("/join/{inviteCode}/{userId}")
  ResponseEntity<Void> joinGroupByInvitationCode(
      @PathVariable String inviteCode, @PathVariable Long userId) {
    return groupService.joinGroupByInvitationCode(inviteCode, userId);
  }
}
