import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import { SangrahApiService, Event } from '../../core/api/sangrah-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { LayoutComponent } from '../../shared/layout/layout.component';
import { EventMediaUploadComponent } from './components/event-media-upload.component';
import { EventMediaApprovalComponent } from './components/event-media-approval.component';
import { EventTimelineComponent } from './components/event-timeline.component';
import { EventAccessRequestComponent } from './components/event-access-request.component';
import { EventAccessRequestsComponent } from './components/event-access-requests.component';
import { EventCollaboratorsComponent } from './components/event-collaborators.component';
import { EventService } from './event.service';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    LayoutComponent,
    EventMediaUploadComponent,
    EventMediaApprovalComponent,
    EventTimelineComponent,
    EventAccessRequestComponent,
    EventAccessRequestsComponent,
    EventCollaboratorsComponent,
  ],
  templateUrl: './event-detail.component.html',
})

export class EventDetailComponent implements OnInit {
  private api = inject(SangrahApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);
  private eventService = inject(EventService);
  private cdr = inject(ChangeDetectorRef);

  event: Event | null = null;
  eventId = '';
  isLoading = true;
  requiresApproval = false;
  currentTab = 'timeline';
  isEventOwner = false;
  isCollaborator = false;

  // Collaborator-specific permissions
  canUploadMedia = false;
  canDirectUpload = false;
  canDeleteMedia = false;
  canReviewMedia = false;
  canReviewAccessRequests = false;
  canEditEventDetails = false;

  pendingAccessCount = 0;

