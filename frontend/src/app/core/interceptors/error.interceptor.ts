import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { NormalizedApiError } from '../errors/api-error.model';
import { catchError, throwError } from 'rxjs';
import { switchMap } from 'rxjs/operators';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Handle 401 Unauthorized
      if (error.status === 401) {
        // If 401 occurred on the login or refresh endpoint itself, session is dead -> log out immediately
        if (req.url.includes('/auth/token/refresh') || req.url.includes('/auth/login')) {
          console.error('❌ [errorInterceptor] 401 received on auth endpoint - session terminated');
          authService.logout();
          return throwError(() => new NormalizedApiError(error));
        }

        // Delegate to AuthService's centralized, deduplicated refresh pipeline
        return authService.refreshToken().pipe(
          switchMap((newToken: string) => {
            // Retry the original request with the freshly acquired access token
            const retryReq = req.clone({
              setHeaders: {
                Authorization: `Bearer ${newToken}`
              }
            });
            return next(retryReq);
          }),
          catchError((refreshErr) => {
            console.error('❌ [errorInterceptor] Token refresh failed:', refreshErr);
            authService.logout();
            return throwError(() => new NormalizedApiError(new Error('Session expired. Please log in again.')));
          })
        );
      }

      // Propagate all standard backend custom exceptions normalized into RFC-9457 Problem Details
      const normalizedError = new NormalizedApiError(error);
      return throwError(() => normalizedError);
    })
  );
};
