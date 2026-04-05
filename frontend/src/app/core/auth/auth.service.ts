import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { SangrahApiService } from '../api/sangrah-api.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(SangrahApiService);
  private readonly router = inject(Router);
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  private hasToken(): boolean {
    // Check if we're in a browser environment (SSR safe)
    if (typeof window === 'undefined') {
      return false;
    }
    return !!localStorage.getItem('token');
  }

  login(email: string, password: string): Observable<any> {
    return new Observable(observer => {
      this.api.login({ email, password }).subscribe({
        next: (response) => {
          this.isAuthenticatedSubject.next(true);
          observer.next(response);
          observer.complete();
        },
        error: (error) => observer.error(error),
      });
    });
  }

  register(email: string, password: string, displayName: string): Observable<any> {
    return new Observable(observer => {
      this.api.register({ email, password, displayName }).subscribe({
        next: (response) => {
          this.isAuthenticatedSubject.next(true);
          observer.next(response);
          observer.complete();
        },
        error: (error) => observer.error(error),
      });
    });
  }

  logout(): void {
    // Safe to call on both server and browser
    if (typeof window !== 'undefined') {
      localStorage.removeItem('token');
    }
    this.api.logout();
    this.isAuthenticatedSubject.next(false);
    this.router.navigate(['/login']);
  }

  isAuthenticated(): boolean {
    return this.hasToken();
  }

  getToken(): string | null {
    // Check if we're in a browser environment (SSR safe)
    if (typeof window === 'undefined') {
      return null;
    }
    return localStorage.getItem('token');
  }

  /**
   * Restore auth state from localStorage.
   * Called on app initialization to ensure auth state is maintained on page refresh.
   */
  restoreAuthState(): void {
    const hasToken = this.hasToken();
    this.isAuthenticatedSubject.next(hasToken);
    if (hasToken) {
      console.log('🔄 [AuthService] Auth state restored from localStorage');
    }
  }
}