  // Edit modal properties
  showEditModal = false;
  isSavingEvent = false;
  editFormData = {
    title: '',
    description: '',
    eventDate: '',
    visibility: 'PUBLIC',
    moderationEnabled: false,
  };

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const eventId = params.get('id');
      if (eventId) {
        this.eventId = eventId;
        this.loadEvent(eventId);
      }
    });
  }

  loadEvent(eventId: string): void {
    this.api.getEventById(eventId).subscribe({
      next: (event: any) => {
        this.event = event;
        console.log('📌 [EventDetail] Event loaded:', {
          eventId: event.id,
          ownerUserId: event.ownerUserId,
          collaborators: event.collaborators,
          collaboratorCount: event.collaborators ? event.collaborators.length : 0
        });

        // CRITICAL: Log the raw collaborators array to see exact structure
        if (event.collaborators && event.collaborators.length > 0) {
          console.log('🔬 [EventDetail] RAW Collaborators Array:');
          console.log(JSON.stringify(event.collaborators, null, 2));
        }

        // Check if user needs to request access for PROTECTED events
        this.requiresApproval = event.requiresApproval === true;

        // Get current user ID and compare with event owner
        this.authService.getCurrentUserId().subscribe({
          next: (currentUserId: string) => {
            console.log('👤 [EventDetail] Current user ID:', currentUserId);

            this.isEventOwner = currentUserId === event.ownerUserId;
            console.log('🔑 [EventDetail] Is event owner:', this.isEventOwner);

            // If not owner, check if user is a collaborator with specific permissions
            if (!this.isEventOwner && event.collaborators) {
              console.log('🔍 [EventDetail] Checking collaborators for user:', currentUserId);
              console.log('📋 [EventDetail] Available collaborators:', event.collaborators);

              // Detailed debugging: log each collaborator's userId field
              if (event.collaborators && event.collaborators.length > 0) {
                console.log('🔎 [EventDetail] Collaborators detailed inspection:');
                event.collaborators.forEach((c: any, index: number) => {
                  console.log(`  [${index}] All available fields in collaborator object:`);
                  console.log(`    Fields: ${Object.keys(c).join(', ')}`);
                  Object.keys(c).forEach(key => {
                    console.log(`      ${key}: ${JSON.stringify(c[key])} (type: ${typeof c[key]})`);
                  });
                });
                console.log('🔎 [EventDetail] Current user ID to match: "' + currentUserId + '" (type: ' + typeof currentUserId + ')');
              }

              // Try to find collaborator with various field names (fallback if field name differs)
              let collaborator = event.collaborators.find((c: any) => c.userId === currentUserId);

              if (!collaborator) {
                // Fallback 1: Try snake_case field name
                collaborator = event.collaborators.find((c: any) => c.user_id === currentUserId);
                if (collaborator) {
                  console.log('ℹ️ [EventDetail] Found using fallback: user_id field');
                }
              }

              if (!collaborator) {
                // Fallback 2: Try lowercase comparison
                collaborator = event.collaborators.find((c: any) =>
                  (c.userId || c.user_id || '').toLowerCase() === currentUserId.toLowerCase()
                );
                if (collaborator) {
                  console.log('ℹ️ [EventDetail] Found using fallback: case-insensitive match');
                }
              }

              if (!collaborator) {
                // Fallback 3: Try trimming whitespace
                collaborator = event.collaborators.find((c: any) =>
                  (c.userId || c.user_id || '').trim() === currentUserId.trim()
                );
                if (collaborator) {
                  console.log('ℹ️ [EventDetail] Found using fallback: trimmed whitespace match');
                }
              }

              if (!collaborator) {
                // Fallback 4: Try matching ANY field value that contains the current user ID
                console.log('🔍 [EventDetail] Fallback 4: Searching all fields for user ID match...');
                for (let c of event.collaborators) {
                  for (let key of Object.keys(c)) {
                    if (c[key] === currentUserId) {
                      console.log(`ℹ️ [EventDetail] Found user ID in field "${key}"`);
                      collaborator = c;
                      break;
                    }
                  }
                  if (collaborator) break;
                }
              }

              if (collaborator) {
                console.log('✅ [EventDetail] User found as collaborator:', collaborator);
                this.isCollaborator = true;
                this.canUploadMedia = collaborator.canUploadMedia === true;
                this.canDirectUpload = collaborator.canDirectUpload === true;
                this.canDeleteMedia = collaborator.canDeleteMedia === true;
                this.canReviewMedia = collaborator.canReviewMedia === true;
                this.canReviewAccessRequests = collaborator.canReviewAccessRequests === true;
                this.canEditEventDetails = collaborator.canEditEventDetails === true;
                console.log('📊 [EventDetail] Collaborator permissions set:', {
                  canUploadMedia: this.canUploadMedia,
                  canDirectUpload: this.canDirectUpload,
                  canDeleteMedia: this.canDeleteMedia,
                  canReviewMedia: this.canReviewMedia,
                  canReviewAccessRequests: this.canReviewAccessRequests,
                  canEditEventDetails: this.canEditEventDetails
                });
              } else {
                console.log('❌ [EventDetail] User NOT found in collaborators list - no match in any field name variation');
                console.log('⚠️ [EventDetail] DEBUGGING: Expected user ID: ' + currentUserId);
                console.log('⚠️ [EventDetail] DEBUGGING: Collaborator in array:', event.collaborators[0]);
              }
            } else if (this.isEventOwner) {
              // Owner has all permissions
              console.log('🔑 [EventDetail] Setting owner permissions (all enabled)');
              this.isCollaborator = true;
              this.canUploadMedia = true;
              this.canDirectUpload = true;
              this.canDeleteMedia = true;
              this.canReviewMedia = true;
              this.canReviewAccessRequests = true;
              this.canEditEventDetails = true;
            }

            // Only load media if user has approval or event is public
            if (!this.requiresApproval) {
              this.loadEventMedia(eventId);
            } else {
              this.isLoading = false;
            }
          },
          error: (error) => {
            console.error('❌ Failed to get current user ID:', error);
            this.isEventOwner = false;
            this.isCollaborator = false;
            this.isLoading = false;
          },
        });
      },
      error: (error) => {
        console.error('❌ Failed to load event:', error);
        this.isLoading = false;
      },
    });
  }

  loadEventMedia(eventId: string): void {
    this.eventService.loadEventMedia(eventId).subscribe({
      next: () => {
        this.isLoading = false;
        // ensure UI updates immediately after media loads
        try { this.cdr.detectChanges(); } catch {}
      },
      error: (error) => {
        console.error('Failed to load event media:', error);
        this.isLoading = false;
        try { this.cdr.detectChanges(); } catch {}
      },
    });
  }

  onUploadComplete(): void {
    // Reload media list after upload
    this.loadEventMedia(this.eventId);
  }

  onMediaUpdated(): void {
    // Reload media list after approval/rejection
    this.loadEventMedia(this.eventId);
  }

  openEditEventModal(): void {
    if (this.event) {
      this.editFormData = {
        title: this.event.title,
        description: this.event.description,
        eventDate: this.event.eventDate,
        visibility: this.event.visibility,
        moderationEnabled: this.event.moderationEnabled || false,
      };
      this.showEditModal = true;
    }
  }

  closeEditModal(): void {
    this.showEditModal = false;
  }

  saveEventChanges(): void {
    if (!this.eventId || !this.editFormData.title) {
      alert('Please fill in all required fields');
      return;
    }

    this.isSavingEvent = true;
    this.api.updateEvent(this.eventId, this.editFormData).subscribe({
      next: (updatedEvent: any) => {
        this.event = updatedEvent;
        this.showEditModal = false;
        this.isSavingEvent = false;
        alert('Event updated successfully');
      },
      error: (error) => {
        this.isSavingEvent = false;
        console.error('Failed to update event:', error);
        alert('Failed to update event: ' + (error.error?.error || error.message));
      },
    });
  }

  deleteEvent(): void {
    if (!confirm('Are you sure you want to delete this event? This action cannot be undone.')) {
      return;
    }

    this.api.deleteEvent(this.eventId).subscribe({
      next: () => {
        alert('Event deleted successfully');
        this.router.navigate(['/event']);
      },
      error: (error) => {
        console.error('Failed to delete event:', error);
        alert('Failed to delete event: ' + (error.error?.error || error.message));
      },
    });
  }
}






