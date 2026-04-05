import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { SangrahApiService, Event } from '../../core/api/sangrah-api.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-pending-requests-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, LayoutComponent],
  template: `
    <app-layout>
      <div class="space-y-8">
        <!-- Back Button -->
        <a routerLink="/pending-requests" class="inline-flex items-center gap-2 text-blue-400 hover:text-blue-300 transition">
          ← Back to Requests
        </a>

        <!-- Event Info -->
        <div *ngIf="event && !isLoading" class="bg-gradient-to-r from-blue-600/20 to-purple-600/20 border border-blue-500/30 rounded-2xl p-8">
          <h1 class="text-3xl font-bold mb-2">{{ event.title }}</h1>
          <p class="text-slate-300">Manage access requests for this event</p>
        </div>

        <!-- Access Requests -->
        <div class="space-y-4">
          <h2 class="text-2xl font-bold">Pending Requests ({{ accessRequests.length }})</h2>

          <div *ngIf="accessRequests.length > 0" class="space-y-3">
            <div *ngFor="let request of accessRequests" class="p-4 rounded-lg bg-slate-800/30 border border-slate-700/50 flex items-center justify-between gap-4">
              <div class="flex-1">
                <p class="font-semibold">User Request</p>
                <p class="text-sm text-slate-400">Requested on {{ request.requestedAt | date:'MMM d, y' }}</p>
              </div>
              <div class="flex gap-2">
                <button (click)="approveRequest(request.id)" class="px-4 py-2 bg-green-600 hover:bg-green-700 rounded-lg text-sm font-semibold transition">
                  Approve
                </button>
                <button (click)="rejectRequest(request.id)" class="px-4 py-2 bg-red-600 hover:bg-red-700 rounded-lg text-sm font-semibold transition">
                  Reject
                </button>
              </div>
            </div>
          </div>

          <div *ngIf="accessRequests.length === 0 && !isLoading" class="text-center py-12 rounded-lg bg-slate-800/20 border border-slate-700/50">
            <p class="text-slate-400">No pending access requests for this event</p>
          </div>
        </div>

        <!-- Loading State -->
        <div *ngIf="isLoading" class="text-center py-16">
          <p class="text-slate-400">Loading access requests...</p>
        </div>
      </div>
    </app-layout>
  `,
})
export class PendingRequestsDetailComponent implements OnInit {
  private api = inject(SangrahApiService);
  private route = inject(ActivatedRoute);

  event: Event | null = null;
  accessRequests: any[] = [];
  isLoading = true;

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const eventId = params.get('eventId');
      if (eventId) {
        this.loadEventAndRequests(eventId);
      }
    });
  }

  loadEventAndRequests(eventId: string): void {
    this.api.getEventById(eventId).subscribe({
      next: (event) => {
        this.event = event;
      },
    });

    this.api.getEventAccessRequests(eventId).subscribe({
      next: (requests: any) => {
        this.accessRequests = requests;
        this.isLoading = false;
      },
      error: () => {
        this.accessRequests = [];
        this.isLoading = false;
      },
    });
  }

  approveRequest(requestId: string): void {
    if (this.event) {
      this.api.approveAccessRequest(this.event.id, requestId).subscribe(() => {
        this.accessRequests = this.accessRequests.filter((r) => r.id !== requestId);
      });
    }
  }

  rejectRequest(requestId: string): void {
    if (this.event) {
      this.api.rejectAccessRequest(this.event.id, requestId, {}).subscribe(() => {
        this.accessRequests = this.accessRequests.filter((r) => r.id !== requestId);
      });
    }
  }
}
