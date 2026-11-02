import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BillingService, CostEstimateDTO } from '../../billing.service';

@Component({
  selector: 'app-billing-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  templateUrl: './billing-summary.component.html'
})
export class BillingSummaryComponent {
  private readonly billingService = inject(BillingService);

  readonly costEstimate = this.billingService.costEstimate;

  getImagePercentage(estimate: CostEstimateDTO | null): number {
    if (!estimate) return 0;
    const total = estimate.storageBreakdown.images.gbDays + estimate.storageBreakdown.videos.gbDays;
    if (total === 0) return 0;
    return (estimate.storageBreakdown.images.gbDays / total) * 100;
  }

  getVideoPercentage(estimate: CostEstimateDTO | null): number {
    if (!estimate) return 0;
    const total = estimate.storageBreakdown.images.gbDays + estimate.storageBreakdown.videos.gbDays;
    if (total === 0) return 0;
    return (estimate.storageBreakdown.videos.gbDays / total) * 100;
  }

  getContextualMessage(estimate: CostEstimateDTO | null): string {
    if (!estimate) return '';
    return this.billingService.getContextualCostMessage(estimate);
  }
}
