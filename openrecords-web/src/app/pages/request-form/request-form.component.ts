import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { CreateFoiaRequest } from '../../models/foia-request.model';

/**
 * State machine for the form's submission status.
 *   - idle: form is editable, no submission in flight
 *   - submitting: POST is pending, disable inputs
 *   - error: server rejected the submission, show details
 */
type SubmitState =
  | { kind: 'idle' }
  | { kind: 'submitting' }
  | { kind: 'error'; message: string; fieldErrors?: Record<string, string> };

@Component({
  selector: 'app-request-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './request-form.component.html',
  styleUrl: './request-form.component.scss',
})
export class RequestFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  readonly submitState = signal<SubmitState>({ kind: 'idle' });

  /**
   * Reactive form with validators that match the backend DTO constraints.
   * If you change limits here, also update CreateFoiaRequestDto.java.
   */
  readonly form: FormGroup = this.fb.group({
    subject: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', [Validators.required, Validators.maxLength(10_000)]],
    recordsRequested: ['', [Validators.required, Validators.maxLength(10_000)]],
    dateRangeStart: [null],
    dateRangeEnd: [null],
    feeWaiverRequested: [false],
    maxFeeWilling: [null, [Validators.min(0)]],
  });

  /**
   * Helper for the template — true if the field has been touched/dirty AND is invalid.
   * Prevents showing red errors on fields the user hasn't interacted with yet.
   */
  hasError(controlName: string, errorKey: string): boolean {
    const control = this.form.get(controlName);
    return !!(control && control.touched && control.hasError(errorKey));
  }

  isInvalid(controlName: string): boolean {
    const control = this.form.get(controlName);
    return !!(control && control.touched && control.invalid);
  }

  /**
   * Submit handler.
   * Called by (ngSubmit) on the form element.
   */
  onSubmit(): void {
    // If form has errors, mark all controls as touched so error messages appear
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitState.set({ kind: 'submitting' });

    const payload: CreateFoiaRequest = this.form.value;

    this.api.createRequest(payload).subscribe({
      next: (created) => {
        // Navigate to the detail page for the newly created request
        this.router.navigate(['/requests', created.id]);
      },
      error: (err) => {
        console.error('Failed to create request:', err);

        // Try to extract field errors from the RFC 7807 ProblemDetail
        const fieldErrors: Record<string, string> = {};
        const backendErrors = err?.error?.errors;
        if (Array.isArray(backendErrors)) {
          for (const e of backendErrors) {
            if (e?.field && e?.message) {
              fieldErrors[e.field] = e.message;
            }
          }
        }

        this.submitState.set({
          kind: 'error',
          message:
            err?.error?.detail ??
            err?.message ??
            'Failed to submit your request. Please try again.',
          fieldErrors:
            Object.keys(fieldErrors).length > 0 ? fieldErrors : undefined,
        });
      },
    });
  }
}