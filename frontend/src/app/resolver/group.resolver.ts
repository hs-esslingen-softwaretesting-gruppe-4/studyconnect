import { ActivatedRouteSnapshot, Resolve, Router } from "@angular/router";
import { GroupResponse, UserResponse } from "../api-services/api";
import { GroupsServiceWrapper } from "../services/wrapper-services/groups.service.wrapper";
import { TaskResponseDisplayable, TasksServiceWrapper } from "../services/wrapper-services/tasks.service.wrapper";
import { Injectable } from "@angular/core";

export interface GroupResolvedData {
  group: GroupResponse | undefined;
  tasks: TaskResponseDisplayable[] | undefined;
  members: UserResponse[] | undefined;
  admins: UserResponse[] | undefined;
}

@Injectable({
  providedIn: 'root',
})
/**
 * Resolver to load group data, its members, and tasks before loading a route.
 */
export class GroupResolver implements Resolve<GroupResolvedData | undefined> {
  constructor(private readonly groupsService: GroupsServiceWrapper, private readonly tasksService: TasksServiceWrapper, private readonly router: Router) {}

  async resolve(route: ActivatedRouteSnapshot): Promise<GroupResolvedData | undefined> {
    const groupIdParam = route.paramMap.get('groupId');
    if (!groupIdParam || Number.isNaN(Number(groupIdParam))) {
      this.router.navigate(['not-found'], { skipLocationChange: false });
      return undefined;
    }
    const groupId = Number(groupIdParam);

    try
    {
      const [group, members, tasks, admins] = await Promise.all([
        this.groupsService.getGroupById(groupId),
        this.groupsService.getGroupMembers(groupId),
        this.tasksService.getTasksForGroupAsDisplayable(groupId),
        this.groupsService.getGroupAdmins(groupId),
      ]);
      return { group, members, tasks, admins };
    }
    catch (error) {
      console.error('Failed to load group:', error);
      this.router.navigate(['not-found'], { skipLocationChange: false });
      return undefined;
    }
  }
}
