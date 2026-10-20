import { Injectable, inject, PLATFORM_ID, NgZone, OnDestroy, signal, computed } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { toObservable } from '@angular/core/rxjs-interop';
import { Observable, Subscription, interval, of } from 'rxjs';
import { distinctUntilChanged, catchError, map } from 'rxjs/operators';
import { SangrahApiService } from '../api/sangrah-api.service';
import { AuthService } from '../auth/auth.service';

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {
  private readonly api = inject(SangrahApiService);
  private readonly authService = inject(AuthService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly ngZone = inject(NgZone);

  // Modern Angular Signals state
  public readonly unreadCount = signal<number>(0);
  public readonly hasUnread = computed(() => this.unreadCount() > 0);
  public readonly unreadCount$: Observable<number> = toObservable(this.unreadCount);

  private pollSubscription: Subscription | null = null;
  private authSubscription: Subscription | null = null;

  private isPolling = false;
  private isFetching = false;
  private lastFetchTimestamp = 0;

  private readonly POLL_INTERVAL_MS = 30000; // 30 seconds
  private readonly MIN_FETCH_GAP_MS = 5000;   // Throttle: at least 5s between explicit network fetches

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
   * Start polling for unread notifications.
   * Managed centrally so only ONE polling interval exists across the entire application.
   */
  public startPolling(): void {
    if (this.isPolling || !isPlatformBrowser(this.platformId)) {
      return;
    }

    this.isPolling = true;

    // Fetch immediately on authentication / activation
    this.refreshUnreadCount(true);

    // Schedule interval outside Angular Zone to avoid triggering unnecessary change detection ticks
    this.ngZone.runOutsideAngular(() => {
      this.pollSubscription = interval(this.POLL_INTERVAL_MS).subscribe(() => {
        this.ngZone.run(() => {
          this.refreshUnreadCount(false);
        });
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
   * Refresh unread notification count from backend.
   * Includes in-flight deduplication and throttling to prevent flood loops.
   */
  public refreshUnreadCount(force = false): void {
    if (!isPlatformBrowser(this.platformId) || !this.authService.isAuthenticated()) {
      return;
    }

    const now = Date.now();
    if (!force && (now - this.lastFetchTimestamp < this.MIN_FETCH_GAP_MS)) {
      return;
    }

    if (this.isFetching) {
      return;
    }

    this.isFetching = true;

    // Prefer dedicated unread-count endpoint; gracefully fallback to getNotifications(0, 1)
    this.fetchCountObservable().subscribe({
      next: (count: number) => {
        this.lastFetchTimestamp = Date.now();
        this.isFetching = false;
        this.unreadCount.set(count);
      },
      error: (err) => {
        console.warn('⚠️ [NotificationService] Failed to fetch unread notification count:', err);
        this.isFetching = false;
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
