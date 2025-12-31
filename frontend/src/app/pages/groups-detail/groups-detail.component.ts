import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { GroupResponse, TaskPriority, TaskStatus, UserResponse } from '../../api-services/api';
import { GroupsServiceWrapper } from '../../services/wrapper-services/groups.service.wrapper';
import { TaskResponseDisplayable } from '../../services/wrapper-services/tasks.service.wrapper';
import { CreateTaskDialogComponent } from '../../components/create-task-dialog/create-task-dialog.component';
import {
  StatusDialogComponent,
  StatusDialogData,
} from '../../components/status-dialog/status-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIcon } from "@angular/material/icon";
import {MatTimepickerModule} from '@angular/material/timepicker';

@Component({
  selector: 'app-groups-detail.component',
  imports: [CommonModule, MatButtonModule, MatCardModule, MatTabsModule, MatIcon, MatTimepickerModule],
  templateUrl: './groups-detail.component.html',
  styleUrl: './groups-detail.component.scss',
})
export class GroupsDetailComponent implements OnInit {
  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly groupService: GroupsServiceWrapper,
    private readonly dialog: MatDialog
  ) {}

  userId = signal<number | undefined>(undefined);
  isMember = signal<boolean>(false);
  isAdmin = signal<boolean>(false);
  groupMembers = signal<UserResponse[]>([]);
  groupAdmins = signal<UserResponse[]>([]);
  group = signal<GroupResponse | undefined>(undefined);
  groupTasks = signal<TaskResponseDisplayable[]>([]);
  groupIsPublic = signal<boolean>(false);
  visibleAdmins = computed(() => this.groupAdmins().slice(0, 5));
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
      width: '720px',
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
}
