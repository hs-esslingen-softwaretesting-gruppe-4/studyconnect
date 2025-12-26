import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NavbarButtonComponent } from './navbar-button.component';

describe('NavbarButton', () => {
  let component: NavbarButtonComponent;
  let fixture: ComponentFixture<NavbarButtonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NavbarButtonComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NavbarButtonComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
