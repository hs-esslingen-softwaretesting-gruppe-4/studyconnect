import { lastValueFrom } from "rxjs";
import { TaskRequest, TaskResponse, TasksService } from "../../api-services/api";
import { Injectable } from "@angular/core";

export interface TaskResponseDisplayable extends TaskResponse {
  tagsHashColor: string;
}

@Injectable({
  providedIn: 'root',
})
export class TasksServiceWrapper {
  constructor(private readonly tasksService: TasksService) {}

  private static readonly categoryFallbackColor = "#9aa0a6";

  private toDisplayable(task: TaskResponse): TaskResponseDisplayable {
    return {
      ...task,
      tagsHashColor: this.hashCategoryToColor(task.tags?.[0]),
    };
  }

  public hashCategoryToColor(tag?: string): string {
    if (!tag?.trim()) {
      return TasksServiceWrapper.categoryFallbackColor;
    }

    const normalized = tag.trim().toLowerCase();
    let hash = 0;
    for (let index = 0; index < normalized.length; index += 1) {
      hash = (hash << 5) - hash + normalized.charCodeAt(index);
      hash |= 0;
    }

    const hue = (hash >>> 0) % 360;
    const saturation = 60;
    const lightness = 55;
    return `hsl(${hue} ${saturation}% ${lightness}%)`;
  }

  /**
   * Retrieves all tasks for a specific group.
   * @param groupId The ID of the group.
   * @returns A promise that resolves to an array of TaskResponse objects.
   */
  public async getTasksForGroup(groupId: number) : Promise<TaskResponse[]> {
    return await lastValueFrom(this.tasksService.apiTasksGroupsGroupIdGet(groupId));
  }

  public async getTasksForGroupAsDisplayable(groupId: number) : Promise<TaskResponseDisplayable[]> {
    const tasks = await lastValueFrom(this.tasksService.apiTasksGroupsGroupIdGet(groupId));
    return tasks.map((task) => this.toDisplayable(task));
  }

  /**
   * Creates a new task for a specific group.
   * @param groupId The ID of the group.
   * @param taskRequest The task request payload.
   * @returns A promise that resolves to the created TaskResponseDisplayable object.
   */
  public async createTaskForGroup(groupId:number, taskRequest : TaskRequest) : Promise<TaskResponseDisplayable> {
    const task = await lastValueFrom(this.tasksService.apiTasksGroupsGroupIdPost(groupId, taskRequest));
    return this.toDisplayable(task);
  }

  /**
   * Updates an existing task.
   * @param taskId The ID of the task to update.
   * @param taskRequest The updated task request payload.
   * @returns A promise that resolves to the updated TaskResponseDisplayable object.
   */
  public async updateTask(taskId: number, taskRequest: TaskRequest) : Promise<TaskResponseDisplayable> {
    const task = await lastValueFrom(this.tasksService.apiTasksTaskIdPut(taskId, taskRequest));
    return this.toDisplayable(task);
  }

  /**
   * Deletes a task by its ID.
   * @param taskId The ID of the task to delete.
   */
  public async deleteTask(taskId: number) : Promise<void> {
    await lastValueFrom(this.tasksService.apiTasksTaskIdDelete(taskId));
  }

  /**
   * Retrieves all tasks for a specific user.
   * @param userId The ID of the user.
   * @returns A promise that resolves to an array of TaskResponseDisplayable objects.
   */
  public async getTaskForUser(userId: number) : Promise<TaskResponseDisplayable[]> {
    const tasks = await lastValueFrom(this.tasksService.apiTasksUsersUserIdGet(userId));
    return tasks.map((task) => this.toDisplayable(task));
  }
}
