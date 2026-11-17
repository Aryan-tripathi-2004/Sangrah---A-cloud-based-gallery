import { Component, OnInit, inject, ChangeDetectionStrategy, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router, ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { of } from 'rxjs';
import { map, filter, switchMap, catchError } from 'rxjs/operators';
import { EventApiService } from '../../core/api/services/event-api.service';
import { Event, EventAccessStatus } from '../../core/api/models/api.models';
import { LayoutComponent } from '../../shared/layout/layout.component';
import { EventAccessRequestComponent } from './components/event-access-request.component';

@Component({
  selector: 'app-events-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, LayoutComponent, EventAccessRequestComponent],
  templateUrl: './events-list.component.html',
})
export class EventsListComponent implements OnInit {
  private readonly eventApi = inject(EventApiService);
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  // Modern Angular 19 reactive signals
  readonly showRequestModal = signal<boolean>(false);
  readonly pendingRequestEvent = signal<Event | null>(null);
  readonly events = signal<Event[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly isLoadingMore = signal<boolean>(false);
  readonly currentPage = signal<number>(0);
  readonly hasMore = signal<boolean>(false);
  readonly totalElements = signal<number>(0);
  readonly selectedFilter = signal<'all' | 'public' | 'protected' | 'private' | 'myEvents'>('all');

  ngOnInit(): void {
    this.loadEvents(0, false);

    // Open request modal when redirected here with a requestEventId query param
    this.activatedRoute.queryParamMap
      .pipe(
        map(params => params.get('requestEventId')),
        filter((reqId): reqId is string => !!reqId),
        switchMap(reqId =>
          this.eventApi.getEventById(reqId).pipe(
            catchError(() => of<Event>({
              id: reqId,
              ownerUserId: '',
              title: '',
              description: '',
              eventDate: '',
              visibility: 'PROTECTED',
              moderationEnabled: false,
              status: 'ACTIVE',
              createdAt: ''
            }))
          )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(ev => {
        this.pendingRequestEvent.set(ev);
        this.showRequestModal.set(true);
      });
  }

  selectFilter(filter: 'all' | 'public' | 'protected' | 'private' | 'myEvents'): void {
    this.selectedFilter.set(filter);
    this.currentPage.set(0);
    this.loadEvents(0, false);
  }

  loadEvents(page = 0, append = false): void {
    if (append) {
      this.isLoadingMore.set(true);
    } else {
      this.isLoading.set(true);
    }

    let request;
    const size = 12;

    switch (this.selectedFilter()) {
      case 'all':
        request = this.eventApi.getGlobalEvents(undefined, page, size);
        break;
      case 'public':
        request = this.eventApi.getGlobalEvents('PUBLIC', page, size);
        break;
      case 'protected':
        request = this.eventApi.getGlobalEvents('PROTECTED', page, size);
        break;
      case 'private':
        request = this.eventApi.getGlobalEvents('PRIVATE', page, size);
        break;
      case 'myEvents':
        request = this.eventApi.getMyEvents(page, size);
        break;
    }

    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (response: any) => {
        const rawList: any[] = Array.isArray(response)
          ? response
          : (response?.content || []);

        const mapped: Event[] = rawList.map(event => ({
          ...event,
          id: event.id || event.eventId || '',
          visibility: (event.visibility || '').toString().toUpperCase(),
          accessStatus: (event.accessStatus ? event.accessStatus.toLowerCase() : 'none') as EventAccessStatus
        }));

        if (append) {
          this.events.update(prev => [...prev, ...mapped]);
        } else {
          this.events.set(mapped);
        }

        this.currentPage.set(page);
        if (response && response.totalElements !== undefined) {
          this.totalElements.set(response.totalElements);
          this.hasMore.set(response.hasNext ?? (!response.last && page + 1 < response.totalPages));
        } else {
          this.totalElements.set(mapped.length);
          this.hasMore.set(false);
        }

        this.isLoading.set(false);
        this.isLoadingMore.set(false);
      },
      error: (err) => {
        console.error('Failed to load events:', err);
        if (!append) {
          this.events.set([]);
        }
        this.isLoading.set(false);
        this.isLoadingMore.set(false);
      },
    });
  }

  loadMore(): void {
    if (!this.hasMore() || this.isLoadingMore() || this.isLoading()) {
      return;
    }
    this.loadEvents(this.currentPage() + 1, true);
  }

  getCoverImageUrl(mediaId: string): string {
    return this.eventApi.getMediaFileUrl(mediaId);
  }

  viewEventOrRequest(event: Event): void {
    const status = event.accessStatus || 'none';

    // For protected events without access, show request modal
    if (event.visibility === 'PROTECTED' && ['none', 'expired', 'revoked', 'rejected'].includes(status)) {
      this.pendingRequestEvent.set(JSON.parse(JSON.stringify(event)));
      this.showRequestModal.set(true);
      return;
    }

    // For owners, navigate to view event
    if (event.visibility === 'PROTECTED' && status === 'owner') {
      this.router.navigate(['/event', event.id]);
      return;
    }

    // If loading, wait and try again or just navigate (let detail page handle it)
    if (event.visibility === 'PROTECTED' && status === 'loading') {
      this.router.navigate(['/event', event.id]);
      return;
    }

    // For other cases, navigate to event detail
    this.router.navigate(['/event', event.id]);
  }

  closeRequestModal(): void {
    this.showRequestModal.set(false);
    this.pendingRequestEvent.set(null);
  }

  onRequestSubmitted(): void {
    const current = this.pendingRequestEvent();
    if (!current) return;
    const id = current.id || current.eventId;
    this.events.update(list => list.map(e => e.id === id ? { ...e, accessStatus: 'pending' as EventAccessStatus } : e));
    this.closeRequestModal();
  }

  onRequestWithdrawn(): void {
    const current = this.pendingRequestEvent();
    if (!current) return;
    const id = current.id || current.eventId;
    this.events.update(list => list.map(e => e.id === id ? { ...e, accessStatus: 'none' as EventAccessStatus } : e));
    this.closeRequestModal();
  }
}
