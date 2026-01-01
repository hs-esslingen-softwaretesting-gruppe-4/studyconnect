import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NotFoundComponent } from './not-found.component';
import { AuthService } from '../../services/auth.service';
import { provideRouter } from '@angular/router';

describe('NotFoundComponent', () => {
  let component: NotFoundComponent;
  let fixture: ComponentFixture<NotFoundComponent>;

  beforeEach(async () => {

    const authServiceStub = {
      isAuthenticated: () => false,
      login: () => Promise.resolve(),
    };

    await TestBed.configureTestingModule({
      imports: [NotFoundComponent],
      providers: [{ provide: AuthService, useValue: authServiceStub }, provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NotFoundComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
