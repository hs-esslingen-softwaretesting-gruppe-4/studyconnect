import { lastValueFrom } from "rxjs";
import { CreateGroupRequest, GroupResponse, GroupsService, UserResponse } from "../../api-services/api";
import { Injectable } from "@angular/core";

export interface GroupNameAndId {
  id: number;
  name: string;
}

@Injectable({
  providedIn: 'root',
})
export class GroupsServiceWrapper {
  constructor(private readonly groupsService: GroupsService) {}

  async createGroup(groupData: CreateGroupRequest): Promise<GroupResponse> {
    return await lastValueFrom(this.groupsService.apiGroupsPost(groupData));
  }
  async searchPublicGroups(query: string): Promise<GroupResponse[]> {
    return await lastValueFrom(this.groupsService.apiGroupsSearchGet(query));
  }

  async getPublicGroups(): Promise<GroupResponse[]> {
    return await lastValueFrom(this.groupsService.apiGroupsGet());
  }

  async joinGroupByInviteCode(inviteCode: string, userId: number): Promise<void> {
    await lastValueFrom(this.groupsService.apiGroupsJoinInviteCodeUserIdPost(inviteCode, userId));
  }

  async getJoinedGroupsForCurrentUser(userId: number): Promise<GroupResponse[]> {
    return await lastValueFrom(this.groupsService.apiGroupsMembershipUserIdGet(userId));
  }

  async getGroupById(groupId: number): Promise<GroupResponse> {
    return await lastValueFrom(this.groupsService.apiGroupsGroupIdGet(groupId));
  }

  async getGroupMembers(groupId: number): Promise<UserResponse[]> {
    return await lastValueFrom(this.groupsService.apiGroupsGroupIdMembersGet(groupId));
  }

  async getGroupAdmins(groupId: number): Promise<UserResponse[]> {
    return await lastValueFrom(this.groupsService.apiGroupsGroupIdAdminsGet(groupId));
  }

  async leaveGroup(groupId: number, userId: number): Promise<void> {
    await lastValueFrom(this.groupsService.apiGroupsGroupIdMembersUserIdDelete(groupId, userId));
  }

  async removeGroupAdmin(groupId: number, userId: number): Promise<void> {
    await lastValueFrom(this.groupsService.apiGroupsGroupIdAdminsUserIdDelete(groupId, userId));
  }

  async deleteGroup(groupId: number): Promise<void> {
    await lastValueFrom(this.groupsService.apiGroupsGroupIdDelete(groupId));
  }

  async joinGroup(groupId: number, userId: number): Promise<void> {
    await lastValueFrom(this.groupsService.apiGroupsGroupIdJoinPost(groupId, userId));
  }

  async getGroupNamesAndIdsForCurrentUser(userId: number): Promise<GroupNameAndId[]> {
    const groups = await this.getJoinedGroupsForCurrentUser(userId);
    return groups.map(group => ({ id: group.id, name: group.name }));
  }

  async updateGroupDetails(groupId: number, updatedData: Partial<CreateGroupRequest>): Promise<GroupResponse> {
    return await lastValueFrom(this.groupsService.apiGroupsGroupIdPatch(groupId, updatedData));
  }
}
