import { Injectable, signal, computed } from '@angular/core';

export type UserRole = 'REQUESTER' | 'STAFF' | 'ADMIN';

export interface UserPersona {
  id: number;
  email: string;
  fullName: string;
  role: UserRole;
}

/**
 * Available personas for the "View as" switcher.
 * Mock auth — Phase 6 replaces with real authentication.
 */
export const AVAILABLE_PERSONAS: UserPersona[] = [
  { id: 1, email: 'testuser@example.com',        fullName: 'Test Requester',  role: 'REQUESTER' },
  { id: 2, email: 'intake.officer@example.com',  fullName: 'Intake Officer',  role: 'STAFF' },
  { id: 3, email: 'case.officer@example.com',    fullName: 'Case Officer',    role: 'STAFF' },
];

const STORAGE_KEY = 'openrecords.persona.email';

@Injectable({ providedIn: 'root' })
export class UserContextService {
  // Default to first persona if nothing stored
  private readonly _current = signal<UserPersona>(this.loadInitial());

  /** Read-only signal of the active persona. */
  readonly current = this._current.asReadonly();

  /** Convenience computed signals for templates. */
  readonly isStaff = computed(() => this._current().role === 'STAFF' || this._current().role === 'ADMIN');
  readonly isRequester = computed(() => this._current().role === 'REQUESTER');

  switchTo(email: string): void {
    const persona = AVAILABLE_PERSONAS.find(p => p.email === email);
    if (!persona) {
      console.warn(`Unknown persona: ${email}`);
      return;
    }
    this._current.set(persona);
    localStorage.setItem(STORAGE_KEY, email);
  }

  private loadInitial(): UserPersona {
    const saved = localStorage.getItem(STORAGE_KEY);
    return AVAILABLE_PERSONAS.find(p => p.email === saved) ?? AVAILABLE_PERSONAS[0];
  }
}