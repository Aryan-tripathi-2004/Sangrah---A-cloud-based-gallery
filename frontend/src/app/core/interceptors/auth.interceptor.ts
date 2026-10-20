import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Skip adding token for public authentication endpoints
  if (req.url.includes('/auth/token/refresh') ||
      req.url.includes('/auth/login') ||
      req.url.includes('/auth/register')) {
    return next(req);
  }

  // Add Authorization header for protected API requests
  if (req.url.includes('/api/')) {
    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;

    if (token) {
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }
  }

  return next(req);
};
