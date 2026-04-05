import { inject } from '@angular/core';
import { AuthService } from './auth.service';

/**
 * App initializer that restores auth state from localStorage on app startup.
 * This ensures that on page refresh, if the user has a token, we restore their state
 * and the auth guard can check that the user is still authenticated.
 */
export function initializeAuth() {
  return () => {
    const authService = inject(AuthService);

    // Restore auth state from localStorage
    // This is crucial for handling page refreshes on protected routes
    authService.restoreAuthState();

    return Promise.resolve();
  };
}
