import { TaskResponseDisplayable, TasksServiceWrapper } from './../../services/wrapper-services/tasks.service.wrapper';
import { CommonModule } from '@angular/common';
import { Component, computed, OnInit, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router } from '@angular/router';
import { TaskPriority, TaskStatus, UserResponse } from '../../api-services/api';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import {
  StatusDialogComponent,
  StatusDialogData,
} from '../../components/status-dialog/status-dialog.component';

@Component({
  selector: 'app-task-detail',
  imports: [CommonModule, MatIconModule, MatButtonModule, MatCardModule],
  templateUrl: './task-detail.html',
  styleUrl: './task-detail.scss',
})
export class TaskDetailComponent implements OnInit{

  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly tasksService: TasksServiceWrapper,
    private readonly dialog: MatDialog
  ) {}

  task = signal<TaskResponseDisplayable | undefined>(undefined);
  members = signal<UserResponse[]>([]);
  userId = signal<number | undefined>(undefined);
  showNotAMember = computed(() => this.members().some(member => member.id === this.userId()) === false);
  readonly TaskPriority = TaskPriority;

  ngOnInit(): void {
    this.task.set(this.route.snapshot.data['task']);
    this.members.set(this.route.snapshot.data['groupMembers']);
    this.userId.set(this.route.snapshot.data['userId']);
  }

  backToGroupsDetail(): void {
    this.router.navigate(['/groups/' + this.route.snapshot.paramMap.get('groupId')]);
  }

  toEditTask(): void {
    this.router.navigate(['/groups/' + this.route.snapshot.paramMap.get('groupId') + '/tasks/' + this.route.snapshot.paramMap.get('taskId') + '/edit']);
  }

  getMemberLabel(userId: number): string {
    const member = this.members().find(user => user.id === userId);
    const name = member ? `${member.firstname} ${member.lastname}` : `User ${userId}`;
    return name;
  }

  getAssigneeLabels(task: TaskResponseDisplayable): string[] {
    if (!task.assignee_ids?.length) {
      return [];
    }
    return task.assignee_ids.map(id => this.getMemberLabel(id));
  }

  getCreatorLabel(task: TaskResponseDisplayable): string {
    return this.getMemberLabel(task.created_by_id);
  }

  tagColor(tag: string): string {
    return this.tasksService.hashCategoryToColor(tag);
  }

  formatStatus(status?: TaskStatus): string {
    return status ? this.formatEnumValue(status) : '-';
  }

  formatPriority(priority?: TaskPriority): string {
    return priority ? this.formatEnumValue(priority) : '-';
  }

  private formatEnumValue(value: string): string {
    const normalized = value.toLowerCase().replace(/_/g, ' ');
    return normalized.charAt(0).toUpperCase() + normalized.slice(1);
  }

  async deleteTask(): Promise<void> {
    const task = this.task();
    if (!task) {
      return;
    }

    try {
      await this.tasksService.deleteTask(task.id);
      this.backToGroupsDetail();
    } catch (error) {
      console.error('Failed to delete task:', error);
      this.openStatusDialog({
        title: 'Delete failed',
        message: 'Unable to delete the task. Please try again.',
        type: 'error',
        autoCloseMs: 5000,
      });
    }
  }

  private openStatusDialog(data: StatusDialogData): void {
    this.dialog.open(StatusDialogComponent, {
      data,
    });
  }
}
