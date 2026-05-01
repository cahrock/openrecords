import { Routes } from '@angular/router';
import { authGuard, staffGuard } from './services/auth.guard';

export const routes: Routes = [
  // ============================================================
  // Public routes (no auth required)
  // ============================================================
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login.component').then(m => m.LoginComponent),
    title: 'Sign in — OpenRecords',
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./pages/register/register.component').then(m => m.RegisterComponent),
    title: 'Create account — OpenRecords',
  },
  {
    path: 'check-email',
    loadComponent: () =>
      import('./pages/check-email/check-email.component').then(m => m.CheckEmailComponent),
    title: 'Check your email — OpenRecords',
  },
  {
    path: 'verify',
    loadComponent: () =>
      import('./pages/verify/verify.component').then(m => m.VerifyComponent),
    title: 'Verify your email — OpenRecords',
  },

  // ============================================================
  // Authenticated routes (any logged-in user)
  // ============================================================
  {
    path: 'requests',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/request-list/request-list.component').then(m => m.RequestListComponent),
    title: 'My Requests — OpenRecords',
  },
  {
    path: 'requests/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/request-form/request-form.component').then(m => m.RequestFormComponent),
    title: 'New FOIA Request — OpenRecords',
  },
  {
    path: 'requests/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/request-detail/request-detail.component').then(m => m.RequestDetailComponent),
    title: 'Request Detail — OpenRecords',
  },

  // ============================================================
  // Staff-only routes (STAFF or ADMIN role required)
  // ============================================================
  {
    path: 'staff/queue',
    canActivate: [staffGuard],
    loadComponent: () =>
      import('./pages/staff-queue/staff-queue.component').then(m => m.StaffQueueComponent),
    title: 'Staff Queue — OpenRecords',
  },

  // ============================================================
  // Redirects + 404
  // ============================================================
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'requests',
  },
  {
    path: '**',
    loadComponent: () =>
      import('./pages/not-found/not-found.component').then(m => m.NotFoundComponent),
    title: 'Not Found — OpenRecords',
  },
];