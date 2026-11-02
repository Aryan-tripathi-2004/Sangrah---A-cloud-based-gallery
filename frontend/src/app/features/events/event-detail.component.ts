import { Component, OnDestroy, OnInit, inject, ChangeDetectionStrategy, DestroyRef, signal, computed, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { of } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';
import { EventApiService } from '../../core/api/services/event-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { Event } from '../../core/api/models/api.models';
import { EventService } from './event.service';
import { EventPermissionsService, EventPermissions, DEFAULT_EVENT_PERMISSIONS } from './event-permissions.service';
import { extractErrorMessage } from '../../core/errors/api-error.model';

import { LayoutComponent } from '../../shared/layout/layout.component';
import { EventMediaUploadComponent } from './components/event-media-upload.component';
import { EventMediaApprovalComponent } from './components/event-media-approval.component';
import { EventTimelineComponent } from './components/event-timeline.component';
import { EventAccessRequestComponent } from './components/event-access-request.component';
import { EventAccessRequestsComponent } from './components/event-access-requests.component';
import { EventCollaboratorsComponent } from './components/event-collaborators.component';
import { EditEventModalComponent } from '../../shared/components/edit-event-modal.component';
import { DeleteEventModalComponent } from '../../shared/components/delete-event-modal.component';
import { SuccessModalComponent } from '../../shared/components/success-modal.component';
import { ErrorModalComponent } from '../../shared/components/error-modal.component';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
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
  private readonly api = inject(EventApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly eventService = inject(EventService);
  private readonly permissionsService = inject(EventPermissionsService);
  private readonly ngZone = inject(NgZone);

  event = signal<Event | null>(null);
  eventId = signal<string>('');
  isLoading = signal<boolean>(true);
  requiresApproval = signal<boolean>(false);
  currentTab = signal<string>('timeline');

  // Permissions state managed by EventPermissionsService
  readonly permissions = signal<EventPermissions>(DEFAULT_EVENT_PERMISSIONS);
  readonly isEventOwner = computed(() => this.permissions().isEventOwner);
  readonly isCollaborator = computed(() => this.permissions().isCollaborator);
  readonly canUploadMedia = computed(() => this.permissions().canUploadMedia);
  readonly canDirectUpload = computed(() => this.permissions().canDirectUpload);
  readonly canDeleteMedia = computed(() => this.permissions().canDeleteMedia);
  readonly canReviewMedia = computed(() => this.permissions().canReviewMedia);
  readonly canReviewAccessRequests = computed(() => this.permissions().canReviewAccessRequests);
  readonly canEditEventDetails = computed(() => this.permissions().canEditEventDetails);

  pendingAccessCount = signal<number>(0);

  showEditModal = signal<boolean>(false);
  showDeleteModal = signal<boolean>(false);
  showSuccessModal = signal<boolean>(false);
  showErrorModal = signal<boolean>(false);

  successTitle = signal<string>('');
  successMessage = signal<string>('');
  successCountdownSeconds = signal<number | null>(null);
  successContext = signal<'edit' | 'delete' | null>(null);

  errorTitle = signal<string>('Action failed');
  errorMessage = signal<string>('');
  lastFailedAction = signal<'save' | 'delete' | null>(null);

  isDeletingEvent = signal<boolean>(false);
  private deleteCountdownTimer: ReturnType<typeof setInterval> | null = null;

  collaboratorCount = computed(() => this.event()?.collaborators?.length ?? 0);
  mediaCount = computed(() => this.eventService.getEventMediaValue().length);

  currentCoverImageUrl = computed(() => {
    const e = this.event();
    return e?.coverImageId ? this.api.getMediaFileUrl(e.coverImageId) : null;
  });

  constructor() {
    this.destroyRef.onDestroy(() => {
      this.clearDeleteCountdown();
    });
  }

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const id = params.get('id');
      if (id) {
        this.eventId.set(id);
        this.loadEvent(id);
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
    this.isLoading.set(true);

    this.api.getEventById(eventId)
      .pipe(
        switchMap((event: Event) => {
          this.event.set(event);
          this.requiresApproval.set(event.requiresApproval === true);
          this.pendingAccessCount.set(0);

          return this.authService.getCurrentUserId().pipe(
            catchError(() => of('')),
            switchMap((currentUserId: string) => {
              const perms = currentUserId
                ? this.permissionsService.computePermissions(event, currentUserId)
                : DEFAULT_EVENT_PERMISSIONS;
              this.permissions.set(perms);

              if (!this.requiresApproval()) {
                return this.eventService.loadEventMedia(eventId);
              }
              return of([]);
            })
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: () => this.isLoading.set(false),
        error: () => this.isLoading.set(false),
      });
  }

  loadEventMedia(eventId: string): void {
    this.eventService.loadEventMedia(eventId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.isLoading.set(false),
        error: () => this.isLoading.set(false),
      });
  }

  onUploadComplete(): void {
    this.loadEventMedia(this.eventId());
  }

  onMediaUpdated(): void {
    this.loadEventMedia(this.eventId());
  }

  openEditEventModal(): void {
    this.showEditModal.set(true);
    this.showDeleteModal.set(false);
    this.showErrorModal.set(false);
    this.showSuccessModal.set(false);
  }

  closeEditModal(): void {
    this.showEditModal.set(false);
  }

  onEventSaved(updatedEvent: Event): void {
    const ev = this.event();
    this.event.set({
      ...ev,
      ...updatedEvent,
    });
    this.showEditModal.set(false);
    this.showErrorModal.set(false);
    this.showSuccessModal.set(true);
    this.successContext.set('edit');
    this.successTitle.set('Event updated');
    this.successMessage.set('The event details were saved successfully.');
    this.successCountdownSeconds.set(null);
    this.loadEvent(this.eventId());
  }

  onEventSaveError(errorMsg: string): void {
    this.showEditModal.set(true);
    this.showError('Unable to save changes', `Failed to update event: ${errorMsg}`, 'save');
  }

  openDeleteModal(): void {
    if (!this.isEventOwner() || !this.event()) {
      return;
    }
    this.showDeleteModal.set(true);
    this.showErrorModal.set(false);
    this.showSuccessModal.set(false);
  }

  closeDeleteModal(): void {
    if (this.isDeletingEvent()) {
      return;
    }
    this.showDeleteModal.set(false);
  }

  confirmDeleteEvent(): void {
    const id = this.eventId();
    if (!id || !this.isEventOwner() || this.isDeletingEvent()) {
      return;
    }

    this.isDeletingEvent.set(true);

    this.api.deleteEvent(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isDeletingEvent.set(false);
          this.showDeleteModal.set(false);
          this.showErrorModal.set(false);
          this.showSuccessModal.set(true);
          this.successContext.set('delete');
          this.successTitle.set('Event deleted');
          this.successMessage.set('The event has been deleted. You will be redirected to the events list.');
          this.successCountdownSeconds.set(3);

          this.startDeleteCountdown();
        },
        error: (error) => {
          this.isDeletingEvent.set(false);
          this.showError('Delete failed', `Failed to delete event: ${this.getErrorMessage(error)}`, 'delete');
        },
      });
  }

  closeSuccessModal(): void {
    if (this.successContext() === 'delete') {
      this.clearDeleteCountdown();
      this.navigateToEvents();
      return;
    }

    this.showSuccessModal.set(false);
    this.successContext.set(null);
    this.successCountdownSeconds.set(null);
  }

  closeErrorModal(): void {
    this.showErrorModal.set(false);
    this.lastFailedAction.set(null);
  }

  retryLastAction(): void {
    const action = this.lastFailedAction();
    this.closeErrorModal();

    if (action === 'save') {
      this.showEditModal.set(true);
    } else if (action === 'delete') {
      this.openDeleteModal();
    }
  }

  private showError(title: string, message: string, action: 'save' | 'delete'): void {
    this.errorTitle.set(title);
    this.errorMessage.set(message);
    this.lastFailedAction.set(action);
    this.showErrorModal.set(true);
  }

  private getErrorMessage(error: unknown): string {
    return extractErrorMessage(error, 'Unknown error');
  }

  private startDeleteCountdown(): void {
    this.clearDeleteCountdown();

    this.ngZone.runOutsideAngular(() => {
      this.deleteCountdownTimer = setInterval(() => {
        const current = this.successCountdownSeconds();
        if (current === null) {
          this.clearDeleteCountdown();
          return;
        }

        const next = current - 1;
        this.ngZone.run(() => {
          this.successCountdownSeconds.set(next);

          if (next <= 0) {
            this.clearDeleteCountdown();
            this.navigateToEvents();
          }
        });
      }, 1000);
    });
  }

  private clearDeleteCountdown(): void {
    if (this.deleteCountdownTimer) {
      clearInterval(this.deleteCountdownTimer);
      this.deleteCountdownTimer = null;
    }
  }

  private navigateToEvents(): void {
    this.showSuccessModal.set(false);
    this.successContext.set(null);
    this.successCountdownSeconds.set(null);
    this.router.navigate(['/events']);
  }
}
