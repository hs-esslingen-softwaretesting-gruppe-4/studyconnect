import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { vi } from 'vitest';

import { CreateGroupComponent } from './create-group.component';
import { GroupsServiceWrapper } from '../../services/wrapper-services/groups.service.wrapper';

describe('CreateGroupComponent', () => {
  let component: CreateGroupComponent;
  let fixture: ComponentFixture<CreateGroupComponent>;
  let groupsServiceSpy: { createGroup: ReturnType<typeof vi.fn> };
  let dialogSpy: { open: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    groupsServiceSpy = {
      createGroup: vi.fn(),
    };
    dialogSpy = {
      open: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [CreateGroupComponent],
      providers: [
        provideRouter([]),
        { provide: GroupsServiceWrapper, useValue: groupsServiceSpy },
        { provide: MatDialog, useValue: dialogSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              data: {
                userId: 1,
                users: [{ label: 'Test User (test@example.com)', value: 1 }],
              },
            },
          },
        },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreateGroupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
