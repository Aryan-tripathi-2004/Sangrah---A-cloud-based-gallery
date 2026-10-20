import { Component, OnInit, inject, ChangeDetectionStrategy, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SangrahApiService, Event } from '../../core/api/sangrah-api.service';
import { BillingService, CostEstimateDTO } from '../billing/billing.service';
import { LayoutComponent } from '../../shared/layout/layout.component';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, LayoutComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  private readonly api = inject(SangrahApiService);
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

  // Billing observables
  readonly costEstimate$: Observable<CostEstimateDTO | null>;

  constructor() {
    this.costEstimate$ = this.billingService.costEstimate$;
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    // Load user profile for display name
    this.api.getProfile().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (user) => {
        this.userDisplayName.set(user.displayName || 'User');
      },
      error: (err) => {
        console.error('Failed to load profile:', err);
      }
    });

    // Load cost estimate
    this.billingService.loadCostEstimate().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      error: (err) => console.error('Failed to load cost estimate:', err)
    });

    // Load storage usage
    this.api.getStorageUsage().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (data) => {
        const usedBytes = data.totalBytesUsed || 0;
        const limitBytes = this.storageLimit() * 1024 * 1024 * 1024; // 100 GB in bytes
        this.storageUsed.set(usedBytes / (1024 * 1024 * 1024));
        this.storagePercentage.set(limitBytes > 0 ? (usedBytes / limitBytes) * 100 : 0);
      },
      error: (err) => {
        console.error('Failed to load storage usage:', err);
        this.storageUsed.set(0);
        this.storagePercentage.set(0);
      }
    });

    // Load global events
    this.api.getGlobalEvents().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (events) => {
        this.recentEvents.set(Array.isArray(events) ? events.slice(0, 5) : []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load events:', err);
        this.recentEvents.set([]);
        this.isLoading.set(false);
      }
    });

    // Load gallery timeline
    this.api.getGalleryTimeline().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (response.groups && Array.isArray(response.groups)) {
          const totalItems = response.groups.reduce((total: number, group: any) => {
            return total + (group.items?.length || 0);
          }, 0);
          this.galleryCount.set(totalItems);
        }
      },
      error: (err) => {
        console.error('Failed to load gallery timeline:', err);
        this.galleryCount.set(0);
      }
    });

    // Load pending access requests
    this.api.getPendingAccessRequests().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (requests) => {
        this.pendingCount.set(Array.isArray(requests) ? requests.length : 0);
      },
      error: (err) => {
        console.error('Failed to load pending requests:', err);
        this.pendingCount.set(0);
      }
    });
  }
}
