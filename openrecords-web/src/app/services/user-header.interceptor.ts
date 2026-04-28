import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { UserContextService } from './user-context.service';

/**
 * Adds X-User-Email header to /api requests so the backend knows
 * which persona to attribute the action to.
 */
export const userHeaderInterceptor: HttpInterceptorFn = (req, next) => {
  // Only attach to our API, not to third-party URLs
  if (!req.url.startsWith('/api')) {
    return next(req);
  }

  const userContext = inject(UserContextService);
  const email = userContext.current().email;

  const cloned = req.clone({
    setHeaders: { 'X-User-Email': email },
  });

  return next(cloned);
};