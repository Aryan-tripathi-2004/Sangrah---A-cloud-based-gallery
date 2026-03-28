import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, LayoutComponent],
  template: `
    <app-layout>
      <!-- Hero Section -->
      <div class="relative min-h-screen -mx-4 sm:-mx-6 lg:-mx-8 flex items-center bg-gradient-to-b from-slate-900 via-purple-900/10 to-slate-950">
        <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 w-full">
          <div class="text-center max-w-4xl mx-auto py-20">
            <h1 class="text-5xl sm:text-6xl font-bold mb-6 bg-gradient-to-r from-blue-400 via-purple-400 to-pink-400 bg-clip-text text-transparent">
              Share Moments, Manage Events
            </h1>
            <p class="text-xl text-slate-300 mb-8 leading-relaxed">
              The modern platform for collaborative events and shared galleries. Create, share, and manage your memories in one beautiful space.
            </p>
            <div class="flex gap-4 justify-center flex-wrap">
              <a routerLink="/signup" class="px-8 py-4 bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 rounded-lg font-semibold transition shadow-lg">
                Get Started Free
              </a>
              <a routerLink="/about" class="px-8 py-4 border border-slate-600 hover:border-slate-500 rounded-lg font-semibold transition">
                Learn More
              </a>
            </div>
          </div>

          <!-- Stats -->
          <div class="grid grid-cols-1 md:grid-cols-3 gap-8 mt-20">
            <div class="text-center">
              <div class="text-4xl font-bold text-blue-400 mb-2">500+</div>
              <p class="text-slate-400">Active Events</p>
            </div>
            <div class="text-center">
              <div class="text-4xl font-bold text-purple-400 mb-2">50K+</div>
              <p class="text-slate-400">Shared Memories</p>
            </div>
            <div class="text-center">
              <div class="text-4xl font-bold text-pink-400 mb-2">10K+</div>
              <p class="text-slate-400">Happy Users</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Features Section -->
      <div class="py-20 border-t border-slate-800/50">
        <div class="max-w-7xl mx-auto">
          <h2 class="text-4xl font-bold text-center mb-16">Why Choose Sangrah?</h2>

          <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div class="p-8 rounded-2xl bg-slate-800/30 border border-slate-700/50 hover:border-slate-600 transition">
              <div class="w-12 h-12 bg-blue-500/20 rounded-lg flex items-center justify-center mb-4">
                <span class="text-2xl">🎉</span>
              </div>
              <h3 class="text-xl font-semibold mb-3">Event Management</h3>
              <p class="text-slate-400">Create public or private events, manage access, and moderate content seamlessly.</p>
            </div>

            <div class="p-8 rounded-2xl bg-slate-800/30 border border-slate-700/50 hover:border-slate-600 transition">
              <div class="w-12 h-12 bg-purple-500/20 rounded-lg flex items-center justify-center mb-4">
                <span class="text-2xl">🖼️</span>
              </div>
              <h3 class="text-xl font-semibold mb-3">Shared Galleries</h3>
              <p class="text-slate-400">Upload and organize photos with your friends. Control visibility and sharing settings.</p>
            </div>

            <div class="p-8 rounded-2xl bg-slate-800/30 border border-slate-700/50 hover:border-slate-600 transition">
              <div class="w-12 h-12 bg-pink-500/20 rounded-lg flex items-center justify-center mb-4">
                <span class="text-2xl">💾</span>
              </div>
              <h3 class="text-xl font-semibold mb-3">Cloud Storage</h3>
              <p class="text-slate-400">Secure cloud storage for your media with transparent billing and unlimited scalability.</p>
            </div>
          </div>
        </div>
      </div>

      <!-- CTA Section -->
      <div class="py-20 text-center">
        <h2 class="text-4xl font-bold mb-6">Ready to start sharing?</h2>
        <p class="text-xl text-slate-400 mb-8">Join thousands of users creating amazing events and galleries.</p>
        <a routerLink="/signup" class="inline-block px-8 py-4 bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 rounded-lg font-semibold transition shadow-lg">
          Create Your Account
        </a>
      </div>
    </app-layout>
  `,
})
export class HomeComponent {}
