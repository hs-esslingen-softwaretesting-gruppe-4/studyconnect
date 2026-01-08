import { ActivatedRouteSnapshot, Router } from "@angular/router";
import { TaskResponseDisplayable, TasksServiceWrapper } from "../services/wrapper-services/tasks.service.wrapper";
import { Injectable } from "@angular/core";

@Injectable({
  providedIn: 'root',
})
/**
 * Resolver to load task data before loading a route.
 */
export class TaskResolver {

  constructor(private readonly tasksService: TasksServiceWrapper, private readonly router: Router) { }

  async resolve(route: ActivatedRouteSnapshot): Promise<TaskResponseDisplayable | undefined> {
    const groupId = route.paramMap.get('groupId');
    if (!groupId || Number.isNaN(Number(groupId))) {
      this.router.navigate(['not-found'], { skipLocationChange: false });
      return undefined;
    }
    const taskId = route.paramMap.get('taskId');
    if (!taskId || Number.isNaN(Number(taskId))) {
      this.router.navigate(['not-found'], { skipLocationChange: false });
      return undefined;
    }

    try {
      const task = await this.tasksService.getTaskById(Number(groupId), Number(taskId));
      return task;
    } catch (error) {
      console.error('Failed to load task:', error);
      this.router.navigate(['not-found'], { skipLocationChange: false });
      return undefined;
    }
  }
}
