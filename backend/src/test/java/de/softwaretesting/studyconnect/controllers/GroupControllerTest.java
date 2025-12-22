package de.softwaretesting.studyconnect.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.softwaretesting.studyconnect.dtos.request.CreateGroupRequestDTO;
import de.softwaretesting.studyconnect.dtos.request.UpdateGroupRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.GroupResponseDTO;
import de.softwaretesting.studyconnect.services.GroupService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("GroupController Tests")
class GroupControllerTest {

  @Mock private GroupService groupService;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new GroupController(groupService)).build();
    objectMapper = new ObjectMapper();
  }

  @Test
  @DisplayName("Should get all public groups")
  void shouldGetAllPublicGroups() throws Exception {
    GroupResponseDTO dto = sampleGroupResponse();
    given(groupService.getAllPublicGroups()).willReturn(ResponseEntity.ok(List.of(dto)));

    mockMvc
        .perform(get("/api/groups"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(dto.getId()))
        .andExpect(jsonPath("$[0].name").value(dto.getName()))
        .andExpect(jsonPath("$[0].inviteCode").value(dto.getInviteCode()));

    verify(groupService).getAllPublicGroups();
  }

  @Test
  @DisplayName("Should create group successfully")
  void shouldCreateGroupSuccessfully() throws Exception {
    CreateGroupRequestDTO request =
        new CreateGroupRequestDTO("New Group", "desc", true, 10, Set.of(1L, 2L), Set.of(1L), 1L);
    GroupResponseDTO dto = sampleGroupResponse();
    given(groupService.createGroup(any(CreateGroupRequestDTO.class)))
        .willReturn(ResponseEntity.status(HttpStatus.CREATED).body(dto));

    mockMvc
        .perform(
            post("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(dto.getId()))
        .andExpect(jsonPath("$.name").value(dto.getName()))
        .andExpect(jsonPath("$.inviteCode").value(dto.getInviteCode()));

    verify(groupService).createGroup(any(CreateGroupRequestDTO.class));
  }

  @Test
  @DisplayName("Should fail to create group with invalid payload")
  void shouldFailToCreateGroupWithInvalidPayload() throws Exception {
    CreateGroupRequestDTO invalid =
        new CreateGroupRequestDTO(null, "desc", true, 10, Set.of(), Set.of(), 1L);

    mockMvc
        .perform(
            post("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should get group by ID")
  void shouldGetGroupById() throws Exception {
    GroupResponseDTO dto = sampleGroupResponse();
    given(groupService.getGroupById(10L)).willReturn(ResponseEntity.ok(dto));

    mockMvc
        .perform(get("/api/groups/{groupId}", 10L))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(dto.getId()))
        .andExpect(jsonPath("$.name").value(dto.getName()))
        .andExpect(jsonPath("$.inviteCode").value(dto.getInviteCode()));

    verify(groupService).getGroupById(10L);
  }

  @Test
  @DisplayName("Should get groups by user membership")
  void shouldGetGroupsByUserId() throws Exception {
    GroupResponseDTO dto = sampleGroupResponse();
    given(groupService.getGroupsByUserId(2L)).willReturn(ResponseEntity.ok(List.of(dto)));

    mockMvc
        .perform(get("/api/groups/membership/{userId}", 2L))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].id").value(dto.getId()));

    verify(groupService).getGroupsByUserId(2L);
  }

  @Test
  @DisplayName("Should update group")
  void shouldUpdateGroup() throws Exception {
    UpdateGroupRequestDTO updateDto =
        new UpdateGroupRequestDTO("Updated", "desc", false, Set.of(2L), Set.of(2L));
    GroupResponseDTO dto = sampleGroupResponse();
    given(groupService.updateGroup(eq(10L), any(UpdateGroupRequestDTO.class)))
        .willReturn(ResponseEntity.ok(dto));

    mockMvc
        .perform(
            patch("/api/groups/{groupId}", 10L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(dto.getId()));

    verify(groupService).updateGroup(eq(10L), any(UpdateGroupRequestDTO.class));
  }

  @Test
  @DisplayName("Should delete group")
  void shouldDeleteGroup() throws Exception {
    given(groupService.deleteGroup(10L)).willReturn(ResponseEntity.noContent().build());

    mockMvc.perform(delete("/api/groups/{groupId}", 10L)).andExpect(status().isNoContent());

    verify(groupService).deleteGroup(10L);
  }

  @Test
  @DisplayName("Should remove member from group")
  void shouldRemoveMemberFromGroup() throws Exception {
    given(groupService.removeMemberFromGroup(10L, 2L))
        .willReturn(ResponseEntity.noContent().build());

    mockMvc
        .perform(delete("/api/groups/{groupId}/members/{userId}", 10L, 2L))
        .andExpect(status().isNoContent());

    verify(groupService).removeMemberFromGroup(10L, 2L);
  }

  @Test
  @DisplayName("Should remove admin from group")
  void shouldRemoveAdminFromGroup() throws Exception {
    given(groupService.removeAdminFromGroup(10L, 2L))
        .willReturn(ResponseEntity.noContent().build());

    mockMvc
        .perform(delete("/api/groups/{groupId}/admins/{userId}", 10L, 2L))
        .andExpect(status().isNoContent());

    verify(groupService).removeAdminFromGroup(10L, 2L);
  }

  @Test
  @DisplayName("Should join group by invite code")
  void shouldJoinGroupByInviteCode() throws Exception {
    given(groupService.joinGroupByInvitationCode("code", 3L))
        .willReturn(ResponseEntity.noContent().build());

    mockMvc
        .perform(post("/api/groups/join/{inviteCode}/{userId}", "code", 3L))
        .andExpect(status().isNoContent());

    verify(groupService).joinGroupByInvitationCode("code", 3L);
  }

  @Test
  @DisplayName("Should handle invalid path variables")
  void shouldHandleInvalidPathVariables() throws Exception {
    mockMvc.perform(get("/api/groups/invalid")).andExpect(status().isBadRequest());
    mockMvc.perform(get("/api/groups/membership/invalid")).andExpect(status().isBadRequest());
  }

  private GroupResponseDTO sampleGroupResponse() {
    return new GroupResponseDTO(10L, "Test Group", "desc", false, 1L, null, null, 1, 5, "invite");
  }
}
