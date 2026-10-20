import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser, DOCUMENT } from '@angular/common';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, throwError, of } from 'rxjs';
import { map, catchError, tap, shareReplay, finalize } from 'rxjs/operators';
import { SangrahApiService, AuthResponse } from '../api/sangrah-api.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(SangrahApiService);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly document = inject(DOCUMENT);

  private readonly isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
  public readonly isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  private readonly tokenSubject = new BehaviorSubject<string | null>(this.getStoredToken());
  public readonly token$ = this.tokenSubject.asObservable();

  private refreshInProgress$: Observable<string> | null = null;
  private refreshTimer: ReturnType<typeof setTimeout> | null = null;
  private listenersInitialized = false;

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  private hasToken(): boolean {
    if (!this.isBrowser()) {
      return false;
    }
    return !!localStorage.getItem('token');
  }

  private getStoredToken(): string | null {
    if (!this.isBrowser()) {
      return null;
    }
    return localStorage.getItem('token');
  }

  private getStoredRefreshToken(): string | null {
    if (!this.isBrowser()) {
      return null;
    }
    return localStorage.getItem('refreshToken');
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.api.login({ email, password }).pipe(
      tap((response) => {
        this.handleAuthSuccess(response.token, response.refreshToken);
      })
    );
  }

  register(email: string, password: string, displayName: string): Observable<AuthResponse> {
    return this.api.register({ email, password, displayName }).pipe(
      tap((response) => {
        this.handleAuthSuccess(response.token, response.refreshToken);
      })
    );
  }

  private handleAuthSuccess(token: string, refreshToken?: string): void {
    if (this.isBrowser()) {
      localStorage.setItem('token', token);
      if (refreshToken) {
        localStorage.setItem('refreshToken', refreshToken);
      }
    }
    this.tokenSubject.next(token);
    this.isAuthenticatedSubject.next(true);
    this.initVisibilityAndNetworkListeners();
    this.scheduleProactiveRefresh(token);
  }

  logout(): void {
    this.cancelProactiveRefresh();
    this.api.logout();
    if (this.isBrowser()) {
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
    }
    this.tokenSubject.next(null);
    this.isAuthenticatedSubject.next(false);
    this.router.navigate(['/login']);
  }

  isAuthenticated(): boolean {
    return this.hasToken();
  }

  getToken(): string | null {
    return this.getStoredToken();
  }

  /**
   * Restore auth state on application startup or page reload.
   */
  restoreAuthState(): void {
    const token = this.getStoredToken();
    const refreshToken = this.getStoredRefreshToken();

    if (token && refreshToken) {
      this.tokenSubject.next(token);
      this.isAuthenticatedSubject.next(true);
      this.initVisibilityAndNetworkListeners();

      const expiry = this.getTokenExpiryMillis(token);
      const now = Date.now();

      // If token has already expired or will expire within 2 minutes, refresh immediately
      if (expiry && (expiry - now < 120000)) {
        console.log('🔄 [AuthService] Stored token expired or near expiry on startup. Refreshing...');
        this.refreshToken().subscribe({
          error: (err) => console.warn('⚠️ [AuthService] Startup refresh notice:', err)
        });
      } else {
        this.scheduleProactiveRefresh(token);
      }
      console.log('🔄 [AuthService] Auth state restored from localStorage');
    } else {
      this.isAuthenticatedSubject.next(false);
    }
  }

  /**
   * Centralized, deduplicated refresh pipeline.
   * Concurrent 401s share the same in-flight HTTP request.
   */
  refreshToken(): Observable<string> {
    if (this.refreshInProgress$) {
      return this.refreshInProgress$;
    }

    const refreshToken = this.getStoredRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    this.refreshInProgress$ = this.api.refreshAccessToken().pipe(
      map((response) => {
        const newToken = response.token;
        if (newToken) {
          this.handleAuthSuccess(newToken, response.refreshToken);
        }
        return newToken;
      }),
      shareReplay(1),
      finalize(() => {
        this.refreshInProgress$ = null;
      })
    );

    return this.refreshInProgress$;
  }

  /**
   * Schedules a proactive refresh before the access token expires.
   */
  private scheduleProactiveRefresh(token: string): void {
    this.cancelProactiveRefresh();
    if (!this.isBrowser()) return;

    const expiry = this.getTokenExpiryMillis(token);
    if (!expiry) return;

    const now = Date.now();
    // Proactively refresh 90 seconds before expiration, minimum 5 second buffer
    const delayMs = Math.max(5000, expiry - now - 90000);

    console.log(`⏱️ [AuthService] Scheduled proactive token refresh in ${Math.round(delayMs / 1000)}s`);
    this.refreshTimer = setTimeout(() => {
      console.log('🔄 [AuthService] Proactive refresh timer fired');
      this.refreshToken().subscribe({
        next: () => console.log('✅ [AuthService] Proactive token refresh succeeded'),
        error: (err) => console.warn('⚠️ [AuthService] Proactive token refresh failed:', err)
      });
    }, delayMs);
  }

  private cancelProactiveRefresh(): void {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = null;
    }
  }

  /**
   * Refreshes token if needed when user returns to tab or network reconnects.
   */
  private initVisibilityAndNetworkListeners(): void {
    if (!this.isBrowser() || this.listenersInitialized) return;
    this.listenersInitialized = true;

    const windowRef = this.document.defaultView;
    if (!windowRef) return;

    const checkAndRefresh = () => {
      const token = this.getStoredToken();
      if (!token) return;
      const expiry = this.getTokenExpiryMillis(token);
      if (expiry && (expiry - Date.now() < 120000)) {
        console.log('🔄 [AuthService] Tab resumed / network reconnected with near-expired token. Refreshing...');
        this.refreshToken().subscribe({
          error: (err) => console.warn('⚠️ [AuthService] Resume refresh notice:', err)
        });
      }
    };

    windowRef.addEventListener('focus', checkAndRefresh);
    windowRef.addEventListener('online', checkAndRefresh);
  }

  private getTokenExpiryMillis(token: string): number | null {
    try {
      const parts = token.split('.');
      if (parts.length < 2) return null;
      const payload = parts[1];
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
      const decoded = JSON.parse(atob(padded));
      return typeof decoded.exp === 'number' ? decoded.exp * 1000 : null;
    } catch {
      return null;
    }
  }

  /**
   * Get current user ID by fetching profile from API
   */
  getCurrentUserId(): Observable<string> {
    return this.api.getProfile().pipe(
      map((profile) => profile.id || ''),
      catchError((error) => {
        console.error('Failed to get current user ID:', error);
        return throwError(() => error);
      })
    );
  }
}
