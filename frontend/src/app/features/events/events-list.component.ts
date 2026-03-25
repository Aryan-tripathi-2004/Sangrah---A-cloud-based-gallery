import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SangrahApiService, Event } from '../../core/api/sangrah-api.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-events-list',
  standalone: true,
  imports: [CommonModule, RouterLink, LayoutComponent],
  template: `
    <app-layout>
      <div class="space-y-8">
        <!-- Header -->
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-4xl font-bold mb-2">Events</h1>
            <p class="text-slate-400">Discover and join events happening around you</p>
          </div>
          <a href="#" class="px-6 py-3 bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 rounded-lg font-semibold transition">
            Create Event
          </a>
        </div>

        <!-- Filters -->
        <div class="flex gap-4 flex-wrap">
          <button class="px-4 py-2 rounded-lg bg-blue-600 text-white font-medium">All Events</button>
          <button class="px-4 py-2 rounded-lg bg-slate-700 hover:bg-slate-600 text-slate-300 font-medium transition">Public</button>
          <button class="px-4 py-2 rounded-lg bg-slate-700 hover:bg-slate-600 text-slate-300 font-medium transition">Protected</button>
          <button class="px-4 py-2 rounded-lg bg-slate-700 hover:bg-slate-600 text-slate-300 font-medium transition">My Events</button>
        </div>

        <!-- Events Grid -->
        <div *ngIf="!isLoading && events.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div *ngFor="let event of events" class="group rounded-xl bg-slate-800/30 border border-slate-700/50 hover:border-slate-600 overflow-hidden transition">
            <!-- Event Banner -->
            <div class="h-40 bg-gradient-to-br from-blue-600/20 to-purple-600/20 relative overflow-hidden">
              <div class="absolute inset-0 flex items-center justify-center text-5xl opacity-30 group-hover:scale-110 transition transform">
                🎉
              </div>
            </div>

            <!-- Event Info -->
            <div class="p-6">
              <h3 class="font-bold text-xl mb-2 group-hover:text-blue-400 transition">{{ event.title }}</h3>
              <p class="text-sm text-slate-400 mb-4 line-clamp-2">{{ event.description }}</p>

              <!-- Meta Info -->
              <div class="space-y-2 mb-4 text-sm text-slate-400">
                <div class="flex items-center gap-2">
                  <span>📅</span>
                  <span>{{ event.eventDate | date:'MMM d, y' }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <span *ngIf="event.visibility === 'PUBLIC'">🌐</span>
                  <span *ngIf="event.visibility === 'PROTECTED'">🔒</span>
                  <span>{{ event.visibility }}</span>
                </div>
              </div>

              <!-- Action Button -->
              <a [routerLink]="['/event', event.id]" class="block w-full text-center px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg font-semibold transition">
                View Event
              </a>
            </div>
          </div>
        </div>

        <!-- Loading State -->
        <div *ngIf="isLoading" class="text-center py-16">
          <p class="text-slate-400">Loading events...</p>
        </div>

        <!-- Empty State -->
        <div *ngIf="!isLoading && events.length === 0" class="text-center py-16">
          <div class="text-5xl mb-4">🎪</div>
          <h3 class="text-2xl font-bold mb-2">No Events Found</h3>
          <p class="text-slate-400 mb-6">Get started by creating your first event</p>
          <a href="#" class="inline-block px-6 py-3 bg-blue-600 hover:bg-blue-700 rounded-lg font-semibold transition">
            Create Event
          </a>
        </div>
      </div>
    </app-layout>
  `,
})
export class EventsListComponent implements OnInit {
  private api = inject(SangrahApiService);

  events: Event[] = [];
  isLoading = true;

  ngOnInit(): void {
    this.loadEvents();
  }

  loadEvents(): void {
    this.api.getGlobalEvents().subscribe({
      next: (events) => {
        this.events = events;
        this.isLoading = false;
      },
      error: () => {
        this.events = [];
        this.isLoading = false;
      },
    });
  }
}
