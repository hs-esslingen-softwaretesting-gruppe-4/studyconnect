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

  private formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleString('de-DE', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
  }

  private toDisplayable(task: TaskResponse): TaskResponseDisplayable {
    const formattedTask = { ...task };
    formattedTask.created_at = this.formatDate(formattedTask.created_at);
    formattedTask.updated_at = this.formatDate(formattedTask.updated_at);
    if (formattedTask.last_status_change_at) {
      formattedTask.last_status_change_at = this.formatDate(formattedTask.last_status_change_at);
    }
    if (formattedTask.due_date) {
      formattedTask.due_date = this.formatDate(formattedTask.due_date);
    }
    return {
      ...formattedTask,
      tagsHashColor: this.hashCategoryToColor(formattedTask.tags?.[0]),
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

  public async getTaskById(groupId: number, taskId: number) : Promise<TaskResponseDisplayable> {
    const tasks = await lastValueFrom(this.tasksService.apiTasksGroupsGroupIdGet(groupId));
    const task = tasks.find(task => task.id === taskId) as TaskResponseDisplayable;
    return this.toDisplayable(task);
  }

  private mapDisplayableTaskToRequest(task: TaskResponseDisplayable): TaskRequest {
    return {
      title: task.title,
      description: task.description,
      due_date: task.due_date ?? undefined,
      priority: task.priority,
      status: task.status,
      category: task.category,
      tags: task.tags,
      assignee_ids: task.assignee_ids,
      created_by_id: task.created_by_id,
    };
  }
}
