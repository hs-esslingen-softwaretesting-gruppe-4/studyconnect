package de.softwaretesting.studyconnect.controllers;

import de.softwaretesting.studyconnect.dtos.request.CreateGroupRequestDTO;
import de.softwaretesting.studyconnect.dtos.request.UpdateGroupRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.GroupResponseDTO;
import de.softwaretesting.studyconnect.services.GroupService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class GroupController {

  private final GroupService groupService;

  @GetMapping
  ResponseEntity<List<GroupResponseDTO>> getAllGroupsPublicGroups() {
    return groupService.getAllPublicGroups();
  }

  @PostMapping
  ResponseEntity<GroupResponseDTO> createGroup(@RequestBody @Valid CreateGroupRequestDTO dto) {
    return this.groupService.createGroup(dto);
  }

  @GetMapping("{groupId}")
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
   * @returns a ResponseEntity containing the updated GroupResponseDTO
   */
  @PatchMapping("{groupId}")
  ResponseEntity<GroupResponseDTO> updateGroup(
      @PathVariable Long groupId, @RequestBody @Valid UpdateGroupRequestDTO dto) {
    return groupService.updateGroup(groupId, dto);
  }

  @DeleteMapping("{groupId}")
  ResponseEntity<Void> deleteGroup(@PathVariable Long groupId) {
    return groupService.deleteGroup(groupId);
  }

  @DeleteMapping("{groupId}/members/{userId}")
  ResponseEntity<Void> removeMemberFromGroup(
      @PathVariable Long groupId, @PathVariable Long userId) {
    return groupService.removeMemberFromGroup(groupId, userId);
  }

  @DeleteMapping("{groupId}/admins/{userId}")
  ResponseEntity<Void> removeAdminFromGroup(@PathVariable Long groupId, @PathVariable Long userId) {
    return groupService.removeAdminFromGroup(groupId, userId);
  }

  @PostMapping("/join/{inviteCode}/{userId}")
  ResponseEntity<Void> joinGroupByInvitationCode(
      @PathVariable String inviteCode, @PathVariable Long userId) {
    return groupService.joinGroupByInvitationCode(inviteCode, userId);
  }
}
