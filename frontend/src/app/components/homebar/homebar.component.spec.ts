import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HomebarComponent } from './homebar.component';

describe('Homebar', () => {
  let component: HomebarComponent;
  let fixture: ComponentFixture<HomebarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomebarComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(HomebarComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
