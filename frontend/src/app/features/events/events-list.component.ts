import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router, ActivatedRoute } from '@angular/router';
import { SangrahApiService, Event } from '../../core/api/sangrah-api.service';
import { AuthService } from '../../core/auth/auth.service';
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
  private authService = inject(AuthService);
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
        this.events = (events || []).map((event: any) => ({
          ...event,
          visibility: (event.visibility || '').toString().toUpperCase()
        }));
        
        // Get current user ID to check ownership
        this.authService.getCurrentUserId().subscribe({
          next: (currentUserId: string) => {
            // Initialize accessStatus for protected events
            this.events.forEach(event => {
              if (event.visibility === 'PROTECTED') {
                // Check if user is owner
                if ((event as any).ownerUserId === currentUserId) {
                  (event as any).accessStatus = 'owner'; // Mark as owner
                } else {
                  (event as any).accessStatus = 'loading'; // Default to loading state
                }
              }
            });
            
            this.isLoading = false;
            this.cdr.detectChanges();
            
            // Fetch access status for protected events in background (non-blocking)
            this.events.forEach(event => {
              if (event.visibility === 'PROTECTED' && (event as any).accessStatus !== 'owner') {
                this.api.getAccessStatus(event.id).subscribe({
                  next: (status: any) => {
                    (event as any).accessStatus = status.status ? status.status.toLowerCase() : 'none';
                    this.cdr.detectChanges();
                  },
                  error: () => {
                    (event as any).accessStatus = 'none';
                    this.cdr.detectChanges();
                  }
                });
              }
            });
          },
          error: () => {
            // If can't get user ID, proceed without owner check
            this.events.forEach(event => {
              if (event.visibility === 'PROTECTED') {
                (event as any).accessStatus = 'loading';
              }
            });
            
            this.isLoading = false;
            this.cdr.detectChanges();
            
            this.events.forEach(event => {
              if (event.visibility === 'PROTECTED') {
                this.api.getAccessStatus(event.id).subscribe({
                  next: (status: any) => {
                    (event as any).accessStatus = status.status ? status.status.toLowerCase() : 'none';
                    this.cdr.detectChanges();
                  },
                  error: () => {
                    (event as any).accessStatus = 'none';
                    this.cdr.detectChanges();
                  }
                });
              }
            });
          }
        });
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
    // For protected events without access, show request modal
    if (event.visibility === 'PROTECTED' && ['none', 'expired', 'revoked', 'rejected'].includes((event as any).accessStatus)) {
      // Create a fresh copy of the event to avoid stale references
      this.pendingRequestEvent = JSON.parse(JSON.stringify(event));
      this.showRequestModal = true;
      this.cdr.detectChanges();
      return;
    }
    
    // For owners, navigate to view event
    if (event.visibility === 'PROTECTED' && (event as any).accessStatus === 'owner') {
      this.router.navigate(['/event', event.id]);
      return;
    }
    
    // If loading, wait and try again or just navigate (let detail page handle it)
    if (event.visibility === 'PROTECTED' && (event as any).accessStatus === 'loading') {
      // Navigate to event detail page; it will handle access restrictions
      this.router.navigate(['/event', event.id]);
      return;
    }
    
    // For other cases, navigate to event detail
    this.router.navigate(['/event', event.id]);
  }
  
  closeRequestModal(): void {
    this.showRequestModal = false;
    this.pendingRequestEvent = null;
    this.cdr.detectChanges();
  }

  onRequestSubmitted(): void {
    // Mark the corresponding event in the list as pending
    if (!this.pendingRequestEvent) return;
    const id = this.pendingRequestEvent.id || this.pendingRequestEvent.eventId;
    const found = this.events.find(e => e.id === id);
    if (found) {
      (found as any).accessStatus = 'pending';
      this.cdr.detectChanges();
    }
    this.closeRequestModal();
  }

  onRequestWithdrawn(): void {
    if (!this.pendingRequestEvent) return;
    const id = this.pendingRequestEvent.id || this.pendingRequestEvent.eventId;
    const found = this.events.find(e => e.id === id);
    if (found) {
      (found as any).accessStatus = 'none';
      this.cdr.detectChanges();
    }
    this.closeRequestModal();
  }
}
