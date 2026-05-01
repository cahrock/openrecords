import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Attaches the JWT access token to every /api request.
 *
 * On 401 responses, clears the session and redirects to /login.
 * Login and registration endpoints are exempt — they don't have a token yet.
 *
 * Phase 6-5e-bonus would add silent refresh-token rotation. For now, expired
 * tokens force a re-login.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  // Only handle /api requests; pass through everything else (assets, etc.)
  if (!req.url.startsWith('/api')) {
    return next(req);
  }

  // Don't attach a token to auth endpoints (login, register, verify)
  // — they're how you GET a token, so adding one is circular.
  const isAuthEndpoint = req.url.includes('/api/v1/auth/');

  let processedRequest = req;
  if (!isAuthEndpoint) {
    const token = auth.getAccessToken();
    if (token) {
      processedRequest = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` },
      });
    }
  }

  return next(processedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      // Only handle 401s on protected endpoints (not auth endpoints)
      if (error.status === 401 && !isAuthEndpoint) {
        // Token expired or invalid. Clear session and redirect.
        auth.logout();

        // Capture the current URL so we redirect back after login
        const currentUrl = router.url;
        const redirect =
          currentUrl !== '/login' ? { redirect: currentUrl } : undefined;

        router.navigate(['/login'], { queryParams: redirect });
      }

      return throwError(() => error);
    })
  );
};