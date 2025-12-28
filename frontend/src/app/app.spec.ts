import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { App } from './app';
import { AuthService } from './services/auth.service';
import { provideRouter } from '@angular/router';

describe('App', () => {
  beforeEach(async () => {
    const authServiceStub = {
      getFullName: () => Promise.resolve('Test User'),
      isAuthenticated: () => false,
      login: () => Promise.resolve(),
      logout: () => Promise.resolve(),
    };

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [{ provide: AuthService, useValue: authServiceStub }, provideRouter([]), provideNoopAnimations()],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the router outlet', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('router-outlet')).toBeTruthy();
  });
});
