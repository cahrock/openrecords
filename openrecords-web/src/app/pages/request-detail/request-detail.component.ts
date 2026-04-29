import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import {
  FoiaRequest,
  FoiaRequestStatus,
  StatusHistoryEntry,
  TERMINAL_STATUSES,
} from '../../models/foia-request.model';
import { getAllowedTransitions } from '../../models/status-transitions';
import { ASSIGNABLE_STAFF, UserContextService } from '../../services/user-context.service';

type DetailState =
  | { kind: 'loading' }
  | { kind: 'ok'; data: FoiaRequest }
  | { kind: 'error'; message: string; status?: number };

type ActionState =
  | { kind: 'idle' }
  | { kind: 'submitting' }
  | { kind: 'error'; message: string };

@Component({
  selector: 'app-request-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DatePipe],
  templateUrl: './request-detail.component.html',
  styleUrl: './request-detail.component.scss',
})
export class RequestDetailComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  readonly userContext = inject(UserContextService);

  readonly state = signal<DetailState>({ kind: 'loading' });
  readonly actionState = signal<ActionState>({ kind: 'idle' });

  // Form state for status transition + assignment
  readonly transitionTarget = signal<FoiaRequestStatus | ''>('');
  readonly transitionReason = signal<string>('');
  readonly assigneeId = signal<number | null>(null);

  readonly isStaff = this.userContext.isStaff;
  readonly assignableStaff = ASSIGNABLE_STAFF;

  /** Computed list of legal transitions for the current request. */
  readonly allowedTransitions = computed<FoiaRequestStatus[]>(() => {
    const s = this.state();
    if (s.kind !== 'ok') return [];
    return getAllowedTransitions(s.data.status);
  });

  readonly history = signal<StatusHistoryEntry[]>([]);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.state.set({ kind: 'error', message: 'No request ID provided.' });
      return;
    }
    this.loadRequest(id);
  }

  loadRequest(id: string): void {
    this.state.set({ kind: 'loading' });
    this.api.getRequestById(id).subscribe({
      next: (data) => {
        this.state.set({ kind: 'ok', data });
        this.assigneeId.set(data.assignee?.id ?? null);
        this.loadHistory(id);
      },
      error: (err) => {
        console.error('Failed to load request:', err);
        this.state.set({
          kind: 'error',
          message: err?.error?.detail ?? err?.message ?? 'Failed to load request',
          status: err?.status,
        });
      },
    });
  }

  /** Load the audit-trail history for the current request. */
  loadHistory(id: string): void {
    this.api.getRequestHistory(id).subscribe({
      next: (history) => this.history.set(history),
      error: (err) => {
        console.error('Failed to load history:', err);
        // Non-fatal — the page still works without history
      },
    });
  }

  /** Apply a status transition. */
  transitionStatus(): void {
    const s = this.state();
    if (s.kind !== 'ok') return;
    if (!this.transitionTarget()) return;

    this.actionState.set({ kind: 'submitting' });

    this.api.transitionStatus(
      s.data.id,
      this.transitionTarget(),
      this.transitionReason() || ''
    ).subscribe({
      next: (updated) => {
        this.state.set({ kind: 'ok', data: updated });
        this.transitionTarget.set('');
        this.transitionReason.set('');
        this.loadHistory(updated.id);
        this.actionState.set({ kind: 'idle' });
      },
      error: (err) => {
        console.error('Transition failed:', err);
        this.actionState.set({
          kind: 'error',
          message: err?.error?.detail ?? err?.message ?? 'Transition failed',
        });
      },
    });
  }

  /** Apply assignment change. */
  saveAssignment(): void {
    const s = this.state();
    if (s.kind !== 'ok') return;

    this.actionState.set({ kind: 'submitting' });

    this.api.assignRequest(s.data.id, this.assigneeId()).subscribe({
      next: (updated) => {
        this.state.set({ kind: 'ok', data: updated });
        this.loadHistory(updated.id);
        this.actionState.set({ kind: 'idle' });
      },
      error: (err) => {
        console.error('Assignment failed:', err);
        this.actionState.set({
          kind: 'error',
          message: err?.error?.detail ?? err?.message ?? 'Assignment failed',
        });
      },
    });
  }

  /** Quick-action: assign to current user (mock auth). */
  assignToMe(): void {
    this.assigneeId.set(this.userContext.current().id);
    this.saveAssignment();
  }

  formatStatus(status: FoiaRequestStatus): string {
    return status
      .toLowerCase()
      .split('_')
      .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
      .join(' ');
  }

  isTerminal(status: FoiaRequestStatus): boolean {
    return TERMINAL_STATUSES.has(status);
  }
}