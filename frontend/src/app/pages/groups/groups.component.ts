import { Component, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableDataSource } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { GroupsServiceWrapper } from '../../services/wrapper-services/groups.service.wrapper';
import { GroupResponse } from '../../api-services/api';
import { GenericTableComponent } from '../../components/generic-table/generic-table.component';
import { ButtonColor, TableActionButton, TableColumn } from '../../models/generic-table';
import {
  StatusDialogComponent,
  StatusDialogData,
} from '../../components/status-dialog/status-dialog.component';

@Component({
  selector: 'app-groups.component',
  imports: [
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatDividerModule,
    MatTabsModule,
    MatDialogModule,
    GenericTableComponent,
  ],
  templateUrl: './groups.component.html',
  styleUrl: './groups.component.scss',
})
export class GroupsComponent implements OnInit {
  userId = signal<number | undefined>(undefined);
  userGroups = signal<GroupResponse[]>([]);
  publicGroups = signal<GroupResponse[]>([]);
  searchQuery = signal('');
  lastSearchQuery = signal('');
  searchResults = signal<GroupResponse[]>([]);
  isSearching = signal(false);
  joiningGroupIds = signal<Set<number>>(new Set<number>());
  showInviteJoinForm = signal(false);
  inviteCodeInput = signal('');
  joinByInviteLoading = signal(false);

  hasJoinedGroups = computed(() => this.userGroups().length > 0);
  hasSuggestedGroups = computed(() => this.suggestedGroups().length > 0);
  hasSearchResults = computed(() => this.searchResults().length > 0);
  hasSearched = computed(() => this.lastSearchQuery().length > 0);
  suggestedGroups = computed(() => this.publicGroups().slice(0, 10));
  showSearchEmptyState = computed(
    () => this.hasSearched() && !this.isSearching() && !this.hasSearchResults()
  );

  searchTableDataSource = new MatTableDataSource<GroupResponse>([]);

  readonly searchColumns: TableColumn<GroupResponse>[] = [
    { id: 'name', label: 'Group' },
    { id: 'description', label: 'Description', isUnsortable: true },
    { id: 'member_count', label: 'Members' },
    { id: 'max_members', label: 'Capacity' },
  ];

  readonly searchActions: TableActionButton<GroupResponse>[] = [
    {
      id: 'view',
      label: 'View group',
      color: ButtonColor.PRIMARY,
      action: group => this.viewGroup(group),
    },
    {
      id: 'join',
      label: 'Join',
      color: ButtonColor.ACCENT,
      action: group => this.joinGroup(group),
      showCondition: group => this.canJoinGroup(group) && !this.isJoining(group.id),
    },
  ];

  constructor(
    private readonly groupsService: GroupsServiceWrapper,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.userId.set(this.route.snapshot.data['userId']);
    this.loadAllData();
  }

  private async loadAllData(): Promise<void> {
    const userId = this.userId();
    if (userId === undefined) {
      const publicGroups = await this.groupsService.getPublicGroups();
      this.publicGroups.set(publicGroups);
      this.userGroups.set([]);
    } else {
      const [joinedGroups, publicGroups] = await Promise.all([
        this.groupsService.getJoinedGroupsForCurrentUser(userId),
        this.groupsService.getPublicGroups(),
      ]);
      this.userGroups.set(joinedGroups);
      this.publicGroups.set(publicGroups);
    }
  }

  onSearchInput(event: Event): void {
    const target = event.target as HTMLInputElement | null;
    this.searchQuery.set(target?.value ?? '');
  }

  async runSearch(): Promise<void> {
    const query = this.searchQuery().trim();
    if (!query) {
      this.clearSearch();
      return;
    }

    this.isSearching.set(true);
    this.lastSearchQuery.set(query);
    try {
      const results = await this.groupsService.searchPublicGroups(query);
      this.setSearchResults(results.slice(0, 10));
    } finally {
      this.isSearching.set(false);
    }
  }

  clearSearch(): void {
    this.searchQuery.set('');
    this.lastSearchQuery.set('');
    this.setSearchResults([]);
  }

