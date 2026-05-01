import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

type VerifyState =
  | { kind: 'verifying' }
  | { kind: 'success'; email: string; message: string }
  | { kind: 'error'; message: string }
  | { kind: 'missing-token' };

@Component({
  selector: 'app-verify',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './verify.component.html',
  styleUrl: './verify.component.scss',
})
export class VerifyComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);

  readonly state = signal<VerifyState>({ kind: 'verifying' });

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token || token.trim().length === 0) {
      this.state.set({ kind: 'missing-token' });
      return;
    }

    this.auth.verifyEmail(token).subscribe({
      next: (response) => {
        this.state.set({
          kind: 'success',
          email: response.email,
          message: response.message,
        });
      },
      error: (err: HttpErrorResponse) => {
        this.state.set({
          kind: 'error',
          message:
            err.error?.detail ??
            'The verification link is invalid or has expired.',
        });
      },
    });
  }
}