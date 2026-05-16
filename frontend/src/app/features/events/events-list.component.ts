import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router, ActivatedRoute } from '@angular/router';
import { SangrahApiService, Event } from '../../core/api/sangrah-api.service';
import { LayoutComponent } from '../../shared/layout/layout.component';
import { map } from 'rxjs/operators';
import { EventAccessRequestComponent } from './components/event-access-request.component';

@Component({
  selector: 'app-events-list',
  standalone: true,
  imports: [CommonModule, RouterLink, LayoutComponent, EventAccessRequestComponent],
  templateUrl: './events-list.component.html',
})
export class EventsListComponent implements OnInit {
  private api = inject(SangrahApiService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);
  private activatedRoute = inject(ActivatedRoute);

  showRequestModal = false;
  pendingRequestEvent: any = null;

  events: Event[] = [];
  isLoading = true;
  selectedFilter: 'all' | 'public' | 'protected' | 'private' | 'myEvents' = 'all';

  ngOnInit(): void {
    this.loadEvents();

    // Open request modal when redirected here with a requestEventId query param
    try {
      this.activatedRoute.queryParamMap.subscribe(params => {
        const reqId = params.get('requestEventId');
        if (reqId) {
          this.api.getEventById(reqId).subscribe({
            next: (ev: any) => {
              this.pendingRequestEvent = ev;
              this.showRequestModal = true;
              this.cdr.detectChanges();
            },
            error: () => {
              this.pendingRequestEvent = { id: reqId };
              this.showRequestModal = true;
              this.cdr.detectChanges();
            }
          });
        }
      });
    } catch (e) { }
  }

  selectFilter(filter: 'all' | 'public' | 'protected' | 'private' | 'myEvents'): void {
    this.selectedFilter = filter;
    this.loadEvents();
  }

  loadEvents(): void {
    this.isLoading = true;
    let request;

    switch (this.selectedFilter) {
      case 'all':
        request = this.api.getGlobalEvents();
        break;
      case 'public':
        // Get global events and filter for PUBLIC visibility
        request = this.api.getGlobalEvents().pipe(
          map(events => events.filter(e => e.visibility === 'PUBLIC'))
        );
        break;
      case 'protected':
        // Get global events and filter for PROTECTED visibility
        request = this.api.getGlobalEvents().pipe(
          map(events => events.filter(e => e.visibility === 'PROTECTED'))
        );
        break;
      case 'private':
        // Get global events and filter for PRIVATE visibility
        request = this.api.getGlobalEvents().pipe(
          map(events => events.filter(e => e.visibility === 'PRIVATE'))
        );
        break;
      case 'myEvents':
        request = this.api.getMyEvents();
        break;
    }

    request.subscribe({
      next: (events) => {
        console.log('✅ [EventsListComponent] Successfully loaded events:', events);
        this.events = events || [];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('❌ [EventsListComponent] Failed to load events:', err);
        this.events = [];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }
getCoverImageUrl(mediaId: string): string {
  return this.api.getGalleryMediaFile(mediaId);
}
  viewEventOrRequest(event: Event): void {
    // Check server for access requirements before navigating
    this.api.getEventById(event.id).subscribe({
      next: (fullEvent: any) => {
        // If protected and requiresApproval flag set, show request modal instead of navigating
        if (fullEvent && fullEvent.visibility === 'PROTECTED' && fullEvent.requiresApproval === true) {
          this.pendingRequestEvent = fullEvent;
          this.showRequestModal = true;
          this.cdr.detectChanges();
          return;
        }
        // Otherwise navigate to event detail
        this.router.navigate(['/event', event.id]);
      },
      error: (err) => {
        // If backend returns 403 for protected event, open request modal
        if (err && err.status === 403) {
          this.pendingRequestEvent = event;
          this.showRequestModal = true;
          this.cdr.detectChanges();
          return;
        }
        console.error('Failed to check event access:', err);
      }
    });
  }

  onRequestSubmitted(): void {
    // Mark the corresponding event in the list as pending
    if (!this.pendingRequestEvent) return;
    const id = this.pendingRequestEvent.id || this.pendingRequestEvent.eventId;
    const found = this.events.find(e => e.id === id);
    if (found) {
      (found as any).accessStatus = 'PENDING';
    }
    this.showRequestModal = false;
    this.pendingRequestEvent = null;
    this.cdr.detectChanges();
  }

  onRequestWithdrawn(): void {
    if (!this.pendingRequestEvent) return;
    const id = this.pendingRequestEvent.id || this.pendingRequestEvent.eventId;
    const found = this.events.find(e => e.id === id);
    if (found) {
      (found as any).accessStatus = undefined;
    }
    this.showRequestModal = false;
    this.pendingRequestEvent = null;
    this.cdr.detectChanges();
  }
}