  viewGroup(group: GroupResponse): void {
    this.router.navigate(['/groups', group.id]);
  }

  createGroup(): void {
    this.router.navigate(['/groups/create']);
  }

  toggleInviteJoinForm(): void {
    this.showInviteJoinForm.update(current => !current);
    if (!this.showInviteJoinForm()) {
      this.inviteCodeInput.set('');
    }
  }

  onInviteCodeInput(event: Event): void {
    const target = event.target as HTMLInputElement | null;
    this.inviteCodeInput.set(target?.value ?? '');
  }

  canJoinGroup(group: GroupResponse): boolean {
    const userId = this.userId();
    if (userId === undefined) {
      return false;
    }
    return !this.userGroups().some(existing => existing.id === group.id);
  }

  isJoining(groupId: number): boolean {
    return this.joiningGroupIds().has(groupId);
  }

  async joinGroup(group: GroupResponse): Promise<void> {
    const userId = this.userId();
    if (userId === undefined || this.isJoining(group.id)) {
      return;
    }

    this.joiningGroupIds.update(ids => {
      const next = new Set(ids);
      next.add(group.id);
      return next;
    });

    try {
      await this.groupsService.joinGroupByInviteCode(group.invite_code, userId);
      if (!this.userGroups().some(existing => existing.id === group.id)) {
        this.userGroups.update(groups => [...groups, group]);
      }
      this.publicGroups.update(groups => groups.filter(existing => existing.id !== group.id));
      this.openStatusDialog({
        title: 'Joined group',
        message: `You are now a member of "${group.name}".`,
        type: 'success',
        autoCloseMs: 5000,
      });
    } catch (error) {
      this.openStatusDialog({
        title: 'Join failed',
        message: this.getJoinGroupErrorMessage(error, group.name),
        type: 'error',
        autoCloseMs: 5000,
      });
    } finally {
      this.joiningGroupIds.update(ids => {
        const next = new Set(ids);
        next.delete(group.id);
        return next;
      });
    }
  }

  private setSearchResults(groups: GroupResponse[]): void {
    this.searchResults.set(groups);
    this.searchTableDataSource.data = groups;
  }

  private openStatusDialog(data: StatusDialogData): void {
    this.dialog.open(StatusDialogComponent, {
      data,
      autoFocus: false,
    });
  }

  private getJoinGroupErrorMessage(error: unknown, groupName: string): string {
    const baseMessage = `Could not join "${groupName}".`;
    if (!error || typeof error !== 'object') {
      return `${baseMessage} Please try again.`;
    }

    const apiMessage = (error as { error?: { message?: string } }).error?.message;
    if (typeof apiMessage === 'string' && apiMessage.trim().length > 0) {
      return `${baseMessage} ${apiMessage}`;
    }

    if (error instanceof Error && error.message.trim().length > 0) {
      return `${baseMessage} ${error.message}`;
    }

    return `${baseMessage} Please try again.`;
  }

  joinGroupByInviteCode(inviteCode: string): void {
    const trimmed = inviteCode.trim();
    const userId = this.userId();
    if (!trimmed || userId === undefined || this.joinByInviteLoading()) {
      if (userId === undefined) {
        this.openStatusDialog({
          title: 'Join unavailable',
          message: 'Please sign in before joining a group.',
          type: 'error',
          autoCloseMs: 5000,
        });
      }
      return;
    }

    this.joinByInviteLoading.set(true);

    this.groupsService.joinGroupByInviteCode(trimmed, userId).then(() => {
      this.loadAllData();
      this.inviteCodeInput.set('');
      this.showInviteJoinForm.set(false);
      this.openStatusDialog({
        title: 'Joined group',
        message: `You have successfully joined the group.`,
        type: 'success',
        autoCloseMs: 5000,
      });
    }).catch((error) => {
      this.openStatusDialog({
        title: 'Join failed',
        message: this.getJoinGroupErrorMessage(error, 'the group'),
        type: 'error',
        autoCloseMs: 5000,
      });
    }).finally(() => {
      this.joinByInviteLoading.set(false);
    });
  }
}
