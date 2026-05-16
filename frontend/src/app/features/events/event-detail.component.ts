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

  getCoverImageUrl(mediaId: string): string {
    return this.api.getGalleryMediaFile(mediaId);
  }

  loadEvent(eventId: string): void {
    this.api.getEventById(eventId).subscribe({
      next: (event: any) => {
        this.event = event;

        this.requiresApproval = event.requiresApproval === true;

        this.authService.getCurrentUserId().subscribe({
          next: (currentUserId: string) => {

            this.isEventOwner = currentUserId === event.ownerUserId;

            if (!this.isEventOwner && event.collaborators) {

              let collaborator = event.collaborators.find(
                (c: any) => c.userId === currentUserId
              );

              if (collaborator) {
                this.isCollaborator = true;
                this.canUploadMedia = collaborator.canUploadMedia === true;
                this.canDirectUpload = collaborator.canDirectUpload === true;
                this.canDeleteMedia = collaborator.canDeleteMedia === true;
                this.canReviewMedia = collaborator.canReviewMedia === true;
                this.canReviewAccessRequests = collaborator.canReviewAccessRequests === true;
                this.canEditEventDetails = collaborator.canEditEventDetails === true;
              }

            } else if (this.isEventOwner) {

              this.isCollaborator = true;
              this.canUploadMedia = true;
              this.canDirectUpload = true;
              this.canDeleteMedia = true;
              this.canReviewMedia = true;
              this.canReviewAccessRequests = true;
              this.canEditEventDetails = true;
            }

            if (!this.requiresApproval) {
              this.loadEventMedia(eventId);
            } else {
              this.isLoading = false;
            }
          },

          error: () => {
            this.isLoading = false;
          },
        });
      },

      error: () => {
        this.isLoading = false;
      },
    });
  }

  loadEventMedia(eventId: string): void {
    this.eventService.loadEventMedia(eventId).subscribe({
      next: () => {
        this.isLoading = false;

        try {
          this.cdr.detectChanges();
        } catch {}
      },

      error: () => {
        this.isLoading = false;

        try {
          this.cdr.detectChanges();
        } catch {}
      },
    });
  }

  onUploadComplete(): void {
    this.loadEventMedia(this.eventId);
  }

  onMediaUpdated(): void {
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

        alert(
          'Failed to update event: ' +
          (error.error?.error || error.message)
        );
      },
    });
  }

  deleteEvent(): void {
    if (!confirm('Are you sure you want to delete this event?')) {
      return;
    }

    this.api.deleteEvent(this.eventId).subscribe({
      next: () => {
        alert('Event deleted successfully');
        this.router.navigate(['/event']);
      },

      error: (error) => {
        alert(
          'Failed to delete event: ' +
          (error.error?.error || error.message)
        );
      },
    });
  }
}