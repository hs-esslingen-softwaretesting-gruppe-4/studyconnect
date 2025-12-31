import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GroupsDetailComponent } from './groups-detail.component';
import { provideRouter } from '@angular/router';

describe('GroupsDetailComponent', () => {
  let component: GroupsDetailComponent;
  let fixture: ComponentFixture<GroupsDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GroupsDetailComponent],
      providers: [provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(GroupsDetailComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
