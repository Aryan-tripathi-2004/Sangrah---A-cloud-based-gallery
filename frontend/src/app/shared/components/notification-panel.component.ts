import {
  Component,
  OnInit,
  output,
  inject,
  input,
  ChangeDetectionStrategy,
  signal,
  DestroyRef,
  PLATFORM_ID,
  NgZone,
  Pipe,
  PipeTransform
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NotificationApiService } from '../../core/api/services/notification-api.service';
import { Notification } from '../../core/api/models/api.models';
import { NotificationService } from '../../core/notifications/notification.service';

export const NOTIFICATION_TITLES: Record<string, string> = {
  'COLLABORATOR_ADDED': '👥 Added as Collaborator',
  'ACCESS_REQUEST_RECEIVED': '📩 Access Request',
  'ACCESS_REQUEST_APPROVED': '✅ Access Approved',
  'MEDIA_APPROVED': '✓ Media Approved',
  'MEDIA_REJECTED': '✗ Media Rejected',
  'EVENT_UPDATED': '📝 Event Updated',
  'COLLABORATOR_REMOVED': '👤 Removed from Event'
};

@Pipe({
  name: 'notificationTitle',
  standalone: true,
  pure: true
})
export class NotificationTitlePipe implements PipeTransform {
  transform(type: string): string {
    return NOTIFICATION_TITLES[type] || type;
  }
}

@Component({
  selector: 'app-notification-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NotificationTitlePipe],
  templateUrl: './notification-panel.component.html',
})
export class NotificationPanelComponent implements OnInit {
  private readonly api = inject(NotificationApiService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly ngZone = inject(NgZone);

  readonly closeRequested = output<void>();
  readonly triggerReload = input<boolean>(false);

  readonly notifications = signal<Notification[]>([]);
  readonly unreadCount = signal<number>(0);
  readonly isLoading = signal<boolean>(true);

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.loadNotifications();
    }
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
        this.ngZone.run(() => {
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
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          console.error('❌ [Notification Panel] API Error:', err);
          this.isLoading.set(false);
          this.notifications.set([]);
        });
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

  getTitle(type: string): string {
    return NOTIFICATION_TITLES[type] || type;
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
