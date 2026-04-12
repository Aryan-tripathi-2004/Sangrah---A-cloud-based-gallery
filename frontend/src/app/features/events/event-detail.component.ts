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
  template: `
    <app-layout>
      <div class="space-y-8">
        <!-- Back Button -->
        <a routerLink="/event" class="inline-flex items-center gap-2 text-blue-400 hover:text-blue-300 transition">
          &larr; Back to Events
        </a>

        <!-- Event Header -->
        <div *ngIf="event && !isLoading" class="bg-gradient-to-r from-blue-600/20 to-purple-600/20 border border-blue-500/30 rounded-2xl p-8">
          <div class="flex items-start justify-between mb-6">
            <div>
              <h1 class="text-4xl font-bold mb-2">{{ event.title }}</h1>
              <p class="text-slate-300">{{ event.description }}</p>
            </div>
            <div class="text-right space-y-3">
              <!-- visibility badge -->
              <span class="px-3 py-1 rounded-full text-sm font-semibold block" [ngClass]="{'bg-green-500/20 text-green-400': event.visibility === 'PUBLIC', 'bg-yellow-500/20 text-yellow-400': event.visibility === 'PROTECTED', 'bg-red-500/20 text-red-400': event.visibility === 'PRIVATE'}">
                {{ event.visibility }}
              </span>
              
              <!-- Action Buttons (Owner or canEditEventDetails) -->
              <div *ngIf="isEventOwner || canEditEventDetails" class="flex gap-2">
                <button
                  (click)="openEditEventModal()"
                  class="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg font-semibold transition text-sm">
                  Edit Event
                </button>
                <button
                  *ngIf="isEventOwner"
                  (click)="deleteEvent()"
                  class="px-4 py-2 bg-red-600 hover:bg-red-700 rounded-lg font-semibold transition text-sm">
                  Delete Event
                </button>
              </div>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <p class="text-slate-400 text-sm">Event Date</p>
              <p class="text-lg font-semibold">{{ event.eventDate | date:'MMM d, y' }}</p>
            </div>
            <div>
              <p class="text-slate-400 text-sm">Status</p>
              <p class="text-lg font-semibold capitalize">{{ event.status }}</p>
            </div>
            <div>
              <p class="text-slate-400 text-sm">Created</p>
              <p class="text-lg font-semibold">{{ event.createdAt | date:'MMM d, y' }}</p>
            </div>
          </div>

          <!-- Moderation Badge -->
          <div class="mt-6 flex items-center gap-2">
            <span *ngIf="event.moderationEnabled" class="px-3 py-1 bg-yellow-500/20 border border-yellow-500/30 rounded-full text-xs font-semibold text-yellow-400">
              Moderation Enabled
            </span>
            <span *ngIf="!event.moderationEnabled" class="px-3 py-1 bg-green-500/20 border border-green-500/30 rounded-full text-xs font-semibold text-green-400">
              Auto-approved uploads
            </span>
          </div>
        </div>

        <!-- Tabs (only show if user has approval) -->
        <div *ngIf="event && !requiresApproval" class="flex gap-4 border-b border-slate-800 overflow-x-auto">
          <button
            (click)="currentTab = 'timeline'"
            [class.border-b-2]="currentTab === 'timeline'"
            [class.border-blue-500]="currentTab === 'timeline'"
            class="px-4 py-3 font-semibold transition hover:text-slate-100 whitespace-nowrap"
          >
            Timeline
          </button>
          <button
            *ngIf="isEventOwner || canUploadMedia || canDirectUpload || event?.visibility === 'PUBLIC'"
            (click)="currentTab = 'upload'"
            [class.border-b-2]="currentTab === 'upload'"
            [class.border-blue-500]="currentTab === 'upload'"
            class="px-4 py-3 font-semibold transition hover:text-slate-100 whitespace-nowrap"
          >
            Upload
          </button>
          <button
            *ngIf="isEventOwner || canReviewMedia"
            (click)="currentTab = 'approvals'"
            [class.border-b-2]="currentTab === 'approvals'"
            [class.border-blue-500]="currentTab === 'approvals'"
            class="px-4 py-3 font-semibold transition hover:text-slate-100 whitespace-nowrap"
          >
            Approvals
          </button>

          <!-- NEW: Owner or Collaborator tabs for collaborators, policies, and access requests -->
          <button
            *ngIf="isEventOwner || isCollaborator"
            (click)="currentTab = 'collaborators'"
            [class.border-b-2]="currentTab === 'collaborators'"
            [class.border-blue-500]="currentTab === 'collaborators'"
            class="px-4 py-3 font-semibold transition hover:text-slate-100 whitespace-nowrap"
          >
            Collaborators
          </button>
          <!-- Policies tab removed -->
          <button
            *ngIf="(isEventOwner || canReviewAccessRequests) && event?.visibility === 'PROTECTED'"
            (click)="currentTab = 'access-requests'"
            [class.border-b-2]="currentTab === 'access-requests'"
            [class.border-blue-500]="currentTab === 'access-requests'"
            class="px-4 py-3 font-semibold transition hover:text-slate-100 whitespace-nowrap"
          >
            Access Requests ({{ pendingAccessCount }})
          </button>
        </div>

        <!-- Timeline Tab -->
        <div *ngIf="currentTab === 'timeline' && !requiresApproval">
          <app-event-timeline [eventId]="eventId"></app-event-timeline>
        </div>

        <!-- Upload Tab -->
        <div *ngIf="currentTab === 'upload' && !requiresApproval && (isEventOwner || canUploadMedia || canDirectUpload || event?.visibility === 'PUBLIC')">
          <app-event-media-upload
            [eventId]="eventId"
            [isOwner]="isEventOwner"
            [isModerated]="event?.moderationEnabled || false"
            (uploadComplete)="onUploadComplete()"
          ></app-event-media-upload>
        </div>

        <!-- Approvals Tab (Owner or canReviewMedia permission) -->
        <div *ngIf="currentTab === 'approvals' && (isEventOwner || canReviewMedia) && !requiresApproval">
          <app-event-media-approval
            [eventId]="eventId"
            [isOwner]="isEventOwner"
            (mediaUpdated)="onMediaUpdated()"
          ></app-event-media-approval>
        </div>

        <!-- NEW: Collaborators Tab (Owner or Collaborator) -->
        <div *ngIf="currentTab === 'collaborators' && (isEventOwner || isCollaborator) && !requiresApproval">
          <app-event-collaborators [eventId]="eventId"></app-event-collaborators>
        </div>

        <!-- Policies feature removed -->

        <!-- NEW: Access Requests Tab (Owner or canReviewAccessRequests permission, PROTECTED events) -->
        <div *ngIf="currentTab === 'access-requests' && (isEventOwner || canReviewAccessRequests) && !requiresApproval">
          <app-event-access-requests [eventId]="eventId"></app-event-access-requests>
        </div>

        <!-- NEW: Access Request Component for non-owners (PROTECTED events without approval) -->
        <app-event-access-request
          *ngIf="requiresApproval && event?.visibility === 'PROTECTED' && !isEventOwner"
          [eventId]="eventId"
          [event]="event">
        </app-event-access-request>

        <!-- NEW: Access Request Component for non-owners (PROTECTED events with approval) -->
        <app-event-access-request
          *ngIf="!requiresApproval && event?.visibility === 'PROTECTED' && !isEventOwner && !event?.accessStatus"
          [eventId]="eventId"
          [event]="event">
        </app-event-access-request>

        <!-- NEW: Private Event Message for non-owners and non-collaborators -->
        <div *ngIf="event?.visibility === 'PRIVATE' && !isEventOwner && !isCollaborator"
             class="bg-red-500/10 border border-red-500/30 rounded-lg p-6 text-center">
          <p class="text-red-400 font-semibold text-lg">This is a Private Event</p>
          <p class="text-slate-400 mt-2">Only the event owner and assigned collaborators can access this event.</p>
        </div>

        <!-- Loading State -->
        <div *ngIf="isLoading" class="text-center py-16">
          <p class="text-slate-400">Loading event details...</p>
        </div>

        <!-- Edit Event Modal -->
        <div *ngIf="showEditModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div class="bg-slate-900 border border-slate-700 rounded-lg p-6 w-full max-w-2xl max-h-96 overflow-y-auto">
            <h2 class="text-2xl font-bold mb-6">Edit Event</h2>

            <div class="space-y-4">
              <!-- Title -->
              <div>
                <label class="block text-sm font-semibold mb-2">Event Title</label>
                <input
                  [(ngModel)]="editFormData.title"
                  type="text"
                  class="w-full px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-blue-500"
                />
              </div>

              <!-- Description -->
              <div>
                <label class="block text-sm font-semibold mb-2">Description</label>
                <textarea
                  [(ngModel)]="editFormData.description"
                  rows="3"
                  class="w-full px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-blue-500"
                ></textarea>
              </div>

              <!-- Event Date -->
              <div>
                <label class="block text-sm font-semibold mb-2">Event Date</label>
                <input
                  [(ngModel)]="editFormData.eventDate"
                  type="date"
                  class="w-full px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-blue-500"
                />
              </div>

              <!-- Visibility -->
              <div>
                <label class="block text-sm font-semibold mb-2">Visibility</label>
                <select
                  [(ngModel)]="editFormData.visibility"
                  class="w-full px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-blue-500"
                >
                  <option value="PUBLIC">Public</option>
                  <option value="PROTECTED">Protected</option>
                  <option value="PRIVATE">Private</option>
                </select>
              </div>

              <!-- Moderation -->
              <div class="flex items-center gap-3">
                <input
                  [(ngModel)]="editFormData.moderationEnabled"
                  type="checkbox"
                  id="moderation"
                  class="w-4 h-4"
                />
                <label for="moderation" class="text-sm font-semibold">Require moderation for uploads</label>
              </div>
            </div>

            <!-- Buttons -->
            <div class="flex gap-3 mt-6">
              <button
                (click)="saveEventChanges()"
                [disabled]="isSavingEvent"
                class="flex-1 px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-slate-600 rounded-lg font-semibold transition"
              >
                {{ isSavingEvent ? 'Saving...' : 'Save Changes' }}
              </button>
              <button
                (click)="closeEditModal()"
                class="flex-1 px-4 py-2 bg-slate-700 hover:bg-slate-600 rounded-lg font-semibold transition"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      </div>
    </app-layout>
  `,
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






