import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

type SubmitState =
  | { kind: 'idle' }
  | { kind: 'submitting' }
  | { kind: 'error'; message: string };

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  // Backend rule: 8-100 chars, at least one uppercase, lowercase, digit
  private readonly passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/;

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: [
      '',
      [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(100),
        Validators.pattern(this.passwordPattern),
      ],
    ],
    fullName: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(200)]],
  });

  readonly submitState = signal<SubmitState>({ kind: 'idle' });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitState.set({ kind: 'submitting' });

    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => {
        // Redirect to "check email" page with the email as a query param
        const email = this.form.controls.email.value;
        this.router.navigate(['/check-email'], { queryParams: { email } });
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 409) {
          this.submitState.set({
            kind: 'error',
            message:
              'An account with this email already exists. Try signing in instead.',
          });
        } else if (err.status === 400) {
          this.submitState.set({
            kind: 'error',
            message: err.error?.detail ?? 'Please check your input and try again.',
          });
        } else if (err.status === 0) {
          this.submitState.set({
            kind: 'error',
            message: 'Unable to reach the server. Check your connection.',
          });
        } else {
          this.submitState.set({
            kind: 'error',
            message: 'Registration failed. Please try again.',
          });
        }
      },
    });
  }

  fieldHasError(name: 'email' | 'password' | 'fullName'): boolean {
    const control = this.form.controls[name];
    return control.invalid && (control.touched || control.dirty);
  }

  /** Visual cue for users to track password complexity in real time. */
  passwordChecks() {
    const value = this.form.controls.password.value || '';
    return {
      length: value.length >= 8 && value.length <= 100,
      uppercase: /[A-Z]/.test(value),
      lowercase: /[a-z]/.test(value),
      digit: /\d/.test(value),
    };
  }
}