import { Component, OnInit, OnDestroy, Output, EventEmitter, inject, Input, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SangrahApiService } from '../../core/api/sangrah-api.service';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-notification-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed right-6 top-20 w-96 max-h-[500px] bg-slate-900 border border-slate-700 rounded-xl shadow-2xl overflow-hidden z-40 flex flex-col">
      <!-- Header -->
      <div class="bg-gradient-to-r from-slate-800 to-slate-700 px-6 py-4 flex justify-between items-center border-b border-slate-700">
        <div class="flex items-center gap-3">
          <h3 class="font-bold text-lg text-white">Notifications</h3>
          <span *ngIf="unreadCount > 0" class="px-2.5 py-0.5 bg-red-600 rounded-full text-xs font-bold text-white">
            {{ unreadCount }}
          </span>
        </div>
        <button
          (click)="closePanel()"
          class="text-slate-400 hover:text-slate-200 text-xl transition">
          ✕
        </button>
      </div>

      <!-- Notifications List -->
      <div class="overflow-y-auto flex-1 bg-slate-900">
        <div *ngIf="isLoading && notifications.length === 0" class="p-6 text-center">
          <p class="text-slate-400 text-sm">Loading notifications...</p>
        </div>

        <div *ngIf="!isLoading && notifications.length === 0" class="p-8 text-center">
          <div class="text-4xl mb-3">🔔</div>
          <p class="text-slate-400 text-sm">No notifications yet</p>
        </div>

        <div *ngFor="let notif of notifications"
             [class.bg-blue-950/20]="!notif.read"
             class="px-6 py-4 border-b border-slate-800 hover:bg-slate-800/50 cursor-pointer transition"
             (click)="markAsRead(notif)">

          <!-- Notification Content -->
          <div class="flex items-start gap-3">
            <!-- Unread Indicator -->
            <div [class.bg-blue-500]="!notif.read"
                 [class.bg-slate-600]="notif.read"
                 class="w-2.5 h-2.5 rounded-full mt-1.5 flex-shrink-0 transition"></div>

            <!-- Message Content -->
            <div class="flex-1 min-w-0">
              <p class="text-sm font-semibold text-white" [title]="getTitle(notif.type)">
                {{ getTitle(notif.type) }}
              </p>
              <p class="text-xs text-slate-300 mt-1 line-clamp-2">
                {{ notif.payload?.message || 'Notification' }}
              </p>
              <p class="text-xs text-slate-500 mt-2">
                {{ formatTime(notif.createdAt) }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer with Actions -->
      <div class="bg-slate-800 border-t border-slate-700 px-6 py-3 flex gap-2">
        <button
          (click)="markAllRead()"
          *ngIf="unreadCount > 0"
          class="flex-1 text-xs text-center px-3 py-1.5 bg-blue-600/20 hover:bg-blue-600/30 text-blue-400 border border-blue-600/30 rounded transition font-medium">
          Mark all read
        </button>
        <button
          (click)="refreshNotifications()"
          class="flex-1 text-xs text-center px-3 py-1.5 bg-slate-700 hover:bg-slate-600 text-slate-300 rounded transition font-medium">
          Refresh
        </button>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }
  `]
})
export class NotificationPanelComponent implements OnInit, OnDestroy {
  private api = inject(SangrahApiService);
  private cdr = inject(ChangeDetectorRef);

  @Output() closeRequested = new EventEmitter<void>();
  @Input() triggerReload: boolean = false;

  notifications: any[] = [];
  unreadCount = 0;
  isLoading = true;
  private pollSubscription: Subscription | null = null;
  private pollInterval = 30000; // 30 seconds

  ngOnInit(): void {
    // Load immediately on init (with a small delay to ensure component is rendered)
    setTimeout(() => {
      console.log('🚀 [Notification Panel] ngOnInit triggered, loading notifications...');
      this.loadNotifications();
      this.startPolling();
    }, 10);
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  /**
   * Public method for parent to explicitly trigger reload
   */
  public ensureLoaded(): void {
    console.log('🔄 [Notification Panel] ensureLoaded() called from parent');
    if (!this.isLoading && this.notifications.length === 0) {
      this.loadNotifications();
    }
  }

  /**
   * Load notifications from API
   */
  loadNotifications(): void {
    this.isLoading = true;
    this.cdr.markForCheck();
    console.log('📋 [Notification Panel] Loading notifications... isLoading set to true');

    this.api.getNotifications(0, 50).subscribe({
      next: (response: any) => {
        console.log('✅ [Notification Panel] API Response received:', response);
        console.log('Response type:', typeof response);
        console.log('Response keys:', response ? Object.keys(response) : 'null');

        // Handle response structure
        if (response) {
          // Ensure notifications is an array
          const notificationsData = response.notifications;
          console.log('Notifications data:', notificationsData);
          console.log('Is array?', Array.isArray(notificationsData));

          this.notifications = Array.isArray(notificationsData) ? notificationsData : [];
          this.unreadCount = response.unreadCount || 0;

          console.log(`✅ [Notification Panel] Successfully set:
            - notifications: ${this.notifications.length} items
            - unreadCount: ${this.unreadCount}
            - isLoading: will be set to false`);
        } else {
          console.warn('⚠️ [Notification Panel] Empty/null response from API');
          this.notifications = [];
          this.unreadCount = 0;
        }

        // Critical: Set loading to false
        this.isLoading = false;
        console.log('🔴 [Notification Panel] isLoading set to FALSE');

        // Force change detection
        this.cdr.markForCheck();
        console.log('✅ [Notification Panel] Change detection triggered');
      },
      error: (err) => {
        console.error('❌ [Notification Panel] API Error:', err);
        console.error('Error status:', err?.status);
        console.error('Error message:', err?.message);
        console.error('Error response:', err?.error);

        this.isLoading = false;
        this.notifications = [];

        // Force change detection
        this.cdr.markForCheck();
      }
    });
  }

  /**
   * Mark single notification as read
   */
  markAsRead(notification: any): void {
    if (notification.read) return;

    this.api.markNotificationRead(notification.id).subscribe({
      next: () => {
        notification.read = true;
        this.unreadCount = Math.max(0, this.unreadCount - 1);
      },
      error: (err) => console.error('❌ Failed to mark as read:', err)
    });
  }

  /**
   * Mark all notifications as read
   */
  markAllRead(): void {
    this.api.markAllNotificationsRead().subscribe({
      next: () => {
        this.notifications.forEach(n => n.read = true);
        this.unreadCount = 0;
      },
      error: (err) => console.error('❌ Failed to mark all as read:', err)
    });
  }

  /**
   * Refresh notifications immediately
   */
  refreshNotifications(): void {
    this.loadNotifications();
  }

  /**
   * Close panel
   */
  closePanel(): void {
    this.closeRequested.emit();
  }

  /**
   * Start polling for new notifications
   */
  private startPolling(): void {
    this.pollSubscription = interval(this.pollInterval).subscribe(() => {
      this.loadNotifications();
    });
  }

  /**
   * Stop polling
   */
  private stopPolling(): void {
    if (this.pollSubscription) {
      this.pollSubscription.unsubscribe();
    }
  }

  /**
   * Get readable title for notification type
   */
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

  /**
   * Format timestamp to relative time
   */
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
