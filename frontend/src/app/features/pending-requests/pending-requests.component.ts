import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SangrahApiService, Event } from '../../core/api/sangrah-api.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-pending-requests',
  standalone: true,
  imports: [CommonModule, RouterLink, LayoutComponent],
  template: `
    <app-layout>
      <div class="space-y-8">
        <!-- Header -->
        <div>
          <h1 class="text-4xl font-bold mb-2">Access Requests</h1>
          <p class="text-slate-400">Manage who can access your protected events</p>
        </div>

        <!-- Pending Requests -->
        <div *ngIf="!isLoading && pendingEvents.length > 0" class="space-y-4">
          <div *ngFor="let event of pendingEvents" class="p-6 rounded-xl bg-slate-800/30 border border-slate-700/50 hover:border-slate-600 transition">
            <div class="flex items-start justify-between gap-4">
              <div class="flex-1">
                <h3 class="font-bold text-xl mb-2">{{ event.title }}</h3>
                <p class="text-slate-400 mb-4">{{ event.description }}</p>
                <div class="flex gap-4 text-sm text-slate-400">
                  <span>📅 {{ event.eventDate | date:'MMM d, y' }}</span>
                  <span class="text-yellow-400">⏳ Pending Requests</span>
                </div>
              </div>
              <a [routerLink]="['/pending-requests', event.id]" class="px-6 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg font-semibold transition whitespace-nowrap">
                Review ({{ pendingCounts[event.id] || 0 }})
              </a>
            </div>
          </div>
        </div>

        <!-- Loading State -->
        <div *ngIf="isLoading" class="text-center py-16">
          <p class="text-slate-400">Loading pending requests...</p>
        </div>

        <!-- Empty State -->
        <div *ngIf="!isLoading && pendingEvents.length === 0" class="text-center py-16 bg-slate-800/20 border border-slate-700/50 rounded-lg">
          <div class="text-5xl mb-4">✨</div>
          <h3 class="text-2xl font-bold mb-2">All Caught Up</h3>
          <p class="text-slate-400">No pending access requests at the moment</p>
        </div>
      </div>
    </app-layout>
  `,
})
export class PendingRequestsComponent implements OnInit {
  private api = inject(SangrahApiService);

  pendingEvents: Event[] = [];
  pendingCounts: { [key: string]: number } = {};
  isLoading = true;

  ngOnInit(): void {
    this.loadPendingRequests();
  }

  loadPendingRequests(): void {
    this.api.getPendingAccessRequests().subscribe({
      next: (response) => {
        // Handle both array and paginated response
        const events = Array.isArray(response) ? response : (response as any).content || [];
        this.pendingEvents = events;

        if (Array.isArray(events)) {
          events.forEach((event) => {
            this.api.getEventAccessRequests(event.id).subscribe({
              next: (requests) => {
                this.pendingCounts[event.id] = requests.length;
              },
            });
          });
        }
        this.isLoading = false;
      },
      error: () => {
        this.pendingEvents = [];
        this.isLoading = false;
      },
    });
  }
}
