import { Component, OnInit, inject, ChangeDetectionStrategy, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { SangrahApiService, Event, EventAccessRequest } from '../../core/api/sangrah-api.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-pending-requests-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, LayoutComponent],
  templateUrl: './pending-requests-detail.component.html',
})
export class PendingRequestsDetailComponent implements OnInit {
  private readonly api = inject(SangrahApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly event = signal<Event | null>(null);
  readonly accessRequests = signal<EventAccessRequest[]>([]);
  readonly isLoading = signal<boolean>(true);

  ngOnInit(): void {
    this.route.paramMap.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((params) => {
      const eventId = params.get('eventId');
      if (eventId) {
        this.loadEventAndRequests(eventId);
      }
    });
  }

  loadEventAndRequests(eventId: string): void {
    this.isLoading.set(true);

    this.api.getEventById(eventId).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (event) => {
        this.event.set(event);
      },
      error: (err) => console.error('Failed to load event details:', err)
    });

    this.api.getEventAccessRequests(eventId).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (requests) => {
        this.accessRequests.set(requests || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load access requests:', err);
        this.accessRequests.set([]);
        this.isLoading.set(false);
      },
    });
  }

  approveRequest(requestId: string): void {
    const currentEvent = this.event();
    if (currentEvent) {
      this.api.updateAccessRequestStatus(currentEvent.id, requestId, 'APPROVED').pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: () => {
          this.accessRequests.update(requests => requests.filter((r) => r.id !== requestId));
        },
        error: (err) => console.error('Failed to approve request:', err)
      });
    }
  }

  rejectRequest(requestId: string): void {
    const currentEvent = this.event();
    if (currentEvent) {
      this.api.updateAccessRequestStatus(currentEvent.id, requestId, 'REJECTED', {}).pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: () => {
          this.accessRequests.update(requests => requests.filter((r) => r.id !== requestId));
        },
        error: (err) => console.error('Failed to reject request:', err)
      });
    }
  }
}
