import { Component, OnInit, OnDestroy, inject, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { SangrahApiService } from '../../core/api/sangrah-api.service';
import { NotificationPanelComponent } from '../../features/notifications/notification-panel.component';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, NotificationPanelComponent],
  templateUrl: './layout.component.html',
})
export class LayoutComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private apiService = inject(SangrahApiService);
  private router = inject(Router);

  @ViewChild(NotificationPanelComponent) notificationPanel: NotificationPanelComponent | undefined;

  isAuthenticated$ = this.authService.isAuthenticated$;

  showNotifications = false;
  notificationUnreadCount = 0;
  private pollSubscription: Subscription | null = null;
  private pollInterval = 30000; // 30 seconds

  ngOnInit(): void {
    // Start polling for notification count when authenticated
    this.authService.isAuthenticated$.subscribe(isAuth => {
      if (isAuth) {
        this.startPollingUnreadCount();
      } else {
        this.stopPollingUnreadCount();
      }
    });
  }

  ngOnDestroy(): void {
    this.stopPollingUnreadCount();
  }

  /**
   * Toggle notification panel visibility
   * When opening, explicitly trigger notification load
   */
  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;

    // If opening the panel, trigger load after a small delay to ensure component is rendered
    if (this.showNotifications) {
      setTimeout(() => {
        console.log('🔔 [Layout] Panel opened, ensuring notifications are loaded...');
        this.notificationPanel?.ensureLoaded();
      }, 50);
    }
  }

  /**
   * Start polling for unread notification count
   */
  private startPollingUnreadCount(): void {
    if (this.pollSubscription) {
      this.pollSubscription.unsubscribe();
    }

    // Fetch immediately
    this.fetchUnreadCount();

    // Then poll every 30 seconds
    this.pollSubscription = interval(this.pollInterval).subscribe(() => {
      this.fetchUnreadCount();
    });
  }

  /**
   * Stop polling
   */
  private stopPollingUnreadCount(): void {
    if (this.pollSubscription) {
      this.pollSubscription.unsubscribe();
      this.pollSubscription = null;
    }
    this.notificationUnreadCount = 0;
  }

  /**
   * Fetch unread notification count
   */
  private fetchUnreadCount(): void {
    this.apiService.getNotifications(0, 1).subscribe({
      next: (response: any) => {
        this.notificationUnreadCount = response.unreadCount || 0;
      },
      error: (err) => {
        console.warn('Failed to fetch notification count:', err);
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }
}

