import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-login',
  imports: [MatTooltipModule, MatButtonModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {

  constructor(readonly authService: AuthService, private readonly router: Router) {}

  fullname?: string;
  initials: string = '?';

  ngOnInit(): void {
    this.initializeUser();
  }

  /**
   * Initializes the user's full name and initials by fetching the full name from the AuthService.
   * @returns {Promise<void>} A promise that resolves when the user's name and initials have been set.
   */
  private async initializeUser(): Promise<void> {
    const name = await this.authService.getFullName();
    this.fullname = name;
    this.initials = name?.match(/\b(\w)/g)?.join('').toUpperCase() ?? '?';
  }

  /**
   * Handles the login/logout functionality based on the user's authentication status.
   * If the user has a valid token, it logs them out and navigates to the home page.
   * If the user does not have a valid token, it initiates the login process.
   */
  handleLogInOut() {
    if (this.authService.isAuthenticated()) {
      this.authService.logout();
      this.router.navigate(['/']);
    } else {
      this.authService.login();
    }
  }
}
