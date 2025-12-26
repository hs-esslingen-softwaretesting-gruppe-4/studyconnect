import { Component, Input, Output, EventEmitter } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { MatIconButton } from '@angular/material/button';


@Component({
  selector: 'app-error-banner',
  imports: [MatIcon, MatIconButton],
  templateUrl: './error-banner.component.html',
  styleUrl: './error-banner.component.scss',
  standalone: true,
})
export class ErrorBannerComponent {
  @Input() errorMessage: string = '';
  @Output() dismissed = new EventEmitter<void>();

  dismiss(): void {
    this.errorMessage = '';
    this.dismissed.emit();
  }
}
