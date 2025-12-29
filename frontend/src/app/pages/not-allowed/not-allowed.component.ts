import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-not-allowed',
  imports: [CommonModule, MatCardModule, MatButtonModule, RouterModule],
  templateUrl: './not-allowed.component.html',
  styleUrl: './not-allowed.component.scss',
})
export class NotAllowedComponent {
  constructor(private readonly authService: AuthService) {}

  logout(): void {
    void this.authService.logout();
  }
}
