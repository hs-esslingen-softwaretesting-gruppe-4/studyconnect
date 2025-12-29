import { Component, NgZone, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../services/auth.service';
import { ErrorBannerComponent } from '../../components/error-banner/error-banner.component';
import { UsersServiceWrapper } from '../../services/wrapper-services/users.service.wrapper';
import { MatIcon } from "@angular/material/icon";

@Component({
  selector: 'app-register',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    ErrorBannerComponent,
    MatIcon
],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  registerForm: FormGroup;
  isLoading = signal(false);
  errorMessage = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly usersService: UsersServiceWrapper,
    private readonly ngZone: NgZone
  ) {
    this.registerForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      surname: ['', Validators.required],
      lastname: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(8), Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&"#§€<>_\-/|(),\\\]{}])[A-Za-z\d@$!%*?&"#§€<>_\-/|(),\\\]{}]{8,}$/)]], // Atleast 8 characters, one uppercase, one lowercase, one number, one special character
    });
  }


  async onSubmit(): Promise<void> {
    if (this.registerForm.valid) {
      this.isLoading.set(true);
      this.errorMessage = '';

      try {
        await this.usersService.createUser(this.registerForm.value);
      } catch (error: any) {
        this.ngZone.run(() => {
          this.errorMessage =
            error?.error?.message || 'Registration failed. Please try again.';
        });
        return;
      }
      finally {
        this.ngZone.run(() => {
          this.isLoading.set(false);
        });
      }

      // On successful registration, redirect to login with email pre-filled
      this.authService.login();
    }
  }

  handleLogin(): void {
    this.authService.login();
  }
}
