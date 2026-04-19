import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { SangrahApiService, Event } from '../../core/api/sangrah-api.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-pending-requests-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, LayoutComponent],
  templateUrl: './pending-requests-detail.component.html',
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
