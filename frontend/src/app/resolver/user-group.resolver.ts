import { Injectable } from "@angular/core";
import { GroupNameAndId, GroupsServiceWrapper } from "../services/wrapper-services/groups.service.wrapper";
import { UsersServiceWrapper } from "../services/wrapper-services/users.service.wrapper";
import { Resolve } from "@angular/router";

/**
 * Resolver to load user groups before loading a route.
 */
@Injectable({
  providedIn: 'root'
})
export class UserGroupResolver implements Resolve<GroupNameAndId[] | undefined> {

  constructor(private readonly groupsService: GroupsServiceWrapper, private readonly userService: UsersServiceWrapper) {}

  async resolve(): Promise<GroupNameAndId[] | undefined> {
    const userId = await this.userService.getUserIDByToken();
    if (!userId) {
      return undefined;
    }

    try {
      return await this.groupsService.getGroupNamesAndIdsForCurrentUser(userId);
    }
    catch (error){
      console.error(error);
      return undefined;
    }
  }
}
