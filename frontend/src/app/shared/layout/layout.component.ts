import { Component, inject, ViewChild, ChangeDetectionStrategy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { NotificationPanelComponent } from '../components/notification-panel.component';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, RouterLinkActive, NotificationPanelComponent],
  templateUrl: './layout.component.html',
})
export class LayoutComponent {
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);

  @ViewChild(NotificationPanelComponent) notificationPanel: NotificationPanelComponent | undefined;

  readonly isAuthenticated$ = this.authService.isAuthenticated$;
  readonly isAuthenticated = this.authService.isAuthenticated;
  readonly unreadCount = this.notificationService.unreadCount;
  readonly showNotifications = signal<boolean>(false);

  /**
   * Toggle notification panel visibility
   * When opening, explicitly trigger notification load
   */
  toggleNotifications(): void {
    this.showNotifications.update(s => !s);

    if (this.showNotifications()) {
      setTimeout(() => {
        this.notificationPanel?.ensureLoaded();
      }, 50);
    }
  }

  logout(): void {
    this.authService.logout();
  }
}
