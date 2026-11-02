import { Component, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DashboardFacadeService } from './dashboard-facade.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, LayoutComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  private readonly facade = inject(DashboardFacadeService);

  readonly userDisplayName = this.facade.userDisplayName;
  readonly storageUsed = this.facade.storageUsed;
  readonly storagePercentage = this.facade.storagePercentage;
  readonly storageLimit = this.facade.storageLimit;
  readonly galleryCount = this.facade.galleryCount;
  readonly pendingCount = this.facade.pendingCount;
  readonly recentEvents = this.facade.recentEvents;
  readonly isLoading = this.facade.isLoading;
  readonly costEstimate = this.facade.costEstimate;

  ngOnInit(): void {
    this.facade.loadDashboardData();
  }
}
