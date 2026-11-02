import { Component, OnInit, inject, ChangeDetectionStrategy, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EventApiService } from '../../core/api/services/event-api.service';
import { Event } from '../../core/api/models/api.models';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-pending-requests',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, LayoutComponent],
  templateUrl: './pending-requests.component.html',
})
export class PendingRequestsComponent implements OnInit {
  private readonly api = inject(EventApiService);
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
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response: Event[]) => {
        const events: Event[] = Array.isArray(response) ? response : [];
        this.pendingEvents.set(events);

        const counts: { [key: string]: number } = {};
        for (const event of events) {
          counts[event.id] = event.pendingRequestsCount ?? 0;
        }
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
