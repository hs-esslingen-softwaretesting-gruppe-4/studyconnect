import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { RegisterComponent } from './register.component';
import { UsersServiceWrapper } from '../../services/wrapper-services/users.service.wrapper';
import { AuthService } from '../../services/auth.service';

describe('RegisterComponent', () => {
  let usersServiceSpy: { createUser: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    usersServiceSpy = {
      createUser: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        { provide: UsersServiceWrapper, useValue: usersServiceSpy },
        { provide: AuthService, useValue: {} },
        provideRouter([]),
      ],
    }).compileComponents();
  });

  it('stops loading and shows an error banner when registration fails', async () => {
    usersServiceSpy.createUser.mockRejectedValue({ error: { message: 'Backend error' } });

    const fixture = TestBed.createComponent(RegisterComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.registerForm.setValue({
      email: 'test@example.com',
      surname: 'Test',
      lastname: 'User',
      password: 'Password1!',
    });

    await component.onSubmit();
    fixture.detectChanges();

    expect(component.isLoading).toBeFalsy();
    expect(component.errorMessage).toBe('Backend error');
    expect(fixture.nativeElement.querySelector('.error-banner')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('mat-spinner')).toBeFalsy();
  });
});
