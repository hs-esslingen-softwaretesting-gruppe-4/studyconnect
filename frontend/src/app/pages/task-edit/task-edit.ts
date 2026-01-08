import {
  TaskResponseDisplayable,
  TasksServiceWrapper,
} from './../../services/wrapper-services/tasks.service.wrapper';
import { CommonModule } from '@angular/common';
import { Component, computed, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TaskPriority, TaskRequest, TaskStatus, UserResponse } from '../../api-services/api';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import {
  MatAutocompleteModule,
  MatAutocompleteSelectedEvent,
} from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatTimepickerModule } from '@angular/material/timepicker';
import { provideNativeDateAdapter } from '@angular/material/core';
import {
  StatusDialogComponent,
  StatusDialogData,
} from '../../components/status-dialog/status-dialog.component';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'app-task-edit',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatAutocompleteModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatTimepickerModule,
  ],
  templateUrl: './task-edit.html',
  styleUrl: './task-edit.scss',
  providers: [provideNativeDateAdapter()],
})
export class TaskEditComponent implements OnInit {
  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly tasksService: TasksServiceWrapper,
    private readonly fb: FormBuilder,
    private readonly dialog: MatDialog
  ) {
    this.taskForm = this.fb.group({
      title: ['', Validators.required],
      description: [''],
      category: [''],
      priority: [TaskPriority.Medium, Validators.required],
      status: [TaskStatus.Open, Validators.required],
      dueDate: [null as Date | null],
      dueTime: [null as Date | null],
    });
  }

  task = signal<TaskResponseDisplayable | undefined>(undefined);
  userId = signal<number | undefined>(undefined);
  members = signal<UserResponse[]>([]);
  showNotAMember = computed(
    () => this.members().some((member) => member.id === this.userId()) === false
  );
  assigneeIds = signal<number[]>([]);
  tags = signal<string[]>([]);
  isSubmitting = signal(false);
  errorMessage = signal('');

  readonly statusOptions = [
    { value: TaskStatus.Open, label: 'Open' },
    { value: TaskStatus.InProgress, label: 'In progress' },
    { value: TaskStatus.Completed, label: 'Completed' },
    { value: TaskStatus.Cancelled, label: 'Cancelled' },
  ];

  readonly priorityOptions = [
    { value: TaskPriority.Low, label: 'Low' },
    { value: TaskPriority.Medium, label: 'Medium' },
    { value: TaskPriority.High, label: 'High' },
  ];

  taskForm: FormGroup;

  readonly assigneeSearchControl = new FormControl<UserResponse | string>('');
  readonly tagInputControl = new FormControl<string>('');

  private readonly assigneeSearchValue = toSignal(this.assigneeSearchControl.valueChanges, {
    initialValue: '',
  });

  readonly filteredAssignees = computed(() => {
    const query = this.normalizeQuery(this.assigneeSearchValue());
    const excluded = new Set(this.assigneeIds());
    return this.members().filter((member) => {
      if (excluded.has(member.id)) {
        return false;
      }
      const label = `${member.firstname} ${member.lastname} ${member.email}`.toLowerCase();
      return label.includes(query);
    });
  });

  readonly assigneeCards = computed(() =>
    this.assigneeIds().map((id) => {
      const member = this.members().find((user) => user.id === id);
      return {
        id,
        name: member ? `${member.firstname} ${member.lastname}` : `User ${id}`,
      };
    })
  );

  ngOnInit(): void {
    this.task.set(this.route.snapshot.data['task']);
    this.userId.set(this.route.snapshot.data['userId']);
    this.members.set(this.route.snapshot.data['groupMembers']);
    this.initializeForm();
  }

  public backToTaskDetail(): void {
    this.router.navigate([
      '/groups/' +
        this.route.snapshot.paramMap.get('groupId') +
        '/tasks/' +
        this.route.snapshot.paramMap.get('taskId'),
    ]);
  }

  displayMember = (value: UserResponse | string | null): string => {
    if (!value) {
      return '';
    }
    if (typeof value === 'string') {
      return value;
    }
    return `${value.firstname} ${value.lastname}`;
  };

  onAssigneeSelected(event: MatAutocompleteSelectedEvent): void {
    const selected = event.option.value as UserResponse | null;
    if (selected) {
      this.addAssignee(selected.id);
    }
    this.assigneeSearchControl.setValue('');
  }

  addAssignee(userId: number): void {
    this.assigneeIds.update((current) =>
      current.includes(userId) ? current : [...current, userId]
    );
  }

  removeAssignee(userId: number): void {
    this.assigneeIds.update((current) => current.filter((id) => id !== userId));
  }

  addTag(): void {
    const rawValue = this.tagInputControl.value ?? '';
    const trimmed = rawValue.trim();
    if (!trimmed) {
      return;
    }
    const normalized = trimmed.toLowerCase();
    const existing = new Set(this.tags().map((tag) => tag.toLowerCase()));
    if (!existing.has(normalized)) {
      this.tags.update((current) => [...current, trimmed]);
    }
    this.tagInputControl.setValue('');
  }

  removeTag(tag: string): void {
    this.tags.update((current) => current.filter((item) => item !== tag));
  }

  tagColor(tag: string): string {
    return this.tasksService.hashCategoryToColor(tag);
  }

  async submit(): Promise<void> {
    if (this.taskForm.invalid || this.isSubmitting()) {
      this.taskForm.markAllAsTouched();
      return;
    }

    if (this.assigneeIds().length === 0) {
      this.errorMessage.set('Please assign at least one member to the task.');
      return;
    }

    const task = this.task();
    if (!task) {
      this.errorMessage.set('Task data is missing.');
      return;
    }

    const dueDateValue = this.taskForm.get('dueDate')?.value ?? null;
    const dueTimeValue = this.taskForm.get('dueTime')?.value ?? null;
    const dueDate = this.buildDueDate(dueDateValue, dueTimeValue);
    if (dueDateValue || dueTimeValue) {
      if (!dueDate) {
        this.errorMessage.set('Please provide both a due date and a due time.');
        return;
      }
    }

    const payload: TaskRequest = {
      title: this.taskForm.get('title')?.value?.trim() ?? '',
      description: this.trimOptional(this.taskForm.get('description')?.value),
      category: this.trimOptional(this.taskForm.get('category')?.value),
      priority: this.taskForm.get('priority')?.value ?? task.priority,
      status: this.taskForm.get('status')?.value ?? task.status,
      due_date: dueDate ?? undefined,
      assignee_ids: this.assigneeIds(),
      tags: this.tags(),
      created_by_id: task.created_by_id,
    };

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    try {
      const updated = await this.tasksService.updateTask(task.id, payload);
      this.task.set(updated);
      this.backToTaskDetail();
    } catch (error) {
      console.error('Failed to update task:', error);
      this.errorMessage.set('Unable to update the task. Please try again.');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  private initializeForm(): void {
    const task = this.task();
    if (!task) {
      return;
    }

    this.taskForm.patchValue({
      title: task.title ?? '',
      description: task.description ?? '',
      category: task.category ?? '',
      priority: task.priority ?? TaskPriority.Medium,
      status: task.status ?? TaskStatus.Open,
    });

    this.assigneeIds.set(task.assignee_ids ?? []);
    this.tags.set(task.tags ?? []);

    if (task.due_date) {
      const parsed = new Date(task.due_date);
      if (!Number.isNaN(parsed.getTime())) {
        this.taskForm.patchValue({
          dueDate: new Date(parsed.getFullYear(), parsed.getMonth(), parsed.getDate()),
          dueTime: this.buildTimeValue(parsed),
        });
      }
    }
  }

  private normalizeQuery(value: UserResponse | string | null): string {
    if (!value) {
      return '';
    }
    if (typeof value === 'string') {
      return value.toLowerCase();
    }
    return `${value.firstname} ${value.lastname} ${value.email}`.toLowerCase();
  }

  private buildDueDate(dateValue: Date | null, timeValue: Date | null): string | null {
    if (!dateValue || !timeValue) {
      return null;
    }
    const year = dateValue.getFullYear();
    const month = dateValue.getMonth() + 1;
    const day = dateValue.getDate();
    const hours = timeValue.getHours();
    const minutes = timeValue.getMinutes();
    return `${year}-${this.pad(month)}-${this.pad(day)}T${this.pad(hours)}:${this.pad(minutes)}:00`;
  }

  private buildTimeValue(source: Date): Date {
    const time = new Date();
    time.setHours(source.getHours(), source.getMinutes(), 0, 0);
    return time;
  }

  private pad(value: number): string {
    return value.toString().padStart(2, '0');
  }

  private trimOptional(value: string | null | undefined): string | undefined {
    const trimmed = value?.trim();
    return trimmed ? trimmed : undefined;
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

  private backToGroupsDetail(): void {
    this.router.navigate([
      '/groups/' + this.route.snapshot.paramMap.get('groupId'),
    ]);
  }
}
