import { ActivatedRouteSnapshot, Router } from "@angular/router";
import { GroupsServiceWrapper } from "../services/wrapper-services/groups.service.wrapper";
import { UserResponse } from "../api-services/api";
import { Injectable } from "@angular/core";

@Injectable({
  providedIn: 'root',
})
/**
 * Resolver to load group members before loading a route.
 */
export class GroupMemberResolver {
  constructor(private readonly groupsService: GroupsServiceWrapper, private readonly router: Router) {}

  async resolve(route: ActivatedRouteSnapshot): Promise<UserResponse[] | undefined> {
    const groupIdParam = route.paramMap.get('groupId');
    if (!groupIdParam || Number.isNaN(Number(groupIdParam))) {
      this.router.navigate(['not-found'], { skipLocationChange: false });
      return undefined;
    }
    const groupId = Number(groupIdParam);

    try {
      const members = await this.groupsService.getGroupMembers(groupId);
      return members;
    } catch (error) {
      console.error('Failed to load group members:', error);
      this.router.navigate(['not-found'], { skipLocationChange: false });
      return undefined;
    }
  }
}
