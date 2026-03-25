import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <div class="min-h-screen bg-gradient-to-b from-slate-950 to-slate-900 text-slate-100">
      <!-- Header -->
      <header class="border-b border-slate-800/50 bg-slate-900/50 backdrop-blur-sm sticky top-0 z-50">
        <nav class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div class="flex items-center justify-between h-16">
            <!-- Logo -->
            <div class="flex items-center gap-2">
              <div class="w-8 h-8 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg flex items-center justify-center font-bold">S</div>
              <span class="text-xl font-bold bg-gradient-to-r from-blue-400 to-purple-400 bg-clip-text text-transparent">Sangrah</span>
            </div>

            <!-- Desktop Navigation -->
            <div class="hidden md:flex items-center gap-1">
              <a *ngIf="!(isAuthenticated$ | async)" routerLink="/login" routerLinkActive="bg-slate-700" class="px-3 py-2 rounded-lg text-sm font-medium transition hover:bg-slate-800">Login</a>
              <a *ngIf="!(isAuthenticated$ | async)" routerLink="/signup" routerLinkActive="bg-slate-700" class="px-3 py-2 rounded-lg text-sm font-medium transition hover:bg-slate-800">Sign Up</a>

              <a *ngIf="isAuthenticated$ | async" routerLink="/dashboard" routerLinkActive="bg-slate-700" class="px-3 py-2 rounded-lg text-sm font-medium transition hover:bg-slate-800">Dashboard</a>
              <a *ngIf="isAuthenticated$ | async" routerLink="/event" routerLinkActive="bg-slate-700" class="px-3 py-2 rounded-lg text-sm font-medium transition hover:bg-slate-800">Events</a>
              <a *ngIf="isAuthenticated$ | async" routerLink="/gallery" routerLinkActive="bg-slate-700" class="px-3 py-2 rounded-lg text-sm font-medium transition hover:bg-slate-800">Gallery</a>
              <a *ngIf="isAuthenticated$ | async" routerLink="/billing" routerLinkActive="bg-slate-700" class="px-3 py-2 rounded-lg text-sm font-medium transition hover:bg-slate-800">Billing</a>
              <a *ngIf="isAuthenticated$ | async" routerLink="/pending-requests" routerLinkActive="bg-slate-700" class="px-3 py-2 rounded-lg text-sm font-medium transition hover:bg-slate-800">Requests</a>
              <a *ngIf="isAuthenticated$ | async" routerLink="/profile" routerLinkActive="bg-slate-700" class="px-3 py-2 rounded-lg text-sm font-medium transition hover:bg-slate-800">Profile</a>
              <a *ngIf="isAuthenticated$ | async" routerLink="/about" routerLinkActive="bg-slate-700" class="px-3 py-2 rounded-lg text-sm font-medium transition hover:bg-slate-800">About</a>
              <button *ngIf="isAuthenticated$ | async" (click)="logout()" class="ml-4 px-4 py-2 rounded-lg bg-red-600/20 hover:bg-red-600/30 text-red-400 text-sm font-medium transition">Logout</button>
            </div>
          </div>
        </nav>
      </header>

      <!-- Main Content -->
      <main class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-8">
        <ng-content></ng-content>
      </main>

      <!-- Footer -->
      <footer class="border-t border-slate-800/50 bg-slate-900/50 mt-16">
        <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-12">
          <div class="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
            <div>
              <h3 class="text-lg font-bold mb-4">Sangrah</h3>
              <p class="text-slate-400 text-sm">Cloud-based event and gallery management platform.</p>
            </div>
            <div>
              <h4 class="font-semibold mb-4">Product</h4>
              <ul class="space-y-2 text-sm text-slate-400">
                <li><a href="#" class="hover:text-slate-200 transition">Features</a></li>
                <li><a href="#" class="hover:text-slate-200 transition">Pricing</a></li>
                <li><a href="#" class="hover:text-slate-200 transition">Security</a></li>
              </ul>
            </div>
            <div>
              <h4 class="font-semibold mb-4">Company</h4>
              <ul class="space-y-2 text-sm text-slate-400">
                <li><a href="#" class="hover:text-slate-200 transition">About Us</a></li>
                <li><a href="#" class="hover:text-slate-200 transition">Blog</a></li>
                <li><a href="#" class="hover:text-slate-200 transition">Contact</a></li>
              </ul>
            </div>
            <div>
              <h4 class="font-semibold mb-4">Legal</h4>
              <ul class="space-y-2 text-sm text-slate-400">
                <li><a href="#" class="hover:text-slate-200 transition">Privacy</a></li>
                <li><a href="#" class="hover:text-slate-200 transition">Terms</a></li>
              </ul>
            </div>
          </div>
          <div class="border-t border-slate-800 pt-8 text-center text-sm text-slate-400">
            <p>&copy; 2026 Sangrah. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  `,
})
export class LayoutComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  isAuthenticated$ = this.authService.isAuthenticated$;

  logout(): void {
    this.authService.logout();
  }
}
