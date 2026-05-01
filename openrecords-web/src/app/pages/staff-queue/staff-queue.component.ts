import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, PageQuery } from '../../services/api.service';
import {
  FoiaRequest,
  FoiaRequestStatus,
  PageResponse,
  TERMINAL_STATUSES,
} from '../../models/foia-request.model';
import { AuthService } from '../../services/auth.service';

type QueueState =
  | { kind: 'loading' }
  | { kind: 'ok'; data: PageResponse<FoiaRequest> }
  | { kind: 'error'; message: string };

interface QueueFilters {
  status: FoiaRequestStatus | '';
  assignment: 'all' | 'mine' | 'unassigned';
  search: string;
}

const ALL_STATUSES: FoiaRequestStatus[] = [
  'DRAFT', 'SUBMITTED', 'ACKNOWLEDGED', 'ASSIGNED', 'UNDER_REVIEW',
  'ON_HOLD', 'RESPONSIVE_RECORDS_FOUND', 'NO_RECORDS', 'DOCUMENTS_RELEASED',
  'REJECTED', 'CLOSED',
];

@Component({
  selector: 'app-staff-queue',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DatePipe],
  templateUrl: './staff-queue.component.html',
  styleUrl: './staff-queue.component.scss',
})
export class StaffQueueComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);

  readonly state = signal<QueueState>({ kind: 'loading' });
  readonly filters = signal<QueueFilters>({
    status: 'SUBMITTED',
    assignment: 'all',
    search: '',
  });

  readonly allStatuses = ALL_STATUSES;
  readonly currentUser = this.auth.currentUser;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.state.set({ kind: 'loading' });

    const f = this.filters();
    const query: PageQuery & {
      status?: string;
      assigneeId?: number;
      unassignedOnly?: boolean;
      search?: string;
    } = {
      size: 50,
      sort: 'createdAt,desc',
    };

    if (f.status) query.status = f.status;
    if (f.assignment === 'mine') {
      const userId = this.currentUser()?.id;
      if (userId) query.assigneeId = userId;
    }
    if (f.assignment === 'unassigned') query.unassignedOnly = true;
    if (f.search.trim()) query.search = f.search.trim();

    this.api.listRequests(query).subscribe({
      next: (data) => this.state.set({ kind: 'ok', data }),
      error: (err) => {
        console.error('Queue load failed:', err);
        this.state.set({
          kind: 'error',
          message: err?.error?.detail ?? err?.message ?? 'Failed to load queue',
        });
      },
    });
  }

  updateFilter<K extends keyof QueueFilters>(key: K, value: QueueFilters[K]): void {
    this.filters.update(f => ({ ...f, [key]: value }));
    this.load();
  }

  clearFilters(): void {
    this.filters.set({ status: '', assignment: 'all', search: '' });
    this.load();
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

  isOverdue(req: FoiaRequest): boolean {
    if (!req.dueDate || this.isTerminal(req.status)) return false;
    return new Date(req.dueDate) < new Date();
  }
}