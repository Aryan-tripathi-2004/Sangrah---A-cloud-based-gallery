import { HttpClient } from '@angular/common/http';
import { Injectable, inject, PLATFORM_ID, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { isPlatformBrowser } from '@angular/common';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { AuthResponse, LoginPayload, RegisterPayload, User } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/v1';
  private readonly platformId = inject(PLATFORM_ID);

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }
  private readonly currentUserSignal = signal<User | null>(null);
  public readonly currentUser = this.currentUserSignal.asReadonly();
  public readonly currentUser$ = toObservable(this.currentUserSignal);

  public getCurrentUser(): User | null {
    return this.currentUserSignal();
  }

  public setCurrentUser(user: User | null): void {
    this.currentUserSignal.set(user);
  }

  login(payload: LoginPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/login`, payload).pipe(
      tap(response => {
        if (this.isBrowser()) {
          localStorage.setItem('token', response.token);
          if (response.refreshToken) {
            localStorage.setItem('refreshToken', response.refreshToken);
          }
        }
        this.currentUserSignal.set(response.user);
      })
    );
  }

  register(payload: RegisterPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/register`, payload).pipe(
      tap(response => {
        if (this.isBrowser()) {
          localStorage.setItem('token', response.token);
          if (response.refreshToken) {
            localStorage.setItem('refreshToken', response.refreshToken);
          }
        }
        this.currentUserSignal.set(response.user);
      })
    );
  }

  logout(): void {
    const token = this.isBrowser() ? localStorage.getItem('token') : null;

    if (token) {
      this.http.post(`${this.baseUrl}/auth/logout`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      }).subscribe({
        next: () => console.log('✅ [logout] Backend session revoked successfully'),
        error: (err) => console.warn('⚠️ [logout] Backend session revocation notice:', err)
      });
    }

    if (this.isBrowser()) {
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
    }
    this.currentUserSignal.set(null);
  }

  refreshAccessToken(): Observable<AuthResponse> {
    const refreshToken = this.isBrowser() ? localStorage.getItem('refreshToken') : null;
    if (!refreshToken) {
      console.error('❌ No refresh token available in localStorage');
      return throwError(() => new Error('No refresh token available'));
    }

    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/token/refresh`, { refreshToken }).pipe(
      tap(response => {
        if (this.isBrowser()) {
          localStorage.setItem('token', response.token);
          if (response.refreshToken) {
            localStorage.setItem('refreshToken', response.refreshToken);
          }
        }
      }),
      catchError((err) => {
        console.error('❌ [refreshAccessToken] HTTP error:', err);
        return throwError(() => err);
      })
    );
  }
}
