import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { GroupsServiceWrapper } from '../../services/wrapper-services/groups.service.wrapper';
import { GroupsComponent } from './groups.component';

describe('GroupsComponent', () => {
  let component: GroupsComponent;
  let fixture: ComponentFixture<GroupsComponent>;
  let groupsServiceSpy: {
    getPublicGroups: ReturnType<typeof vi.fn>;
    getJoinedGroupsForCurrentUser: ReturnType<typeof vi.fn>;
    searchPublicGroups: ReturnType<typeof vi.fn>;
    joinGroupByInviteCode: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    groupsServiceSpy = {
      getPublicGroups: vi.fn().mockResolvedValue([]),
      getJoinedGroupsForCurrentUser: vi.fn().mockResolvedValue([]),
      searchPublicGroups: vi.fn().mockResolvedValue([]),
      joinGroupByInviteCode: vi.fn().mockResolvedValue(undefined),
    };

    await TestBed.configureTestingModule({
      imports: [GroupsComponent],
      providers: [
        { provide: GroupsServiceWrapper, useValue: groupsServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { data: { userId: 1 } } },
        },
        provideRouter([]),
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(GroupsComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
