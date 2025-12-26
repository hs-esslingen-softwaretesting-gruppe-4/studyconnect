import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NotAllowedComponent } from './not-allowed.component';

describe('NotAllowedComponent', () => {
  let component: NotAllowedComponent;
  let fixture: ComponentFixture<NotAllowedComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotAllowedComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NotAllowedComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
