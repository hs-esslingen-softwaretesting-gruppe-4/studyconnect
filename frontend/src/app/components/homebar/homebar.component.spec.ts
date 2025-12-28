import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HomebarComponent } from './homebar.component';
import { AuthService } from '../../services/auth.service';
import { provideRouter } from '@angular/router';

describe('Homebar', () => {
  let component: HomebarComponent;
  let fixture: ComponentFixture<HomebarComponent>;

  beforeEach(async () => {
    const authServiceStub = {
      getFullName: () => Promise.resolve('Test User'),
      isAuthenticated: () => false,
      login: () => Promise.resolve(),
      logout: () => Promise.resolve(),
    };

    await TestBed.configureTestingModule({
      imports: [HomebarComponent,],
      providers: [{ provide: AuthService, useValue: authServiceStub }, provideRouter([])],
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
