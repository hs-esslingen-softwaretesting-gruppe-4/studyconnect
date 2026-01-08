import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { GroupsServiceWrapper } from '../../services/wrapper-services/groups.service.wrapper';
import { GroupsEditComponent } from './groups-edit.component';

describe('GroupsEditComponent', () => {
  let component: GroupsEditComponent;
  let fixture: ComponentFixture<GroupsEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GroupsEditComponent],
      providers: [
        provideRouter([]),
        {
          provide: GroupsServiceWrapper,
          useValue: {
            updateGroupDetails: () => Promise.resolve(),
          },
        },
        {
          provide: MatDialog,
          useValue: {
            open: () => ({ afterClosed: () => ({ subscribe: () => {} }) }),
          },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => {
                  if (key === 'groupId') {
                    return '5';
                  }
                  return null;
                },
              },
              data: {
                userId: 1,
                users: [
                  { value: 1, label: 'Test User (test@example.com)' },
                  { value: 2, label: 'Sample User (sample@example.com)' },
                ],
                groupResolvedData: {
                  group: {
                    id: 5,
                    name: 'Testing Group',
                    description: 'Group description',
                    is_public: true,
                    created_by_id: 1,
                    created_at: '2024-06-10T09:00:00',
                    last_updated_at: '2024-06-12T10:00:00',
                    member_count: 2,
                    max_members: 10,
                    invite_code: 'invite',
                  },
                  members: [
                    {
                      id: 1,
                      email: 'test@example.com',
                      firstname: 'Test',
                      lastname: 'User',
                    },
                  ],
                  admins: [
                    {
                      id: 1,
                      email: 'test@example.com',
                      firstname: 'Test',
                      lastname: 'User',
                    },
                  ],
                  tasks: [],
                },
              },
            },
          },
        },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(GroupsEditComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
