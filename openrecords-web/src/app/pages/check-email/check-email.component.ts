import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-check-email',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './check-email.component.html',
  styleUrl: './check-email.component.scss',
})
export class CheckEmailComponent {
  private readonly route = inject(ActivatedRoute);

  readonly email = this.route.snapshot.queryParamMap.get('email') ?? '';
}