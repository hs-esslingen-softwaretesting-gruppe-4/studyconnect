import { TaskResponseDisplayable, TasksServiceWrapper } from './../../services/wrapper-services/tasks.service.wrapper';
import { Component, computed, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { ActivatedRoute, Router } from '@angular/router';
import { TaskPriority, TaskRequest, TaskStatus } from '../../api-services/api';
import { MatTabsModule } from '@angular/material/tabs';
import { GroupNameAndId } from '../../services/wrapper-services/groups.service.wrapper';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, MatCardModule, MatTabsModule, MatFormFieldModule, MatSelectModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit{
  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly tasksService: TasksServiceWrapper
  ) {}

  userTasks = signal<TaskResponseDisplayable[] | undefined>(undefined);
  userGroups = signal<GroupNameAndId[] | undefined>(undefined);
  readonly TaskPriority = TaskPriority;
  readonly taskStatusTabs = [
    { status: TaskStatus.Open, label: 'Open' },
    { status: TaskStatus.InProgress, label: 'In progress' },
    { status: TaskStatus.Completed, label: 'Completed' },
    { status: TaskStatus.Cancelled, label: 'Cancelled' },
  ];
  readonly hasTasks = computed(() => (this.userTasks() ?? []).length > 0);
  readonly groupNameById = computed(() => {
    const groups = this.userGroups() ?? [];
    return new Map(groups.map(group => [group.id, group.name]));
  });
  private readonly updatingTaskIds = signal<Set<number>>(new Set<number>());

  ngOnInit(): void {
    this.userTasks.set(this.route.snapshot.data['userTasks']);
    this.userGroups.set(this.route.snapshot.data['userGroups']);
  }

  navigateToGroup(groupId: number): void {
    this.router.navigate(['/groups', groupId]);
  }

  navigateToTask(groupId: number, taskId: number): void {
    this.router.navigate(['/groups', groupId, 'tasks', taskId]);
  }

  groupName(groupId: number): string {
    return this.groupNameById().get(groupId) ?? `Group ${groupId}`;
  }

  isUpdatingTask(taskId: number): boolean {
    return this.updatingTaskIds().has(taskId);
  }

  async onStatusChange(task: TaskResponseDisplayable, newStatus: TaskStatus): Promise<void> {
    if (task.status === newStatus || this.isUpdatingTask(task.id)) {
      return;
    }

    const previousStatus = task.status;
    this.setTaskUpdating(task.id, true);
    this.updateTaskInList(task.id, { status: newStatus });

    const payload: TaskRequest = {
      title: task.title,
      description: task.description,
      due_date: this.normalizeDueDate(task.due_date),
      priority: task.priority,
      status: newStatus,
      category: task.category,
      tags: task.tags ?? [],
      created_by_id: task.created_by_id,
      assignee_ids: task.assignee_ids ?? [],
    };

    try {
      const updated = await this.tasksService.updateTask(task.id, payload);
      this.updateTaskInList(task.id, updated);
    } catch (error) {
      console.error('Failed to update task status:', error);
      this.updateTaskInList(task.id, { status: previousStatus });
    } finally {
      this.setTaskUpdating(task.id, false);
    }
  }

  getTasksForStatus(status: TaskStatus): TaskResponseDisplayable[] {
    const tasks = (this.userTasks() ?? []).filter(task => task.status === status);
    if (status === TaskStatus.Open || status === TaskStatus.InProgress) {
      return tasks.sort((a, b) => this.compareDueDate(a, b));
    }
    return tasks.sort((a, b) => this.compareUpdatedAtDesc(a, b));
  }

  isOverdue(task: TaskResponseDisplayable): boolean {
    if (task.status !== TaskStatus.Open && task.status !== TaskStatus.InProgress) {
      return false;
    }
    const dueTime = this.toTimestamp(task.due_date);
    if (dueTime === null) {
      return false;
    }
    return dueTime < Date.now();
  }

  tagColor(tag: string): string {
    return this.tasksService.hashCategoryToColor(tag);
  }

  priorityLabel(priority: TaskPriority): string {
    switch (priority) {
      case TaskPriority.High:
        return 'High';
      case TaskPriority.Medium:
        return 'Medium';
      case TaskPriority.Low:
        return 'Low';
      default:
        return String(priority);
    }
  }

  private compareDueDate(a: TaskResponseDisplayable, b: TaskResponseDisplayable): number {
    const aTime = this.toTimestamp(a.due_date);
    const bTime = this.toTimestamp(b.due_date);

    if (aTime === null && bTime === null) {
      return 0;
    }
    if (aTime === null) {
      return 1;
    }
    if (bTime === null) {
      return -1;
    }
    if (aTime !== bTime) {
      return aTime - bTime;
    }
    return this.compareUpdatedAtDesc(a, b);
  }

  private compareUpdatedAtDesc(a: TaskResponseDisplayable, b: TaskResponseDisplayable): number {
    const aTime = this.toTimestamp(a.updated_at);
    const bTime = this.toTimestamp(b.updated_at);
    if (aTime === null && bTime === null) {
      return 0;
    }
    if (aTime === null) {
      return 1;
    }
    if (bTime === null) {
      return -1;
    }
    return bTime - aTime;
  }

  private toTimestamp(value?: string | null): number | null {
    if (!value) {
      return null;
    }
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      const match = value.match(/(\d{2})\.(\d{2})\.(\d{4})(?:,?\s*(\d{2}):(\d{2}))?/);
      if (!match) {
        return null;
      }
      const [, day, month, year, hour = '00', minute = '00'] = match;
      const localeParsed = new Date(
        Number(year),
        Number(month) - 1,
        Number(day),
        Number(hour),
        Number(minute)
      );
      if (Number.isNaN(localeParsed.getTime())) {
        return null;
      }
      return localeParsed.getTime();
    }
    return parsed.getTime();
  }

  private setTaskUpdating(taskId: number, isUpdating: boolean): void {
    this.updatingTaskIds.update(current => {
      const next = new Set(current);
      if (isUpdating) {
        next.add(taskId);
      } else {
        next.delete(taskId);
      }
      return next;
    });
  }

  private updateTaskInList(
    taskId: number,
    update: Partial<TaskResponseDisplayable> | TaskResponseDisplayable
  ): void {
    this.userTasks.update(tasks =>
      (tasks ?? []).map(task => (task.id === taskId ? { ...task, ...update } : task))
    );
  }

  private normalizeDueDate(value?: string | Date | null): string | undefined {
    if (!value) {
      return undefined;
    }

    if (value instanceof Date) {
      return this.formatLocalDateTime(value);
    }

    const trimmed = value.trim();
    if (!trimmed) {
      return undefined;
    }

    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(trimmed)) {
      return trimmed.length === 16 ? `${trimmed}:00` : trimmed;
    }

    const localeMatch = trimmed.match(/^(\d{2})\.(\d{2})\.(\d{4}),\s*(\d{2}):(\d{2})$/);
    if (localeMatch) {
      const [, day, month, year, hour, minute] = localeMatch;
      return `${year}-${month}-${day}T${hour}:${minute}:00`;
    }

    const parsed = new Date(trimmed);
    if (!Number.isNaN(parsed.getTime())) {
      return this.formatLocalDateTime(parsed);
    }

    return trimmed;
  }

  private formatLocalDateTime(date: Date): string {
    const year = date.getFullYear();
    const month = this.pad(date.getMonth() + 1);
    const day = this.pad(date.getDate());
    const hours = this.pad(date.getHours());
    const minutes = this.pad(date.getMinutes());
    return `${year}-${month}-${day}T${hours}:${minutes}:00`;
  }

  private pad(value: number): string {
    return value.toString().padStart(2, '0');
  }
}
