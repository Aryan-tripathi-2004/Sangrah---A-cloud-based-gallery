import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { SangrahApiService, Event, Gallery } from '../../core/api/sangrah-api.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, LayoutComponent],
  template: `
    <app-layout>
      <div class="space-y-8">
        <!-- Back Button -->
        <a routerLink="/event" class="inline-flex items-center gap-2 text-blue-400 hover:text-blue-300 transition">
          ← Back to Events
        </a>

        <!-- Event Header -->
        <div *ngIf="event && !isLoading" class="bg-gradient-to-r from-blue-600/20 to-purple-600/20 border border-blue-500/30 rounded-2xl p-8">
          <div class="flex items-start justify-between mb-6">
            <div>
              <h1 class="text-4xl font-bold mb-2">{{ event.title }}</h1>
              <p class="text-slate-300">{{ event.description }}</p>
            </div>
            <div class="text-right">
              <div class="text-3xl mb-2">
                <span *ngIf="event.visibility === 'PUBLIC'">🌐</span>
                <span *ngIf="event.visibility === 'PROTECTED'">🔒</span>
              </div>
              <span class="px-3 py-1 rounded-full text-sm font-semibold" [ngClass]="{'bg-green-500/20 text-green-400': event.visibility === 'PUBLIC', 'bg-yellow-500/20 text-yellow-400': event.visibility === 'PROTECTED'}">
                {{ event.visibility }}
              </span>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <p class="text-slate-400 text-sm">Event Date</p>
              <p class="text-lg font-semibold">{{ event.eventDate | date:'MMM d, y' }}</p>
            </div>
            <div>
              <p class="text-slate-400 text-sm">Status</p>
              <p class="text-lg font-semibold capitalize">{{ event.status }}</p>
            </div>
            <div>
              <p class="text-slate-400 text-sm">Created</p>
              <p class="text-lg font-semibold">{{ event.createdAt | date:'MMM d, y' }}</p>
            </div>
          </div>
        </div>

        <!-- Tabs -->
        <div class="flex gap-4 border-b border-slate-800">
          <button
            (click)="currentTab = 'media'"
            [class.border-b-2]="currentTab === 'media'"
            [class.border-blue-500]="currentTab === 'media'"
            class="px-4 py-3 font-semibold transition hover:text-slate-100"
          >
            📸 Media
          </button>
          <button
            (click)="currentTab = 'requests'"
            [class.border-b-2]="currentTab === 'requests'"
            [class.border-blue-500]="currentTab === 'requests'"
            class="px-4 py-3 font-semibold transition hover:text-slate-100"
          >
            ⏳ Access Requests
          </button>
          <button
            (click)="currentTab = 'settings'"
            [class.border-b-2]="currentTab === 'settings'"
            [class.border-blue-500]="currentTab === 'settings'"
            class="px-4 py-3 font-semibold transition hover:text-slate-100"
          >
            ⚙️ Settings
          </button>
        </div>

        <!-- Media Tab -->
        <div *ngIf="currentTab === 'media'" class="space-y-6">
          <div class="flex items-center justify-between">
            <h2 class="text-2xl font-bold">Event Media</h2>
            <button class="px-6 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg font-semibold transition">
              Upload Media
            </button>
          </div>

          <div *ngIf="eventMedia.length > 0" class="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-4">
            <div *ngFor="let media of eventMedia" class="group rounded-lg overflow-hidden bg-slate-800/30 border border-slate-700/50 hover:border-slate-600 transition">
              <div class="h-40 bg-gradient-to-br from-blue-600/20 to-purple-600/20 flex items-center justify-center text-3xl">
                🖼️
              </div>
              <div class="p-3">
                <p class="font-semibold text-sm truncate">{{ media.originalFileName }}</p>
                <p class="text-xs text-slate-400">{{ media.uploadedAt | date:'short' }}</p>
              </div>
            </div>
          </div>

          <div *ngIf="eventMedia.length === 0" class="text-center py-12 rounded-lg bg-slate-800/20 border border-slate-700/50">
            <p class="text-slate-400 mb-4">No media uploaded yet</p>
            <button class="px-6 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg font-semibold transition">
              Upload First Media
            </button>
          </div>
        </div>

        <!-- Access Requests Tab -->
        <div *ngIf="currentTab === 'requests'" class="space-y-6">
          <h2 class="text-2xl font-bold">Access Requests</h2>
          <p class="text-slate-400">Users who have requested access to this protected event.</p>
          <div class="bg-slate-800/20 border border-slate-700/50 rounded-lg p-6 text-center">
            <p class="text-slate-400">0 pending access requests</p>
          </div>
        </div>

        <!-- Settings Tab -->
        <div *ngIf="currentTab === 'settings'" class="space-y-6">
          <h2 class="text-2xl font-bold">Event Settings</h2>
          <div class="bg-slate-800/20 border border-slate-700/50 rounded-lg p-6">
            <div class="space-y-4">
              <div>
                <label class="block text-sm font-semibold mb-2">Moderation Enabled</label>
                <div class="flex items-center gap-2">
                  <input type="checkbox" [checked]="event?.moderationEnabled" class="w-4 h-4" />
                  <span class="text-slate-400">Require approval for submitted media</span>
                </div>
              </div>
              <button class="px-6 py-2 bg-red-600 hover:bg-red-700 rounded-lg font-semibold transition">
                Delete Event
              </button>
            </div>
          </div>
        </div>

        <!-- Loading State -->
        <div *ngIf="isLoading" class="text-center py-16">
          <p class="text-slate-400">Loading event details...</p>
        </div>
      </div>
    </app-layout>
  `,
})
export class EventDetailComponent implements OnInit {
  private api = inject(SangrahApiService);
  private route = inject(ActivatedRoute);

  event: Event | null = null;
  eventMedia: Gallery[] = [];
  isLoading = true;
  currentTab = 'media';

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const eventId = params.get('id');
      if (eventId) {
        this.loadEvent(eventId);
      }
    });
  }

  loadEvent(eventId: string): void {
    this.api.getEventById(eventId).subscribe({
      next: (event) => {
        this.event = event;
        this.loadEventMedia(eventId);
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  loadEventMedia(eventId: string): void {
    this.api.getEventMedia(eventId).subscribe({
      next: (media) => {
        this.eventMedia = media;
        this.isLoading = false;
      },
      error: () => {
        this.eventMedia = [];
        this.isLoading = false;
      },
    });
  }
}
