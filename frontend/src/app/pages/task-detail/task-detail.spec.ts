import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';

import { TaskDetailComponent } from './task-detail';
import { TasksServiceWrapper } from '../../services/wrapper-services/tasks.service.wrapper';
import { MatDialog } from '@angular/material/dialog';

describe('TaskDetailComponent', () => {
  let component: TaskDetailComponent;
  let fixture: ComponentFixture<TaskDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskDetailComponent],
      providers: [
        provideRouter([]),
        {
          provide: TasksServiceWrapper,
          useValue: {
            hashCategoryToColor: () => '#9aa0a6',
            deleteTask: () => Promise.resolve(),
          },
        },
        {
          provide: MatDialog,
          useValue: {
            open: () => ({}),
          },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              data: {
                userId: 1,
                task: {
                  id: 2,
                  title: 'Write tests',
                  description: 'Add coverage for task detail',
                  due_date: '2024-06-15T12:30:00',
                  priority: 'HIGH',
                  status: 'OPEN',
                  category: 'Testing',
                  tags: ['frontend', 'priority'],
                  created_by_id: 1,
                  assignee_ids: [1],
                  created_at: '2024-06-10T09:00:00',
                  updated_at: '2024-06-12T10:00:00',
                  last_status_change_at: '2024-06-11T08:00:00',
                  group_id: 5,
                  tagsHashColor: '#22d3ee',
                },
                groupMembers: [
                  {
                    id: 1,
                    email: 'test@example.com',
                    firstname: 'Test',
                    lastname: 'User',
                  },
                ],
              },
              paramMap: {
                get: (key: string) => {
                  if (key === 'groupId') {
                    return '5';
                  }
                  if (key === 'taskId') {
                    return '2';
                  }
                  return null;
                },
              },
            },
          },
        },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(TaskDetailComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
