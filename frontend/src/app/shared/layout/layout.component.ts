import { Component, inject, ViewChild, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { NotificationPanelComponent } from '../../core/notifications/notification-panel.component';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, RouterLinkActive, NotificationPanelComponent],
  templateUrl: './layout.component.html',
})
export class LayoutComponent {
  private authService = inject(AuthService);
  private notificationService = inject(NotificationService);
  private cdr = inject(ChangeDetectorRef);

  @ViewChild(NotificationPanelComponent) notificationPanel: NotificationPanelComponent | undefined;

  isAuthenticated$ = this.authService.isAuthenticated$;
  unreadCount$ = this.notificationService.unreadCount$;

  showNotifications = false;

  /**
   * Toggle notification panel visibility
   * When opening, explicitly trigger notification load
   */
  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
    this.cdr.markForCheck();

    if (this.showNotifications) {
      setTimeout(() => {
        this.notificationPanel?.ensureLoaded();
      }, 50);
    }
  }

  logout(): void {
    this.authService.logout();
  }
}
