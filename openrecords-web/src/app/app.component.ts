import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ApiService, HealthResponse } from './services/api.service';
import { AuthService } from './services/auth.service';

type HealthState =
  | { kind: 'loading' }
  | { kind: 'ok'; data: HealthResponse }
  | { kind: 'error'; message: string };

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly elementRef = inject(ElementRef);
  private readonly router = inject(Router);

  // Public so the template can call auth methods directly
  readonly auth = inject(AuthService);

  readonly health = signal<HealthState>({ kind: 'loading' });
  readonly menuOpen = signal(false);

  ngOnInit(): void {
    this.api.getHealth().subscribe({
      next: (data) => this.health.set({ kind: 'ok', data }),
      error: (err) =>
        this.health.set({
          kind: 'error',
          message: err?.message ?? 'Unable to reach the API',
        }),
    });
  }

  toggleMenu(): void {
    this.menuOpen.update(v => !v);
  }

  signOut(): void {
    this.auth.logout();
    this.menuOpen.set(false);
    this.router.navigate(['/login']);
  }

  /** Close the menu when clicking outside it. */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.user-menu')) {
      this.menuOpen.set(false);
    }
  }

  /** Display first initial(s) of name for the avatar circle. */
  getInitials(): string {
    const name = this.auth.currentUser()?.fullName ?? '';
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map(p => p[0]?.toUpperCase() ?? '')
      .join('');
  }
}