import { Component, OnInit, OnDestroy, Output, EventEmitter, inject, Input, ChangeDetectionStrategy, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SangrahApiService, Notification } from '../api/sangrah-api.service';
import { NotificationService } from './notification.service';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-notification-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  templateUrl: './notification-panel.component.html',
})
export class NotificationPanelComponent implements OnInit, OnDestroy {
  private readonly api = inject(SangrahApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  @Output() closeRequested = new EventEmitter<void>();
  @Input() triggerReload: boolean = false;

  readonly notifications = signal<Notification[]>([]);
  readonly unreadCount = signal<number>(0);
  readonly isLoading = signal<boolean>(true);

  private pollSubscription: Subscription | null = null;
  private readonly pollInterval = 30000; // 30 seconds

  ngOnInit(): void {
    setTimeout(() => {
      this.loadNotifications();
      this.startPolling();
    }, 10);
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  public ensureLoaded(): void {
    if (!this.isLoading() && this.notifications().length === 0) {
      this.loadNotifications();
    }
  }

  loadNotifications(): void {
    this.isLoading.set(true);

    this.api.getNotifications(0, 50).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (response) {
          const notificationsData = response.notifications;
          const items = Array.isArray(notificationsData) ? notificationsData : [];
          const unread = response.unreadCount || 0;
          this.notifications.set(items);
          this.unreadCount.set(unread);
          this.notificationService.setUnreadCount(unread);
        } else {
          this.notifications.set([]);
          this.unreadCount.set(0);
          this.notificationService.setUnreadCount(0);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('❌ [Notification Panel] API Error:', err);
        this.isLoading.set(false);
        this.notifications.set([]);
      }
    });
  }

  markAsRead(notification: Notification): void {
    if (notification.read) return;

    this.api.markNotificationRead(notification.id).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => {
        this.notifications.update(items =>
          items.map(n => n.id === notification.id ? { ...n, read: true } : n)
        );
        const newUnread = Math.max(0, this.unreadCount() - 1);
        this.unreadCount.set(newUnread);
        this.notificationService.decrementUnreadCount(1);
      },
      error: (err) => console.error('❌ Failed to mark as read:', err)
    });
  }

  markAllRead(): void {
    this.api.markAllNotificationsRead().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => {
        this.notifications.update(items => items.map(n => ({ ...n, read: true })));
        this.unreadCount.set(0);
        this.notificationService.setUnreadCount(0);
      },
      error: (err) => console.error('❌ Failed to mark all as read:', err)
    });
  }

  refreshNotifications(): void {
    this.loadNotifications();
  }

  closePanel(): void {
    this.closeRequested.emit();
  }

  private startPolling(): void {
    if (typeof window === 'undefined') return;
    this.stopPolling();
    this.pollSubscription = interval(this.pollInterval).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.loadNotifications();
    });
  }

  private stopPolling(): void {
    if (this.pollSubscription) {
      this.pollSubscription.unsubscribe();
      this.pollSubscription = null;
    }
  }

  getTitle(type: string): string {
    const titles: { [key: string]: string } = {
      'COLLABORATOR_ADDED': '👥 Added as Collaborator',
      'ACCESS_REQUEST_RECEIVED': '📩 Access Request',
      'ACCESS_REQUEST_APPROVED': '✅ Access Approved',
      'MEDIA_APPROVED': '✓ Media Approved',
      'MEDIA_REJECTED': '✗ Media Rejected',
      'EVENT_UPDATED': '📝 Event Updated',
      'COLLABORATOR_REMOVED': '👤 Removed from Event'
    };
    return titles[type] || type;
  }

  formatTime(createdAt: any): string {
    if (!createdAt) return 'Just now';

    const date = new Date(createdAt);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;

    return date.toLocaleDateString();
  }
}
