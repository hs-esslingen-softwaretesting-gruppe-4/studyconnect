import { AutocompleteType } from '../../models/autocomplete-type';
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
import { GroupResponse, UpdateGroupRequest } from '../../api-services/api';
import { GroupsServiceWrapper } from '../../services/wrapper-services/groups.service.wrapper';
import {
  StatusDialogComponent,
  StatusDialogData,
} from '../../components/status-dialog/status-dialog.component';

@Component({
  selector: 'app-groups-edit.component',
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
  templateUrl: './groups-edit.component.html',
  styleUrl: './groups-edit.component.scss',
})
export class GroupsEditComponent implements OnInit {

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly groupsService: GroupsServiceWrapper,
    private readonly dialog: MatDialog,
    private readonly fb: FormBuilder
  ) {
    this.editGroupForm = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      is_public: [true, Validators.required],
      max_members: [null, [Validators.required, Validators.min(2)]],
    });
  }

  group = signal<GroupResponse | undefined>(undefined);
  members = signal<number[]>([]);
  admins = signal<number[]>([]);
  userId = signal<number | undefined>(undefined);
  isAdmin = signal<boolean>(false);
  usersFormattedForAutocomplete = signal<AutocompleteType[]>([]);
  isSubmitting = signal(false);
  private readonly initialMembers = signal<number[]>([]);
  private readonly initialAdmins = signal<number[]>([]);

  editGroupForm: FormGroup;
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
    const resolvedData = this.route.snapshot.data['groupResolvedData'];
    const group = resolvedData?.group as GroupResponse | undefined;
    const memberIds = (resolvedData?.members ?? []).map((member: { id: number }) => member.id);
    const adminIds = (resolvedData?.admins ?? []).map((admin: { id: number }) => admin.id);

    this.group.set(group);
    this.members.set(memberIds);
    this.admins.set(adminIds);
    this.initialMembers.set([...memberIds]);
    this.initialAdmins.set([...adminIds]);
    this.userId.set(this.route.snapshot.data['userId']);
    this.usersFormattedForAutocomplete.set(this.route.snapshot.data['users'] ?? []);

    this.isAdmin.set(this.admins().some(adminId => adminId === this.userId()));

    if (group) {
      this.editGroupForm.patchValue({
        name: group.name,
        description: group.description ?? '',
        is_public: group.is_public,
        max_members: group.max_members,
      });
    }
  }

  backToGroupDetail(): void {
    const groupId = this.group()?.id ?? this.route.snapshot.paramMap.get('groupId');
    this.router.navigate(['/groups', groupId]);
  }

  addMember(userId: number): void {
    this.members.update(current =>
      current.includes(userId) ? current : [...current, userId]
    );
  }

  addAdmin(userId: number): void {
    this.admins.update(current =>
      current.includes(userId) ? current : [...current, userId]
    );
  }

  removeMember(userId: number): void {
    this.members.update(current => current.filter(id => id !== userId));
    this.admins.update(current => current.filter(id => id !== userId));
  }

  removeAdmin(userId: number): void {
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
    if (!this.isAdmin()) {
      return;
    }
    if (this.editGroupForm.invalid || this.isSubmitting()) {
      this.editGroupForm.markAllAsTouched();
      return;
    }

    const groupId = this.group()?.id;
    if (!groupId) {
      this.openStatusDialog({
        title: 'Missing group',
        message: 'Group details could not be loaded. Please try again.',
        type: 'error',
        autoCloseMs: 5000,
      });
      return;
    }

    const formValue = this.editGroupForm.getRawValue();
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

    const memberIds = this.members();
    const adminIds = this.admins().filter(id => memberIds.includes(id));
    if (adminIds.length !== this.admins().length) {
      this.admins.set(adminIds);
    }

    const removedMembers = this.initialMembers().filter(id => !memberIds.includes(id));
    const removedAdmins = this.initialAdmins()
      .filter(id => !adminIds.includes(id))
      .filter(id => !removedMembers.includes(id));

    const requestData: UpdateGroupRequest = {
      name: (formValue.name ?? '').trim(),
      description: (formValue.description ?? '').trim() || undefined,
      is_public: formValue.is_public,
      max_members: maxMembersValue,
      member_ids: memberIds,
      admin_ids: adminIds,
    };

    this.isSubmitting.set(true);
    try {
      await this.removeMembersAndAdmins(groupId, removedMembers, removedAdmins);
      const updatedGroup = await this.groupsService.updateGroupDetails(groupId, requestData);
      this.group.set(updatedGroup);
      this.openStatusDialog({
        title: 'Group updated',
        message: `The group "${updatedGroup.name}" has been updated.`,
        type: 'success',
        autoCloseMs: 5000,
      });
      this.router.navigate(['/groups', updatedGroup.id]);
    } catch (error) {
      console.error('Error updating group:', error);
      this.openStatusDialog({
        title: 'Update failed',
        message: 'An error occurred while updating the group. Please try again.',
        type: 'error',
        autoCloseMs: 5000,
      });
    } finally {
      this.isSubmitting.set(false);
    }
  }

  private async removeMembersAndAdmins(
    groupId: number,
    removedMembers: number[],
    removedAdmins: number[]
  ): Promise<void> {
    const adminRemovals = removedAdmins.map(userId =>
      this.groupsService.removeGroupAdmin(groupId, userId)
    );
    const memberRemovals = removedMembers.map(userId =>
      this.groupsService.leaveGroup(groupId, userId)
    );

    if (adminRemovals.length === 0 && memberRemovals.length === 0) {
      return;
    }

    await Promise.all([...adminRemovals, ...memberRemovals]);
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
    return this.usersFormattedForAutocomplete().filter(option => {
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
    const user = this.usersFormattedForAutocomplete().find(
      option => Number(option.value) === userId
    );
    if (!user) {
      return `User ${userId}`;
    }
    const namePart = user.label.split('(')[0]?.trim();
    return namePart || user.label;
  }
}
