import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SangrahApiService, Event } from '../../core/api/sangrah-api.service';
import { BillingService, CostEstimateDTO } from '../billing/billing.service';
import { LayoutComponent } from '../../shared/layout/layout.component';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, LayoutComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  private api = inject(SangrahApiService);
  private billingService = inject(BillingService);

  userDisplayName = 'User';
  storageUsed = 0;
  storagePercentage = 0;
  storageLimit = 100; // 100 GB default limit
  galleryCount = 0;
  pendingCount = 0;
  recentEvents: Event[] = [];
  isLoading = true;

  // Billing observables
  costEstimate$: Observable<CostEstimateDTO | null>;

  constructor() {
    this.costEstimate$ = this.billingService.costEstimate$;
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    // Load user profile for display name
    this.api.getProfile().subscribe({
      next: (user) => {
        this.userDisplayName = user.displayName || 'User';
      },
      error: (err) => {
        console.error('Failed to load profile:', err);
      }
    });

    // Load cost estimate
    this.billingService.loadCostEstimate().subscribe({
      error: (err) => console.error('Failed to load cost estimate:', err)
    });

    // Load storage usage - API returns { totalBytesUsed, fileCount, breakdown: {...}, formatted: {...} }
    this.api.getStorageUsage().subscribe({
      next: (data: any) => {
        const usedBytes = data.totalBytesUsed || 0;
        const limitBytes = this.storageLimit * 1024 * 1024 * 1024; // 100 GB in bytes
        this.storageUsed = usedBytes / (1024 * 1024 * 1024);
        this.storagePercentage = (usedBytes / limitBytes) * 100;
        console.log('Storage loaded:', { storageUsed: this.storageUsed, percentage: this.storagePercentage });
      },
      error: (err) => {
        console.error('Failed to load storage usage:', err);
        this.storageUsed = 0;
        this.storagePercentage = 0;
      }
    });

    // Load global events
    this.api.getGlobalEvents().subscribe({
      next: (events: any) => {
        this.recentEvents = Array.isArray(events) ? events.slice(0, 5) : [];
        this.isLoading = false;
        console.log('Events loaded:', this.recentEvents.length);
      },
      error: (err) => {
        console.error('Failed to load events:', err);
        this.recentEvents = [];
        this.isLoading = false;
      }
    });

    // Load gallery timeline - API returns { view, groups: [], totalGroups }
    this.api.getGalleryTimeline().subscribe({
      next: (response: any) => {
        // Sum up all items from all timeline groups
        if (response.groups && Array.isArray(response.groups)) {
          this.galleryCount = response.groups.reduce((total: number, group: any) => {
            return total + (group.items?.length || 0);
          }, 0);
        }
        console.log('Gallery loaded:', this.galleryCount);
      },
      error: (err) => {
        console.error('Failed to load gallery timeline:', err);
        this.galleryCount = 0;
      }
    });

    // Load pending access requests
    this.api.getPendingAccessRequests().subscribe({
      next: (requests: any) => {
        this.pendingCount = Array.isArray(requests) ? requests.length : 0;
        console.log('Pending requests loaded:', this.pendingCount);
      },
      error: (err) => {
        console.error('Failed to load pending requests:', err);
        this.pendingCount = 0;
      }
    });
  }
}
