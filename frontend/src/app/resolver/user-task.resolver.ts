import { Injectable } from "@angular/core";
import { TaskResponseDisplayable, TasksServiceWrapper } from "../services/wrapper-services/tasks.service.wrapper";
import { UsersServiceWrapper } from "../services/wrapper-services/users.service.wrapper";

/**
 * Resolver to load user tasks before loading a route.
 */
@Injectable({
  providedIn: 'root'
})
export class UserTaskResolver {

  constructor(private readonly userService: UsersServiceWrapper, private readonly taskService: TasksServiceWrapper) {}

  async resolve(): Promise<TaskResponseDisplayable[] | undefined> {
    const userId = await this.userService.getUserIDByToken();
    if (!userId) {
      return undefined;
    }
    try {
      const tasks = await this.taskService.getTaskForUser(userId);
      return tasks;
    }
    catch (error) {
      console.error('Failed to load user tasks:', error);
      return undefined;
    }
  }
}
