import { AutocompleteType } from './../../models/autocomplete-type';
import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { GroupsServiceWrapper } from '../../services/wrapper-services/groups.service.wrapper';
import { CreateGroupRequest } from '../../api-services/api';
import { StatusDialogComponent, StatusDialogData } from '../../components/status-dialog/status-dialog.component';

@Component({
  selector: 'app-create-group.component',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIcon,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './create-group.component.html',
  styleUrl: './create-group.component.scss',
})
export class CreateGroupComponent implements OnInit {
  constructor(
    private readonly groupsService: GroupsServiceWrapper,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly dialog: MatDialog,
    private readonly fb: FormBuilder
  ) {
    this.createGroupForm = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      is_public: [true, Validators.required],
      max_members: [null, [Validators.required, Validators.min(2)]],
    });
  }

  currentUserId = signal<number | undefined>(undefined);
  usersFormattedForAutcomplete = signal<AutocompleteType[]>([]);
  members = signal<number[]>([]);
  admins = signal<number[]>([]);
  isSubmitting = signal(false);

  createGroupForm: FormGroup;
  memberSearchControl = new FormControl<AutocompleteType | string>('');
  adminSearchControl = new FormControl<AutocompleteType | string>('');

  private readonly memberSearchValue = toSignal(this.memberSearchControl.valueChanges, {
    initialValue: '',
  });
  private readonly adminSearchValue = toSignal(this.adminSearchControl.valueChanges, {
    initialValue: '',
  });

  readonly filteredMemberOptions = computed(() =>
    this.filterUserOptions(this.memberSearchValue(), this.members())
  );
  readonly filteredAdminOptions = computed(() =>
    this.filterUserOptions(this.adminSearchValue(), this.admins())
  );
  readonly memberCards = computed(() => this.buildUserCards(this.members()));
  readonly adminCards = computed(() => this.buildUserCards(this.admins()));

  readonly displayAutocompleteLabel = (
    value: AutocompleteType | string | number | null
  ): string => {
    if (value === null || value === undefined) {
      return '';
    }
    if (typeof value === 'string') {
      return value;
    }
    if (typeof value === 'number') {
      return this.getUserDisplayName(value);
    }
    return value.label;
  };

  ngOnInit(): void {
    this.currentUserId.set(this.route.snapshot.data['userId']);
    this.usersFormattedForAutcomplete.set(this.route.snapshot.data['users']);

    // By default, add the current user as a member and admin.
    this.members.set(this.currentUserId() ? [this.currentUserId()!] : []);
    this.admins.set(this.currentUserId() ? [this.currentUserId()!] : []);
  }

  addMember(userId: number) {
    this.members.update(current =>
      current.includes(userId) ? current : [...current, userId]
    );
  }

  addAdmin(userId: number) {
    this.admins.update(current =>
      current.includes(userId) ? current : [...current, userId]
    );
  }

  removeMember(userId: number) {
    this.members.update(current => current.filter(id => id !== userId));
  }

  removeAdmin(userId: number) {
    this.admins.update(current => current.filter(id => id !== userId));
  }

  onMemberSelected(event: MatAutocompleteSelectedEvent): void {
    const option = event.option.value as AutocompleteType | null;
    const userId = option ? Number(option.value) : Number.NaN;

    if (!Number.isNaN(userId)) {
      this.addMember(userId);
    }
    this.memberSearchControl.setValue('');
  }

  onAdminSelected(event: MatAutocompleteSelectedEvent): void {
    const option = event.option.value as AutocompleteType | null;
    const userId = option ? Number(option.value) : Number.NaN;

    // Check if the user is selected as a member too
    if (this.members().includes(userId) === false) {
      this.openStatusDialog({
        title: 'Invalid Admin Selection',
        message: 'Please add the user as a member before making them an admin.',
        type: 'error',
        autoCloseMs: 5000,
      });
      this.adminSearchControl.setValue('');
      return;
    }
    if (!Number.isNaN(userId)) {
      this.addAdmin(userId);
    }
    this.adminSearchControl.setValue('');
  }

  async onSubmit(): Promise<void> {
    if (this.createGroupForm.invalid || this.isSubmitting()) {
      this.createGroupForm.markAllAsTouched();
      return;
    }

    const creatorId = this.currentUserId();
    if (creatorId === undefined) {
      this.openStatusDialog({
        title: 'Missing user session',
        message: 'Please sign in before creating a group.',
        type: 'error',
        autoCloseMs: 5000,
      });
      return;
    }

    const formValue = this.createGroupForm.getRawValue();
    const maxMembersValue = Number(formValue.max_members);
    if (Number.isNaN(maxMembersValue)) {
      this.openStatusDialog({
        title: 'Invalid max members',
        message: 'Please enter a valid maximum member count.',
        type: 'error',
        autoCloseMs: 5000,
      });
      return;
    }

    const requestData: CreateGroupRequest = {
      name: (formValue.name ?? '').trim(),
      description: (formValue.description ?? '').trim() || undefined,
      is_public: formValue.is_public,
      max_members: maxMembersValue,
      member_ids: this.members(),
      admin_ids: this.admins(),
      created_by_id: creatorId,
    };

    this.isSubmitting.set(true);
    try {
      const response = await this.groupsService.createGroup(requestData);
      this.openStatusDialog({
        title: 'Group Created Successfully',
        message: `The group "${response.name}" has been created successfully.`,
        type: 'success',
        autoCloseMs: 5000,
      });
      this.router.navigate(['/groups', response.id]);
    } catch (error) {
      console.error('Error creating group:', error);
      this.openStatusDialog({
        title: 'Group Creation Failed',
        message: 'An error occurred while creating the group. Please try again.',
        type: 'error',
        autoCloseMs: 5000,
      });
    } finally {
      this.isSubmitting.set(false);
    }
  }

  private openStatusDialog(data: StatusDialogData): void {
    this.dialog.open(StatusDialogComponent, {
      data,
      autoFocus: false,
    });
  }

  private filterUserOptions(
    value: AutocompleteType | string | null,
    excludedIds: number[]
  ): AutocompleteType[] {
    const query = this.normalizeQuery(value);
    const excludedSet = new Set(excludedIds);
    return this.usersFormattedForAutcomplete().filter(option => {
      const optionId = Number(option.value);
      if (excludedSet.has(optionId)) {
        return false;
      }
      return option.label.toLowerCase().includes(query);
    });
  }

  private normalizeQuery(value: AutocompleteType | string | null): string {
    if (!value) {
      return '';
    }
    if (typeof value === 'string') {
      return value.toLowerCase();
    }
    return value.label.toLowerCase();
  }

  private buildUserCards(ids: number[]): Array<{ id: number; name: string }> {
    return ids.map(id => ({
      id,
      name: this.getUserDisplayName(id),
    }));
  }

  private getUserDisplayName(userId: number): string {
    const user = this.usersFormattedForAutcomplete().find(
      option => Number(option.value) === userId
    );
    if (!user) {
      return `User ${userId}`;
    }
    const namePart = user.label.split('(')[0]?.trim();
    return namePart || user.label;
  }
}
