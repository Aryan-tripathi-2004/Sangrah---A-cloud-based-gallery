import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SangrahApiService, Event } from '../../core/api/sangrah-api.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-pending-requests',
  standalone: true,
  imports: [CommonModule, RouterLink, LayoutComponent],
  templateUrl: './pending-requests.component.html',
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
      next: (response: any) => {
        // Handle both array and paginated response
        const events = Array.isArray(response) ? response : (response as any).content || [];
        this.pendingEvents = events;

        if (Array.isArray(events)) {
          events.forEach((event) => {
            this.api.getEventAccessRequests(event.id).subscribe({
              next: (requests: any) => {
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
