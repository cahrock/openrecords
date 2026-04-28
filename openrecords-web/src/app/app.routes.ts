import { Routes } from '@angular/router';

/**
 * Top-level application routes.
 *
 * Children use lazy loading (loadComponent) so each page ships in its own
 * JavaScript chunk. Users only download the code for the page they visit.
 * This is a meaningful performance improvement as the app grows.
 */
export const routes: Routes = [
  {
    path: '',
    redirectTo: '/requests',
    pathMatch: 'full',
  },
  {
    path: 'requests',
    loadComponent: () =>
      import('./pages/request-list/request-list.component').then(
        (m) => m.RequestListComponent
      ),
    title: 'My Requests — OpenRecords',
  },
  {
    path: 'requests/new',
    loadComponent: () =>
      import('./pages/request-form/request-form.component').then(
        (m) => m.RequestFormComponent
      ),
    title: 'New Request — OpenRecords',
  },
  {
    path: 'requests/:id',
    loadComponent: () =>
      import('./pages/request-detail/request-detail.component').then(
        (m) => m.RequestDetailComponent
      ),
    title: 'Request Detail — OpenRecords',
  },
  {
    path: 'staff/queue',
    loadComponent: () =>
      import('./pages/staff-queue/staff-queue.component').then(
        (m) => m.StaffQueueComponent
      ),
    title: 'Staff Queue — OpenRecords',
  },
  {
    path: '**',
    loadComponent: () =>
      import('./pages/not-found/not-found.component').then(
        (m) => m.NotFoundComponent
      ),
    title: 'Page Not Found — OpenRecords',
  },
];