import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './layout.component.html',
})
export class LayoutComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  isAuthenticated$ = this.authService.isAuthenticated$;

  logout(): void {
    this.authService.logout();
  }
}
