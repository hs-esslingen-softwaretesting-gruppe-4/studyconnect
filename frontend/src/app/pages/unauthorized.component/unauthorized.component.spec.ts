import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UnauthorizedComponent } from './unauthorized.component';
import { AuthService } from '../../services/auth.service';
import { provideRouter } from '@angular/router';

describe('UnauthorizedComponent', () => {
  let component: UnauthorizedComponent;
  let fixture: ComponentFixture<UnauthorizedComponent>;

  beforeEach(async () => {
    const authServiceStub = {
      isAuthenticated: () => false,
      login: () => Promise.resolve(),
    };

    await TestBed.configureTestingModule({
      imports: [UnauthorizedComponent],
      providers: [{ provide: AuthService, useValue: authServiceStub }, provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(UnauthorizedComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
