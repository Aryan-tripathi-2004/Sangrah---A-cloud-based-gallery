import { Injectable, inject, PLATFORM_ID, NgZone, OnDestroy, signal, computed } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Observable, Subscription, timer, of } from 'rxjs';
import { distinctUntilChanged, catchError, map, switchMap, filter } from 'rxjs/operators';
import { NotificationApiService } from '../api/services/notification-api.service';
import { AuthService } from '../auth/auth.service';

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {
  private readonly api = inject(NotificationApiService);
  private readonly authService = inject(AuthService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly ngZone = inject(NgZone);

  // Modern Angular Signals state
  public readonly unreadCount = signal<number>(0);
  public readonly hasUnread = computed(() => this.unreadCount() > 0);

  private pollSubscription: Subscription | null = null;
  private authSubscription: Subscription | null = null;

  private isPolling = false;

  private readonly POLL_INTERVAL_MS = 30000; // 30 seconds

  constructor() {
    this.initAuthTracking();
  }

  private initAuthTracking(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.authSubscription = this.authService.isAuthenticated$
      .pipe(distinctUntilChanged())
      .subscribe((isAuth: boolean) => {
        if (isAuth) {
          this.startPolling();
        } else {
          this.stopPolling();
          this.unreadCount.set(0);
        }
      });
  }

  /**
   * Start polling for unread notifications using a single declarative stream.
   * Managed centrally so only ONE polling interval exists across the entire application.
   */
  public startPolling(): void {
    if (this.isPolling || !isPlatformBrowser(this.platformId)) {
      return;
    }

    this.isPolling = true;

    // Schedule declarative pipeline outside Angular Zone to prevent change detection thrashing
    this.ngZone.runOutsideAngular(() => {
      this.pollSubscription = timer(0, this.POLL_INTERVAL_MS).pipe(
        filter(() => this.authService.isAuthenticated()),
        switchMap(() => this.fetchCountObservable())
      ).subscribe({
        next: (count: number) => {
          this.ngZone.run(() => this.unreadCount.set(count));
        },
        error: (err) => console.warn('⚠️ [NotificationService] Polling failed', err)
      });
    });
  }

  /**
   * Stop polling (e.g. on user logout).
   */
  public stopPolling(): void {
    this.isPolling = false;
    if (this.pollSubscription) {
      this.pollSubscription.unsubscribe();
      this.pollSubscription = null;
    }
  }

  /**
   * Refresh unread notification count from backend explicitly.
   */
  public refreshUnreadCount(force = false): void {
    if (!isPlatformBrowser(this.platformId) || !this.authService.isAuthenticated()) {
      return;
    }

    this.fetchCountObservable().subscribe({
      next: (count: number) => {
        this.unreadCount.set(count);
      },
      error: (err) => {
        console.warn('⚠️ [NotificationService] Failed to fetch unread notification count:', err);
      }
    });
  }

  private fetchCountObservable(): Observable<number> {
    return this.api.getUnreadNotificationCount().pipe(
      map(res => (res && typeof res.unreadCount === 'number') ? res.unreadCount : 0),
      catchError(() => {
        // Fallback to getNotifications(0, 1) if unread-count endpoint is unavailable
        return this.api.getNotifications(0, 1).pipe(
          map(res => (res && typeof res.unreadCount === 'number') ? res.unreadCount : 0),
          catchError(() => of(this.unreadCount()))
        );
      })
    );
  }

  /**
   * Update the unread count directly (e.g. when panel loads notifications or marks all as read).
   */
  public setUnreadCount(count: number): void {
    this.unreadCount.set(Math.max(0, count));
  }

  /**
   * Decrement unread count by a given amount (e.g. when an individual notification is marked as read).
   */
  public decrementUnreadCount(amount = 1): void {
    this.unreadCount.update(c => Math.max(0, c - amount));
  }

  ngOnDestroy(): void {
    this.stopPolling();
    if (this.authSubscription) {
      this.authSubscription.unsubscribe();
      this.authSubscription = null;
    }
  }
}
