import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { GroupResponse, TaskPriority, TaskRequest, TaskStatus, UserResponse } from '../../api-services/api';
import { GroupsServiceWrapper } from '../../services/wrapper-services/groups.service.wrapper';
import { TaskResponseDisplayable, TasksServiceWrapper } from '../../services/wrapper-services/tasks.service.wrapper';
import { CreateTaskDialogComponent } from '../../components/create-task-dialog/create-task-dialog.component';
import {
  StatusDialogComponent,
  StatusDialogData,
} from '../../components/status-dialog/status-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatMenuModule } from '@angular/material/menu';
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from '../../components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-groups-detail.component',
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatTabsModule,
    MatFormFieldModule,
    MatIcon,
    MatSelectModule,
    MatMenuModule,
  ],
  templateUrl: './groups-detail.component.html',
  styleUrl: './groups-detail.component.scss',
})
export class GroupsDetailComponent implements OnInit {
  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly groupService: GroupsServiceWrapper,
    private readonly dialog: MatDialog,
    private readonly tasksService: TasksServiceWrapper
  ) {}

  userId = signal<number | undefined>(undefined);
  isMember = signal<boolean>(false);
  isAdmin = signal<boolean>(false);
  groupMembers = signal<UserResponse[]>([]);
  groupAdmins = signal<UserResponse[]>([]);
  group = signal<GroupResponse | undefined>(undefined);
  groupTasks = signal<TaskResponseDisplayable[]>([]);
  groupIsPublic = signal<boolean>(false);
  visibleAdmins = computed(() => this.groupAdmins().slice(0, 5)); // Show only first 5 admins in the side panel
  showPublicJoin = computed(() => !this.isMember() && this.groupIsPublic());
  showPrivateBlocked = computed(() => !this.isMember() && !this.groupIsPublic());
  showGroupDetail = computed(() => this.isMember());
  readonly taskPageSize = 5;
  readonly taskStatusTabs = [
    { status: TaskStatus.Open, label: 'Open' },
    { status: TaskStatus.InProgress, label: 'In Progress' },
    { status: TaskStatus.Completed, label: 'Completed' },
    { status: TaskStatus.Cancelled, label: 'Cancelled' },
  ];
  readonly TaskPriority = TaskPriority;
  private readonly taskVisibleCounts = signal<Record<TaskStatus, number>>({
    [TaskStatus.Open]: this.taskPageSize,
    [TaskStatus.InProgress]: this.taskPageSize,
    [TaskStatus.Completed]: this.taskPageSize,
    [TaskStatus.Cancelled]: this.taskPageSize,
  });
  private readonly tagFallbackColor = '#9aa0a6';
  private readonly updatingTaskIds = signal<Set<number>>(new Set<number>());

  ngOnInit(): void {
    this.userId.set(this.route.snapshot.data['userId']);
    const resolvedData = this.route.snapshot.data['groupResolvedData'];
    this.groupMembers.set(resolvedData?.members ?? []);
    this.group.set(resolvedData?.group);
    this.groupTasks.set(resolvedData?.tasks ?? []);
    this.isMember.set(this.groupMembers().some((member) => member.id === this.userId()));
    this.groupIsPublic.set(this.group()?.is_public ?? false);
    this.groupAdmins.set(resolvedData?.admins ?? []);
    this.isAdmin.set(this.groupAdmins().some((admin) => admin.id === this.userId()));
  }

  readonly backToGroups = () => {
    this.router.navigate(['/groups']);
  };

  public async joinGroup(): Promise<void> {
    const groupId = this.group()?.id;
    const userId = this.userId();
    try {
      await this.groupService.joinGroup(groupId!, userId!);
      this.groupMembers.set(await this.groupService.getGroupMembers(groupId!));
      this.openStatusDialog({
        title: 'Join successful',
        message: 'You have successfully joined the group.',
        type: 'success',
        autoCloseMs: 5000,
      });
      this.isMember.set(true);
    } catch (error) {
      console.error('Error joining group:', error);
      this.openStatusDialog({
        title: 'Join failed',
        message: 'An error occurred while trying to join the group. Please try again later.',
        type: 'error',
        autoCloseMs: 5000,
      });
    }
  }

  private openStatusDialog(data: StatusDialogData): void {
    this.dialog.open(StatusDialogComponent, {
      data,
      autoFocus: false,
    });
  }

  public async leaveGroup(): Promise<void> {
    const groupId = this.group()?.id;
    const userId = this.userId();
    try {
      await this.groupService.leaveGroup(groupId!, userId!);
      this.isMember.set(false);
      this.groupMembers.set(
        this.groupMembers().filter((member) => member.id !== userId)
      );
      this.openStatusDialog({
        title: 'Leave successful',
        message: 'You have successfully left the group.',
        type: 'success',
        autoCloseMs: 5000,
      });
    } catch (error) {
      console.error('Error leaving group:', error);
      this.openStatusDialog({
        title: 'Leave failed',
        message: 'An error occurred while trying to leave the group. Please try again later.',
        type: 'error',
        autoCloseMs: 5000,
      });
    }
  }

  async copyInviteCode(): Promise<void> {
    const inviteCode = this.group()?.invite_code;
    if (!inviteCode) {
      return;
    }

    try {
      await this.writeToClipboard(inviteCode);
    } catch (error) {
      console.error('Failed to copy invite code:', error);
      this.openStatusDialog({
        title: 'Copy failed',
        message: 'Unable to copy the invite code. Please copy it manually.',
        type: 'error',
        autoCloseMs: 4000,
      });
    }
  }

  confirmLeaveGroup(): void {
    const group = this.group();
    if (!group) {
      return;
    }

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Leave group?',
        message: `You are about to leave "${group.name}". You will lose access to its tasks.`,
        confirmText: 'Leave group',
        cancelText: 'Stay',
      } satisfies ConfirmDialogData,
      autoFocus: false,
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.leaveGroup();
      }
    });
  }

  openCreateTaskDialog(): void {
    const group = this.group();
    const userId = this.userId();
    if (!group || userId === undefined) {
      return;
    }

    const dialogRef = this.dialog.open(CreateTaskDialogComponent, {
      data: {
        groupId: group.id,
        members: this.groupMembers(),
        userId,
      },
      autoFocus: false,
      width: '850px',
    });

    dialogRef.afterClosed().subscribe((createdTask?: TaskResponseDisplayable) => {
      if (!createdTask) {
        return;
      }
      this.groupTasks.update(tasks => [createdTask, ...tasks]);
      this.openStatusDialog({
        title: 'Task created',
        message: `The task "${createdTask.title}" has been created.`,
        type: 'success',
        autoCloseMs: 5000,
      });
    });
  }

  openTask(taskId: number): void {
    const group = this.group();
    if (!group) {
      return;
    }
    this.router.navigate(['/groups', group.id, 'tasks', taskId]);
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
      this.openStatusDialog({
        title: 'Status update failed',
        message: 'Unable to update the task status. Please try again.',
        type: 'error',
        autoCloseMs: 5000,
      });
    } finally {
      this.setTaskUpdating(task.id, false);
    }
  }

  isUpdatingTask(taskId: number): boolean {
    return this.updatingTaskIds().has(taskId);
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
    this.groupTasks.update(tasks =>
      tasks.map(task => (task.id === taskId ? { ...task, ...update } : task))
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

  private async writeToClipboard(value: string): Promise<void> {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(value);
      return;
    }

    const textarea = document.createElement('textarea');
    textarea.value = value;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    const success = document.execCommand('copy');
    document.body.removeChild(textarea);
    if (!success) {
      throw new Error('Copy command failed');
    }
  }

  getTasksForStatus(status: TaskStatus): TaskResponseDisplayable[] {
    return this.groupTasks().filter(task => task.status === status);
  }

  getVisibleTasksForStatus(status: TaskStatus): TaskResponseDisplayable[] {
    const visibleCount = this.taskVisibleCounts()[status];
    return this.getTasksForStatus(status).slice(0, visibleCount);
  }

  hasMoreTasks(status: TaskStatus): boolean {
    const visibleCount = this.taskVisibleCounts()[status];
    return this.getTasksForStatus(status).length > visibleCount;
  }

  showMoreTasks(status: TaskStatus): void {
    this.taskVisibleCounts.update(counts => ({
      ...counts,
      [status]: counts[status] + this.taskPageSize,
    }));
  }

  getAssigneeNames(task: TaskResponseDisplayable): string {
    if (!task.assignee_ids?.length) {
      return 'Unassigned';
    }

    const members = this.groupMembers();
    const names = task.assignee_ids.map(id => {
      const member = members.find(user => user.id === id);
      return member ? `${member.firstname} ${member.lastname}` : `User ${id}`;
    });

    return names.length ? names.join(', ') : 'Unassigned';
  }

  getTagColor(task: TaskResponseDisplayable, tag: string): string {
    if (!tag?.trim()) {
      return this.tagFallbackColor;
    }
    if (task.tags?.[0] === tag) {
      return task.tagsHashColor || this.tagFallbackColor;
    }
    return this.hashTagToColor(tag);
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

  private hashTagToColor(tag: string): string {
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

  async deleteGroup(): Promise<void> {
    if (!this.isAdmin()) {
      return;
    }
    const groupId = this.group()?.id;
    if (!groupId) {
      return;
    }

    try {
      await this.groupService.deleteGroup(groupId);
      this.openStatusDialog({
        title: 'Group deleted',
        message: 'The group has been successfully deleted.',
        type: 'success',
        autoCloseMs: 5000,
      });
      this.backToGroups();
    } catch (error) {
      console.error('Error deleting group:', error);
      this.openStatusDialog({
        title: 'Delete failed',
        message: 'An error occurred while trying to delete the group. Please try again later.',
        type: 'error',
        autoCloseMs: 5000,
      });
    }
  }

  confirmDeleteGroup(): void {
    const group = this.group();
    if (!group || !this.isAdmin()) {
      return;
    }

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete group?',
        message: `Deleting "${group.name}" will remove the group and its tasks for everyone.`,
        confirmText: 'Delete group',
        cancelText: 'Cancel',
      } satisfies ConfirmDialogData,
      autoFocus: false,
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.deleteGroup();
      }
    });
  }

  goToEditGroup(): void {
    const groupId = this.group()?.id;
    if (!groupId || !this.isAdmin()) {
      return;
    }
    this.router.navigate(['/groups', groupId, 'edit']);
  }
}
