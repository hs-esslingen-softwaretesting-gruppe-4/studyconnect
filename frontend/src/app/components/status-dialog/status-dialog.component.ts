import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export type StatusDialogType = 'success' | 'error' | 'info';

export interface StatusDialogData {
  title: string;
  message: string;
  type: StatusDialogType;
  autoCloseMs?: number;
}

@Component({
  selector: 'app-status-dialog',
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './status-dialog.component.html',
  styleUrl: './status-dialog.component.scss',
  standalone: true,
})
export class StatusDialogComponent implements OnInit, OnDestroy {
  readonly data = inject(MAT_DIALOG_DATA) as StatusDialogData;
  private readonly dialogRef = inject(MatDialogRef<StatusDialogComponent>);
  private autoCloseId: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    const autoCloseMs = this.data.autoCloseMs ?? 5000;
    if (autoCloseMs > 0) {
      this.autoCloseId = setTimeout(() => this.close(), autoCloseMs);
    }
  }

  ngOnDestroy(): void {
    if (this.autoCloseId) {
      clearTimeout(this.autoCloseId);
    }
  }

  get icon(): string {
    switch (this.data.type) {
      case 'success':
        return 'check_circle';
      case 'error':
        return 'error';
      case 'info':
        return 'info';
      default:
        return 'info';
    }
  }

  close(): void {
    this.dialogRef.close();
  }
}
