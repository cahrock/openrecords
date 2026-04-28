import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, inject, signal } from '@angular/core';
import { AVAILABLE_PERSONAS, UserContextService } from '../../services/user-context.service';

@Component({
  selector: 'app-persona-switcher',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './persona-switcher.component.html',
  styleUrl: './persona-switcher.component.scss',
})
export class PersonaSwitcherComponent {
  private readonly userContext = inject(UserContextService);
  private readonly elementRef = inject(ElementRef);

  readonly personas = AVAILABLE_PERSONAS;
  readonly current = this.userContext.current;
  readonly isOpen = signal(false);

  toggle(): void {
    this.isOpen.update(v => !v);
  }

  select(email: string): void {
    this.userContext.switchTo(email);
    this.isOpen.set(false);
    // Reload so all in-flight UI re-fetches with the new persona's header
    window.location.reload();
  }

  /** Close the dropdown when clicking outside it. */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.isOpen.set(false);
    }
  }
}