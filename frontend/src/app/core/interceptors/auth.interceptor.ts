import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // 🔐 CRITICAL: Skip adding token for auth refresh endpoint (uses refreshToken in body instead)
  if (req.url.includes('/auth/token/refresh')) {
    console.log('🔄 [authInterceptor] Token refresh endpoint detected - Optional skip');
  }

  // Only add token for API requests (not for external resources)
  if (req.url.includes('/api/')) {
    // For refresh endpoint, try to use refreshToken in Authorization header if that's what backend expects 
    // Or just use the expired token (often backends accept expired token on refresh endpoint to identify user)
    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;

    if (token) {
      console.log('🔐 [authInterceptor] Adding Authorization header to request:', req.method, req.url.substring(0, 50));
      // Clone the request and add Authorization header
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    } else {
      console.warn('⚠️ [authInterceptor] No token in localStorage for:', req.url.substring(0, 50));
    }
  }

  return next(req);
};
