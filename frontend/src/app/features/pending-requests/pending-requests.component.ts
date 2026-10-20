import { Component, OnInit, inject, ChangeDetectionStrategy, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { switchMap, map, catchError } from 'rxjs/operators';
import { SangrahApiService, Event } from '../../core/api/sangrah-api.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-pending-requests',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, LayoutComponent],
  templateUrl: './pending-requests.component.html',
})
export class PendingRequestsComponent implements OnInit {
  private readonly api = inject(SangrahApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly pendingEvents = signal<Event[]>([]);
  readonly pendingCounts = signal<{ [key: string]: number }>({});
  readonly isLoading = signal<boolean>(true);

  ngOnInit(): void {
    this.loadPendingRequests();
  }

  loadPendingRequests(): void {
    this.isLoading.set(true);

    this.api.getPendingAccessRequests().pipe(
      switchMap((response: any) => {
        const events: Event[] = Array.isArray(response) ? response : (response as any)?.content || [];
        this.pendingEvents.set(events);

        if (!events || events.length === 0) {
          return of({} as { [key: string]: number });
        }

        // Parallelize requests with forkJoin to eliminate the N+1 problem
        const requestCountObservables = events.map(event =>
          this.api.getEventAccessRequests(event.id).pipe(
            map(requests => ({
              eventId: event.id,
              count: Array.isArray(requests) ? requests.length : 0
            })),
            catchError(() => of({ eventId: event.id, count: 0 }))
          )
        );

        return forkJoin(requestCountObservables).pipe(
          map(results => results.reduce((acc, curr) => {
            acc[curr.eventId] = curr.count;
            return acc;
          }, {} as { [key: string]: number }))
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (counts) => {
        this.pendingCounts.set(counts);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load pending access requests:', err);
        this.pendingEvents.set([]);
        this.pendingCounts.set({});
        this.isLoading.set(false);
      }
    });
  }
}
