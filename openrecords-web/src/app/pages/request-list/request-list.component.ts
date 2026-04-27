import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import {
  FoiaRequest,
  FoiaRequestStatus,
  PageResponse,
  TERMINAL_STATUSES,
} from '../../models/foia-request.model';

/**
 * Discriminated union of the three possible UI states.
 *
 * Every async UI page handles three cases:
 *   - loading: still waiting for the API
 *   - ok: data arrived, render it
 *   - error: something went wrong, show a message
 */
type ListState =
  | { kind: 'loading' }
  | { kind: 'ok'; data: PageResponse<FoiaRequest> }
  | { kind: 'error'; message: string };

@Component({
  selector: 'app-request-list',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './request-list.component.html',
  styleUrl: './request-list.component.scss',
})
export class RequestListComponent implements OnInit {
  private readonly api = inject(ApiService);

  readonly state = signal<ListState>({ kind: 'loading' });

  ngOnInit(): void {
    this.loadRequests();
  }

  loadRequests(): void {
    this.state.set({ kind: 'loading' });
    this.api.listRequests({ size: 20, sort: 'createdAt,desc' }).subscribe({
      next: (data) => this.state.set({ kind: 'ok', data }),
      error: (err) => {
        console.error('Failed to load requests:', err);
        this.state.set({
          kind: 'error',
          message: err?.error?.detail ?? err?.message ?? 'Failed to load requests',
        });
      },
    });
  }

  /**
   * Helper for the template — readable status label.
   * "UNDER_REVIEW" -> "Under Review"
   */
  formatStatus(status: FoiaRequestStatus): string {
    return status
      .toLowerCase()
      .split('_')
      .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
      .join(' ');
  }

  /**
   * Helper for the template — used by the CSS to style terminal vs active.
   */
  isTerminal(status: FoiaRequestStatus): boolean {
    return TERMINAL_STATUSES.has(status);
  }
}