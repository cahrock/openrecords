import { inject } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  Router,
  RouterStateSnapshot,
} from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Guard that requires the user to be authenticated.
 *
 * If anonymous, redirects to /login with a `redirect` query param so
 * post-login navigation returns to the originally-requested URL.
 */
export const authGuard: CanActivateFn = (
  _route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }

  // Redirect to login, preserving the requested URL
  router.navigate(['/login'], {
    queryParams: { redirect: state.url },
  });
  return false;
};

/**
 * Guard that requires the user to have STAFF or ADMIN role.
 *
 * If anonymous, redirects to /login (with redirect preservation).
 * If authenticated as REQUESTER, redirects to /requests.
 */
export const staffGuard: CanActivateFn = (
  _route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    router.navigate(['/login'], {
      queryParams: { redirect: state.url },
    });
    return false;
  }

  if (auth.isStaff()) {
    return true;
  }

  // Authenticated REQUESTER trying to access staff route → bounce to home
  router.navigate(['/requests']);
  return false;
};