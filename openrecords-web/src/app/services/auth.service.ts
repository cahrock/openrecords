import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap, throwError } from 'rxjs';
import { ApiService } from './api.service';
import {
  AuthResponse,
  AuthSession,
  AuthUser,
  LoginRequest,
  RegisterRequest,
  RegistrationResponse,
  UserRole,
  VerifyEmailResponse,
} from '../models/auth.model';

const STORAGE_KEY = 'openrecords.auth';

/**
 * Manages authentication state and tokens.
 *
 * Tokens are stored in localStorage (Phase 6 baseline). Phase 8 deployment
 * will upgrade to HttpOnly cookies for production.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);

  // Active session signal. Null = anonymous.
  private readonly _session = signal<AuthSession | null>(this.loadFromStorage());

  // Read-only public access
  readonly session = this._session.asReadonly();

  // Convenience computed signals
  readonly isAuthenticated = computed(() => this._session() !== null);
  readonly currentUser = computed<AuthUser | null>(() => this._session()?.user ?? null);
  readonly currentRole = computed<UserRole | null>(() => this._session()?.role ?? null);
  readonly isStaff = computed(() => {
    const role = this.currentRole();
    return role === 'STAFF' || role === 'ADMIN';
  });
  readonly isRequester = computed(() => this.currentRole() === 'REQUESTER');

  /**
   * Submit credentials. On success, stores tokens and updates session.
   */
  login(request: LoginRequest): Observable<AuthResponse> {
    return this.api.login(request).pipe(
      tap((response) => this.applySession(response))
    );
  }

  /**
   * Register a new account. Does NOT log in — user must verify email first.
   */
  register(request: RegisterRequest): Observable<RegistrationResponse> {
    return this.api.register(request);
  }

  /**
   * Verify email via one-time token from registration link.
   */
  verifyEmail(token: string): Observable<VerifyEmailResponse> {
    return this.api.verifyEmail(token);
  }

  /**
   * Clear session and tokens.
   */
  logout(): void {
    this._session.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  /**
   * Get the current access token for HTTP interceptor use.
   */
  getAccessToken(): string | null {
    return this._session()?.accessToken ?? null;
  }

  /**
   * Get the current refresh token (used by interceptor for token refresh).
   */
  getRefreshToken(): string | null {
    return this._session()?.refreshToken ?? null;
  }

  /**
   * Update tokens after a refresh (called by HTTP interceptor in 6-5e).
   */
  updateTokens(accessToken: string, refreshToken: string): void {
    const current = this._session();
    if (!current) return;
    const updated = { ...current, accessToken, refreshToken };
    this._session.set(updated);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
  }

  // ============================================================
  // Private helpers
  // ============================================================

  private applySession(response: AuthResponse): void {
    const session: AuthSession = {
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      user: response.user,
      role: response.role,
    };
    this._session.set(session);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  }

  private loadFromStorage(): AuthSession | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const parsed = JSON.parse(raw) as AuthSession;
      // Basic sanity check on the shape
      if (parsed?.accessToken && parsed?.user?.email && parsed?.role) {
        return parsed;
      }
      return null;
    } catch {
      // Corrupted storage — clear and start anonymous
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }
}