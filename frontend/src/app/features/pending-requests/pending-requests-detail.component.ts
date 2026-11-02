import { Component, OnInit, inject, ChangeDetectionStrategy, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { map, filter, switchMap, catchError } from 'rxjs/operators';
import { EventApiService } from '../../core/api/services/event-api.service';
import { Event, AccessRequestResponse } from '../../core/api/models/api.models';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-pending-requests-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, LayoutComponent],
  templateUrl: './pending-requests-detail.component.html',
})
export class PendingRequestsDetailComponent implements OnInit {
  private readonly api = inject(EventApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly event = signal<Event | null>(null);
  readonly accessRequests = signal<AccessRequestResponse[]>([]);
  readonly isLoading = signal<boolean>(true);

  ngOnInit(): void {
    this.route.paramMap.pipe(
      map(params => params.get('eventId')),
      filter((eventId): eventId is string => !!eventId),
      switchMap(eventId => {
        this.isLoading.set(true);
        return forkJoin({
          event: this.api.getEventById(eventId).pipe(
            catchError(err => {
              console.error('Failed to load event details:', err);
              return of<Event | null>(null);
            })
          ),
          requests: this.api.getEventAccessRequests(eventId).pipe(
            catchError(err => {
              console.error('Failed to load access requests:', err);
              return of<AccessRequestResponse[]>([]);
            })
          )
        });
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: ({ event, requests }) => {
        this.event.set(event);
        this.accessRequests.set(requests || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Unexpected error loading event and requests:', err);
        this.isLoading.set(false);
      }
    });
  }

  loadEventAndRequests(eventId: string): void {
    this.isLoading.set(true);
    forkJoin({
      event: this.api.getEventById(eventId).pipe(
        catchError(err => {
          console.error('Failed to load event details:', err);
          return of<Event | null>(null);
        })
      ),
      requests: this.api.getEventAccessRequests(eventId).pipe(
        catchError(err => {
          console.error('Failed to load access requests:', err);
          return of<AccessRequestResponse[]>([]);
        })
      )
    }).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: ({ event, requests }) => {
        this.event.set(event);
        this.accessRequests.set(requests || []);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  approveRequest(requestId: string): void {
    const currentEvent = this.event();
    if (currentEvent) {
      this.api.updateAccessRequestStatus(currentEvent.id, requestId, 'APPROVED').pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: () => {
          this.accessRequests.update(requests => requests.filter((r) => (r.requestId || r.id) !== requestId));
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
          this.accessRequests.update(requests => requests.filter((r) => (r.requestId || r.id) !== requestId));
        },
        error: (err) => console.error('Failed to reject request:', err)
      });
    }
  }
}
