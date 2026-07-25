import { Component, OnDestroy, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, FormGroup } from '@angular/forms';
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
import { EditEventModalComponent } from '../../shared/components/edit-event-modal.component';
import { DeleteEventModalComponent } from '../../shared/components/delete-event-modal.component';
import { SuccessModalComponent } from '../../shared/components/success-modal.component';
import { ErrorModalComponent } from '../../shared/components/error-modal.component';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    LayoutComponent,
    EventMediaUploadComponent,
    EventMediaApprovalComponent,
    EventTimelineComponent,
    EventAccessRequestComponent,
    EventAccessRequestsComponent,
    EventCollaboratorsComponent,
    EditEventModalComponent,
    DeleteEventModalComponent,
    SuccessModalComponent,
    ErrorModalComponent,
  ],
  templateUrl: './event-detail.component.html',
})
export class EventDetailComponent implements OnInit, OnDestroy {
  private api = inject(SangrahApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);
  private eventService = inject(EventService);
  private cdr = inject(ChangeDetectorRef);
  private fb = inject(FormBuilder);

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

  showEditModal = false;
  showDeleteModal = false;
  showSuccessModal = false;
  showErrorModal = false;

  successTitle = '';
  successMessage = '';
  successCountdownSeconds: number | null = null;
  successContext: 'edit' | 'delete' | null = null;

  errorTitle = 'Action failed';
  errorMessage = '';
  lastFailedAction: 'save' | 'delete' | null = null;

  isSavingEvent = false;
  isDeletingEvent = false;
  private deleteCountdownTimer: ReturnType<typeof setInterval> | null = null;
  selectedCoverImageFile: File | null = null;
  selectedCoverImagePreviewUrl: string | null = null;
  selectedCoverImageFileName: string | null = null;

  editForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
    description: ['', [Validators.maxLength(2000)]],
    eventDate: ['', [Validators.required]],
    visibility: ['PUBLIC', [Validators.required]],
    moderationEnabled: [false],
  });

  private editFormSnapshot = '';

  get collaboratorCount(): number {
    return this.event?.collaborators?.length ?? 0;
  }

  get mediaCount(): number {
    return this.eventService.getEventMediaValue().length;
  }

  get hasEditChanges(): boolean {
    return JSON.stringify(this.normalizeEditFormValue(this.editForm.getRawValue())) !== this.editFormSnapshot || !!this.selectedCoverImageFile;
  }

  get canSaveEditChanges(): boolean {
    return this.editForm.valid && this.hasEditChanges && !this.isSavingEvent;
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const eventId = params.get('id');

      if (eventId) {
        this.eventId = eventId;
        this.loadEvent(eventId);
      }
    });
  }

  ngOnDestroy(): void {
    this.clearDeleteCountdown();
  }

  getCoverImageUrl(mediaId: string): string {
    return this.api.getMediaFileUrl(mediaId);
  }

  loadEvent(eventId: string): void {
    this.isLoading = true;
    this.api.getEventById(eventId).subscribe({
      next: (event: any) => {
        this.event = event;

        this.requiresApproval = event.requiresApproval === true;
        this.pendingAccessCount = 0;
        this.isEventOwner = false;
        this.isCollaborator = false;
        this.canUploadMedia = false;
        this.canDirectUpload = false;
        this.canDeleteMedia = false;
        this.canReviewMedia = false;
        this.canReviewAccessRequests = false;
        this.canEditEventDetails = false;

        this.authService.getCurrentUserId().subscribe({
          next: (currentUserId: string) => {
            this.applyPermissions(event, currentUserId);

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

  private applyPermissions(event: Event, currentUserId: string): void {
    this.isEventOwner = currentUserId === event.ownerUserId;

    if (this.isEventOwner) {
      this.isCollaborator = true;
      this.canUploadMedia = true;
      this.canDirectUpload = true;
      this.canDeleteMedia = true;
      this.canReviewMedia = true;
      this.canReviewAccessRequests = true;
      this.canEditEventDetails = true;
      return;
    }

    const collaborator = event.collaborators?.find((c: any) => c.userId === currentUserId);

    if (collaborator) {
      this.isCollaborator = true;
      this.canUploadMedia = collaborator.canUploadMedia === true;
      this.canDirectUpload = collaborator.canDirectUpload === true;
      this.canDeleteMedia = collaborator.canDeleteMedia === true;
      this.canReviewMedia = collaborator.canReviewMedia === true;
      this.canReviewAccessRequests = collaborator.canReviewAccessRequests === true;
      this.canEditEventDetails = collaborator.canEditEventDetails === true;
    }
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
      this.editForm.reset({
        title: this.event.title,
        description: this.event.description || '',
        eventDate: this.formatDateForInput(this.event.eventDate),
        visibility: this.event.visibility,
        moderationEnabled: this.event.moderationEnabled || false,
      });

      this.editForm.markAsPristine();
      this.editForm.markAsUntouched();
      this.editFormSnapshot = JSON.stringify(this.normalizeEditFormValue(this.editForm.getRawValue()));
      this.resetSelectedCoverImage();

      this.showEditModal = true;
      this.showDeleteModal = false;
      this.showErrorModal = false;
      this.showSuccessModal = false;
    }
  }

  closeEditModal(): void {
    if (this.isSavingEvent) {
      return;
    }

    this.showEditModal = false;
  }

  onCoverFileSelected(file: File | null): void {
    this.selectedCoverImageFile = file;
    this.selectedCoverImageFileName = file?.name ?? null;

    if (this.selectedCoverImagePreviewUrl) {
      URL.revokeObjectURL(this.selectedCoverImagePreviewUrl);
      this.selectedCoverImagePreviewUrl = null;
    }

    if (file) {
      this.selectedCoverImagePreviewUrl = URL.createObjectURL(file);
    }
  }

  saveEventChanges(): void {
    if (!this.eventId || !this.event || this.isSavingEvent) {
      return;
    }

    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    if (!this.hasEditChanges) {
      return;
    }

    const formValue = this.normalizeEditFormValue(this.editForm.getRawValue());
    const eventDetails = {
      title: formValue.title,
      description: formValue.description,
      eventDate: formValue.eventDate,
      visibility: formValue.visibility,
      moderationEnabled: formValue.moderationEnabled,
    };

    this.isSavingEvent = true;

    this.api.updateEvent(this.eventId, eventDetails, this.selectedCoverImageFile || undefined).subscribe({
      next: (updatedEvent: any) => {
        this.event = {
          ...this.event,
          ...updatedEvent,
        } as Event;

        this.showEditModal = false;
        this.isSavingEvent = false;
        this.resetSelectedCoverImage();
        this.showErrorModal = false;
        this.showSuccessModal = true;
        this.successContext = 'edit';
        this.successTitle = 'Event updated';
        this.successMessage = 'The event details were saved successfully.';
        this.successCountdownSeconds = null;

        this.loadEvent(this.eventId);
      },

      error: (error) => {
        this.isSavingEvent = false;
        this.showEditModal = true;
        this.showError('Unable to save changes', `Failed to update event: ${this.getErrorMessage(error)}`, 'save');
        this.cdr.detectChanges();
      },
    });
  }

  openDeleteModal(): void {
    if (!this.isEventOwner || !this.event) {
      return;
    }

    this.showDeleteModal = true;
    this.showErrorModal = false;
    this.showSuccessModal = false;
  }

  closeDeleteModal(): void {
    if (this.isDeletingEvent) {
      return;
    }

    this.showDeleteModal = false;
  }

  confirmDeleteEvent(): void {
    if (!this.eventId || !this.isEventOwner || this.isDeletingEvent) {
      return;
    }

    this.isDeletingEvent = true;

    this.api.deleteEvent(this.eventId).subscribe({
      next: () => {
        this.isDeletingEvent = false;
        this.showDeleteModal = false;
        this.showSuccessModal = true;
        this.successContext = 'delete';
        this.successTitle = 'Event deleted';
        this.successMessage = 'The event has been deleted. You will be redirected to the events list.';
        this.successCountdownSeconds = 3;

        this.startDeleteCountdown();
      },

      error: (error) => {
        this.isDeletingEvent = false;
        this.showError('Delete failed', `Failed to delete event: ${this.getErrorMessage(error)}`, 'delete');
      },
    });
  }

  closeSuccessModal(): void {
    if (this.successContext === 'delete') {
      this.clearDeleteCountdown();
      this.navigateToEvents();
      return;
    }

    this.showSuccessModal = false;
    this.successContext = null;
    this.successCountdownSeconds = null;
  }

  retryLastAction(): void {
    if (this.lastFailedAction === 'save') {
      this.showErrorModal = false;
      this.saveEventChanges();
      return;
    }

    if (this.lastFailedAction === 'delete') {
      this.showErrorModal = false;
      this.confirmDeleteEvent();
    }
  }

  closeErrorModal(): void {
    this.showErrorModal = false;
  }

  private showError(title: string, message: string, action: 'save' | 'delete'): void {
    this.errorTitle = title;
    this.errorMessage = message;
    this.lastFailedAction = action;
    this.showErrorModal = true;
  }

  private normalizeEditFormValue(value: any): {
    title: string;
    description: string;
    eventDate: string;
    visibility: string;
    moderationEnabled: boolean;
  } {
    return {
      title: (value?.title ?? '').trim(),
      description: (value?.description ?? '').trim(),
      eventDate: value?.eventDate ?? '',
      visibility: value?.visibility ?? 'PUBLIC',
      moderationEnabled: !!value?.moderationEnabled,
    };
  }

  getCurrentCoverImageUrl(): string | null {
    return this.event?.coverImageId ? this.api.getMediaFileUrl(this.event.coverImageId) : null;
  }

  private resetSelectedCoverImage(): void {
    if (this.selectedCoverImagePreviewUrl) {
      URL.revokeObjectURL(this.selectedCoverImagePreviewUrl);
    }

    this.selectedCoverImageFile = null;
    this.selectedCoverImagePreviewUrl = null;
    this.selectedCoverImageFileName = null;
  }

  private formatDateForInput(eventDate: string | null | undefined): string {
    if (!eventDate) {
      return '';
    }

    const parsed = new Date(eventDate);
    if (Number.isNaN(parsed.getTime())) {
      return '';
    }

    const year = parsed.getFullYear();
    const month = `${parsed.getMonth() + 1}`.padStart(2, '0');
    const day = `${parsed.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private getErrorMessage(error: any): string {
    return error?.error?.error || error?.error?.message || error?.message || 'Unknown error';
  }

  private startDeleteCountdown(): void {
    this.clearDeleteCountdown();

    this.deleteCountdownTimer = setInterval(() => {
      if (this.successCountdownSeconds === null) {
        return;
      }

      this.successCountdownSeconds -= 1;

      if (this.successCountdownSeconds <= 0) {
        this.clearDeleteCountdown();
        this.navigateToEvents();
      }
    }, 1000);
  }

  private clearDeleteCountdown(): void {
    if (this.deleteCountdownTimer) {
      clearInterval(this.deleteCountdownTimer);
      this.deleteCountdownTimer = null;
    }
  }

  private navigateToEvents(): void {
    this.showSuccessModal = false;
    this.successContext = null;
    this.successCountdownSeconds = null;
    this.router.navigate(['/events']);
  }
}