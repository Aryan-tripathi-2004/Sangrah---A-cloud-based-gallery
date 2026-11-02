import { Injectable, inject, signal, DestroyRef } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UserApiService } from '../../core/api/services/user-api.service';
import { EventApiService } from '../../core/api/services/event-api.service';
import { GalleryApiService } from '../../core/api/services/gallery-api.service';
import { BillingService } from '../billing/billing.service';
import { Event, TimelineGroup } from '../../core/api/models/api.models';

@Injectable({ providedIn: 'root' })
export class DashboardFacadeService {
  private readonly userApi = inject(UserApiService);
  private readonly eventApi = inject(EventApiService);
  private readonly galleryApi = inject(GalleryApiService);
  private readonly billingService = inject(BillingService);
  private readonly destroyRef = inject(DestroyRef);

  readonly userDisplayName = signal<string>('User');
  readonly storageUsed = signal<number>(0);
  readonly storagePercentage = signal<number>(0);
  readonly storageLimit = signal<number>(100); // 100 GB default limit
  readonly galleryCount = signal<number>(0);
  readonly pendingCount = signal<number>(0);
  readonly recentEvents = signal<Event[]>([]);
  readonly isLoading = signal<boolean>(true);

  // Forward signal directly from BillingService
  readonly costEstimate = this.billingService.costEstimate;

  loadDashboardData(): void {
    this.isLoading.set(true);

    forkJoin({
      user: this.userApi.getProfile().pipe(
        catchError((err) => {
          console.error('Failed to load profile in dashboard facade:', err);
          return of(null);
        })
      ),
      costEstimate: this.billingService.loadCostEstimate().pipe(
        catchError((err) => {
          console.error('Failed to load cost estimate in dashboard facade:', err);
          return of(null);
        })
      ),
      storage: this.galleryApi.getStorageUsageGallery().pipe(
        catchError((err) => {
          console.error('Failed to load storage usage in dashboard facade:', err);
          return of(null);
        })
      ),
      events: this.eventApi.getGlobalEvents().pipe(
        catchError((err) => {
          console.error('Failed to load events in dashboard facade:', err);
          return of([]);
        })
      ),
      timeline: this.galleryApi.getGalleryTimeline().pipe(
        catchError((err) => {
          console.error('Failed to load gallery timeline in dashboard facade:', err);
          return of({ groups: [] });
        })
      ),
      pendingRequests: this.eventApi.getPendingAccessRequests().pipe(
        catchError((err) => {
          console.error('Failed to load pending requests in dashboard facade:', err);
          return of([]);
        })
      ),
    })
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe({
      next: ({ user, storage, events, timeline, pendingRequests }) => {
        if (user?.displayName) {
          this.userDisplayName.set(user.displayName);
        }

        if (storage) {
          const usedBytes = storage.totalBytesUsed || 0;
          const limitBytes = this.storageLimit() * 1024 * 1024 * 1024;
          this.storageUsed.set(usedBytes / (1024 * 1024 * 1024));
          this.storagePercentage.set(limitBytes > 0 ? (usedBytes / limitBytes) * 100 : 0);
        } else {
          this.storageUsed.set(0);
          this.storagePercentage.set(0);
        }

        this.recentEvents.set(Array.isArray(events) ? events.slice(0, 5) : []);

        if (timeline?.groups && Array.isArray(timeline.groups)) {
          const totalItems = timeline.groups.reduce((total: number, group: TimelineGroup) => {
            return total + (group.items?.length || 0);
          }, 0);
          this.galleryCount.set(totalItems);
        } else {
          this.galleryCount.set(0);
        }

        this.pendingCount.set(Array.isArray(pendingRequests) ? pendingRequests.length : 0);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Unexpected error loading dashboard data:', err);
        this.isLoading.set(false);
      }
    });
  }
}
