import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { SangrahApiService } from '../api/sangrah-api.service';
import { catchError, filter, switchMap, take, throwError, BehaviorSubject } from 'rxjs';

// Track if we're already refreshing to prevent infinite loops
let isRefreshing = false;
const refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const api = inject(SangrahApiService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Handle 401 Unauthorized - attempt to refresh token
      if (error.status === 401) {
        
        // CRITICAL: If the 401 is from the refresh endpoint itself, we are fully logged out.
        // Don't try to intercept it again, just fail immediately.
        if (req.url.includes('/auth/token/refresh')) {
          console.error('❌ Refresh token 401 - Session dead');
          isRefreshing = false;
          refreshTokenSubject.next('REFRESH_FAILED');
          if (typeof window !== 'undefined') {
            localStorage.removeItem('token');
            localStorage.removeItem('refreshToken');
          }
          router.navigate(['/login']);
          return throwError(() => error);
        }

        if (!isRefreshing) {
          isRefreshing = true;
          refreshTokenSubject.next(null);
          console.log('🔄 Token expired (401), attempting to refresh...');
          console.log('📋 Refresh token in storage:', !!localStorage.getItem('refreshToken'));

          return api.refreshAccessToken().pipe(
            switchMap((response) => {
              isRefreshing = false;
              console.log('✅ Token refreshed, retrying original request');
              refreshTokenSubject.next(response.token);

              // Get the new token from localStorage and update the request
              const newToken = typeof window !== 'undefined' ? localStorage.getItem('token') : null;

              if (newToken) {
                // Clone request and update Authorization header with new token
                const retryReq = req.clone({
                  setHeaders: {
                    Authorization: `Bearer ${newToken}`
                  }
                });
                console.log('🔄 Retrying request with new token');
                return next(retryReq);
              } else {
                console.error('❌ No token after refresh');
                return throwError(() => new Error('Token refresh failed'));
              }
            }),
            catchError((err) => {
              isRefreshing = false;
              console.error('❌ Token refresh failed:', err);
              // Refresh failed - clear tokens and redirect to login
              if (typeof window !== 'undefined') {
                localStorage.removeItem('token');
                localStorage.removeItem('refreshToken');
              }
              // Unblock queued requests by emitting a failure indicator
              refreshTokenSubject.next('REFRESH_FAILED');
              
              router.navigate(['/login']);
              return throwError(() => new Error('Session expired. Please log in again.'));
            })
          );
        } else {
          // If already refreshing, wait for the subject to emit a new token
          return refreshTokenSubject.pipe(
            filter(token => token != null),
            take(1),
            switchMap(token => {
              if (token === 'REFRESH_FAILED') {
                return throwError(() => new Error('Session expired. Please log in again.'));
              }
              return next(req.clone({
                setHeaders: {
                  Authorization: `Bearer ${token}`
                }
              }));
            }),
            catchError(err => {
              // If the subject errors out (e.g. refresh failed), propagate that error
              return throwError(() => err);
            })
          );
        }
      }

      // For other errors, just pass them through
      return throwError(() => error);
    })
  );
};
