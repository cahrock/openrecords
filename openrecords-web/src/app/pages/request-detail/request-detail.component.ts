import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal, } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { FoiaRequest, FoiaRequestStatus, TERMINAL_STATUSES, } from '../../models/foia-request.model';


type DetailState =
  | { kind: 'loading' }
  | { kind: 'ok'; data: FoiaRequest }
  | { kind: 'error'; message: string; status?: number };

@Component({
  selector: 'app-request-detail',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterLink],
  templateUrl: './request-detail.component.html',
  styleUrl: './request-detail.component.scss'
})
export class RequestDetailComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);

  readonly state = signal<DetailState>({ kind: 'loading' });

  ngOnInit(): void {
    // Read the :id parameter from the URL (/requests/:id)
    const id = this.route.snapshot.paramMap.get('id');

    if (!id) {
      this.state.set({ kind: 'error', message: 'No request ID provided' });
      return;
    }
    this.loadRequest(id);
  }

  loadRequest(id: string): void {
    this.state.set({ kind: 'loading' });
    this.api.getRequestById(id).subscribe({
      next: (data) => {
        this.state.set({ kind: 'ok', data });
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
