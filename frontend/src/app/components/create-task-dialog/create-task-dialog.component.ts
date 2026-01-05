import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTimepickerModule } from '@angular/material/timepicker';
import { TaskPriority, TaskRequest, TaskStatus, UserResponse } from '../../api-services/api';
import { TasksServiceWrapper } from '../../services/wrapper-services/tasks.service.wrapper';

export interface CreateTaskDialogData {
  groupId: number;
  members: UserResponse[];
  userId: number;
}

@Component({
  selector: 'app-create-task-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTimepickerModule,
  ],
  templateUrl: './create-task-dialog.component.html',
  styleUrl: './create-task-dialog.component.scss',
  providers: [provideNativeDateAdapter()],
  standalone: true,
})
export class CreateTaskDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<CreateTaskDialogComponent>);
  private readonly data = inject(MAT_DIALOG_DATA) as CreateTaskDialogData;
  private readonly tasksService = inject(TasksServiceWrapper);
  private readonly fb = inject(FormBuilder);

  readonly members = signal<UserResponse[]>(this.data.members);
  readonly assigneeIds = signal<number[]>([]);
  readonly tags = signal<string[]>([]);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal('');

  readonly taskForm = this.fb.group({
    title: ['', Validators.required],
    description: [''],
    category: [''],
    priority: [TaskPriority.Medium, Validators.required],
    dueDate: [null as Date | null],
    dueTime: [null as Date | null],
  });

  readonly tagInputControl = new FormControl<string>('');
  readonly assigneeSearchControl = new FormControl<UserResponse | string>('');

  private readonly assigneeSearchValue = toSignal(
    this.assigneeSearchControl.valueChanges,
    { initialValue: '' }
  );

  readonly filteredAssignees = computed(() => {
    const query = this.normalizeQuery(this.assigneeSearchValue());
    const excluded = new Set(this.assigneeIds());
    return this.members().filter(member => {
      if (excluded.has(member.id)) {
        return false;
      }
      const label = `${member.firstname} ${member.lastname} ${member.email}`.toLowerCase();
      return label.includes(query);
    });
  });

  readonly assigneeCards = computed(() =>
    this.assigneeIds().map(id => {
      const member = this.members().find(user => user.id === id);
      return {
        id,
        name: member ? `${member.firstname} ${member.lastname}` : `User ${id}`,
      };
    })
  );

  readonly displayMember = (value: UserResponse | string | null): string => {
    if (!value) {
      return '';
    }
    if (typeof value === 'string') {
      return value;
    }
    return `${value.firstname} ${value.lastname}`;
  };

  readonly TaskPriority = TaskPriority;

  onAssigneeSelected(event: MatAutocompleteSelectedEvent): void {
    const selected = event.option.value as UserResponse | null;
    if (selected) {
      this.addAssignee(selected.id);
    }
    this.assigneeSearchControl.setValue('');
  }

  addAssignee(userId: number): void {
    this.assigneeIds.update(current =>
      current.includes(userId) ? current : [...current, userId]
    );
  }

  removeAssignee(userId: number): void {
    this.assigneeIds.update(current => current.filter(id => id !== userId));
  }

  addTag(): void {
    const rawValue = this.tagInputControl.value ?? '';
    const trimmed = rawValue.trim();
    if (!trimmed) {
      return;
    }
    const normalized = trimmed.toLowerCase();
    const existing = new Set(this.tags().map(tag => tag.toLowerCase()));
    if (!existing.has(normalized)) {
      this.tags.update(current => [...current, trimmed]);
    }
    this.tagInputControl.setValue('');
  }

  removeTag(tag: string): void {
    this.tags.update(current => current.filter(item => item !== tag));
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
      priority: this.taskForm.get('priority')?.value ?? TaskPriority.Medium,
      status: TaskStatus.Open,
      due_date: dueDate ?? undefined,
      assignee_ids: this.assigneeIds(),
      tags: this.tags(),
      created_by_id: this.data.userId,
    };

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    try {
      const created = await this.tasksService.createTaskForGroup(this.data.groupId, payload);
      this.dialogRef.close(created);
    } catch (error) {
      console.error('Failed to create task:', error);
      this.errorMessage.set('Unable to create the task. Please try again.');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  cancel(): void {
    this.dialogRef.close();
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

  private pad(value: number): string {
    return value.toString().padStart(2, '0');
  }

  private trimOptional(value: string | null | undefined): string | undefined {
    const trimmed = value?.trim();
    return trimmed ? trimmed : undefined;
  }
}
