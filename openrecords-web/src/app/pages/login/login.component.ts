import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

type SubmitState =
  | { kind: 'idle' }
  | { kind: 'submitting' }
  | { kind: 'error'; message: string }
  | { kind: 'unverified'; message: string };

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  readonly submitState = signal<SubmitState>({ kind: 'idle' });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitState.set({ kind: 'submitting' });

    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        // Redirect to the requested page (route preservation) or default to /requests
        const redirectTo =
          this.route.snapshot.queryParamMap.get('redirect') ?? '/requests';
        this.router.navigateByUrl(redirectTo);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 403) {
          this.submitState.set({
            kind: 'unverified',
            message:
              err.error?.detail ??
              'Please verify your email before logging in.',
          });
        } else if (err.status === 401) {
          this.submitState.set({
            kind: 'error',
            message: 'Email or password is incorrect.',
          });
        } else if (err.status === 0) {
          this.submitState.set({
            kind: 'error',
            message: 'Unable to reach the server. Check your connection.',
          });
        } else {
          this.submitState.set({
            kind: 'error',
            message: err.error?.detail ?? 'Login failed. Please try again.',
          });
        }
      },
    });
  }

  fieldHasError(name: 'email' | 'password'): boolean {
    const control = this.form.controls[name];
    return control.invalid && (control.touched || control.dirty);
  }
}