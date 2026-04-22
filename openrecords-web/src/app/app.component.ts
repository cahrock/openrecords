import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ApiService, HealthResponse } from './services/api.service';

type HealthState =
  | { kind: 'loading' }
  | { kind: 'ok'; data: HealthResponse }
  | { kind: 'error'; message: string };

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  private readonly api = inject(ApiService);

  readonly health = signal<HealthState>({ kind: 'loading' });

  ngOnInit(): void {
    this.api.getHealth().subscribe({
      next: (data) => this.health.set({ kind: 'ok', data }),
      error: (err) => this.health.set({
        kind: 'error',
        message: err?.message ?? 'Unable to reach the API',
      }),
    });
  }
}