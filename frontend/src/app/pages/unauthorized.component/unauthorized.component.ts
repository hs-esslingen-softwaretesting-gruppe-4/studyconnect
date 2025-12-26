import { Component, OnInit, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-unauthorized.component',
  imports: [MatButtonModule, RouterModule],
  templateUrl: './unauthorized.component.html',
  styleUrl: './unauthorized.component.scss',
})
export class UnauthorizedComponent implements OnInit {
  constructor(private readonly authService: AuthService) {}

  isAuthorized = signal(false);

  ngOnInit(): void {
    this.isAuthorized.set(this.authService.isAuthenticated());
  }
  handleLogIn() {
    this.authService.login();
  }

}
