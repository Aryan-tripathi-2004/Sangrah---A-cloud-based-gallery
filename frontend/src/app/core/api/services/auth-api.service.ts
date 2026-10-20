import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { AuthResponse, LoginPayload, RegisterPayload, User } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/v1';
  private readonly currentUserSubject = new BehaviorSubject<User | null>(null);
  public readonly currentUser$ = this.currentUserSubject.asObservable();

  public getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  public setCurrentUser(user: User | null): void {
    this.currentUserSubject.next(user);
  }

  login(payload: LoginPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/login`, payload).pipe(
      tap(response => {
        if (typeof window !== 'undefined') {
          localStorage.setItem('token', response.token);
          if (response.refreshToken) {
            localStorage.setItem('refreshToken', response.refreshToken);
          }
        }
        this.currentUserSubject.next(response.user);
      })
    );
  }

  register(payload: RegisterPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/register`, payload).pipe(
      tap(response => {
        if (typeof window !== 'undefined') {
          localStorage.setItem('token', response.token);
          if (response.refreshToken) {
            localStorage.setItem('refreshToken', response.refreshToken);
          }
        }
        this.currentUserSubject.next(response.user);
      })
    );
  }

  logout(): void {
    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;

    if (token) {
      this.http.post(`${this.baseUrl}/auth/logout`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      }).subscribe({
        next: () => console.log('✅ [logout] Backend session revoked successfully'),
        error: (err) => console.warn('⚠️ [logout] Backend session revocation notice:', err)
      });
    }

    if (typeof window !== 'undefined') {
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
    }
    this.currentUserSubject.next(null);
  }

  refreshAccessToken(): Observable<AuthResponse> {
    const refreshToken = typeof window !== 'undefined' ? localStorage.getItem('refreshToken') : null;
    if (!refreshToken) {
      console.error('❌ No refresh token available in localStorage');
      return throwError(() => new Error('No refresh token available'));
    }

    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/token/refresh`, { refreshToken }).pipe(
      tap(response => {
        if (typeof window !== 'undefined') {
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
