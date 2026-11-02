import { Component, input, OnInit, inject, ChangeDetectionStrategy, DestroyRef, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { EventApiService } from '../../../core/api/services/event-api.service';

export interface EventCollaborator {
  userId: string;
  email?: string;
  displayName?: string;
  canUploadMedia: boolean;
  canReviewMedia: boolean;
  canReviewAccessRequests: boolean;
  canDirectUpload: boolean;
  canDeleteMedia: boolean;
  canEditEventDetails: boolean;
  addedAt?: string;
  addedByUserId?: string;
  showPermissions?: boolean;
}

@Component({
  selector: 'app-event-collaborators',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule],
  templateUrl: './event-collaborators.component.html',
  styles: []
})
export class EventCollaboratorsComponent implements OnInit {
  readonly eventIdInput = input.required<string>({ alias: 'eventId' });

  get eventId(): string {
    return this.eventIdInput();
  }

  private readonly api = inject(EventApiService);
  private readonly destroyRef = inject(DestroyRef);

  // Modern Signal State
  readonly currentTab = signal<'collaborators' | 'add'>('collaborators');
  readonly collaborators = signal<EventCollaborator[]>([]);
  readonly isAdding = signal<boolean>(false);

  // Form Signals
  readonly searchQuery = signal<string>('');
  readonly stagedUserEmail = signal<string>('');
  readonly newCollaboratorEmail = signal<string>('');

  newCollaboratorPermissions = {
    canUploadMedia: true,
    canReviewMedia: false,
    canReviewAccessRequests: false,
    canDirectUpload: false,
    canDeleteMedia: false,
    canEditEventDetails: false
  };

  ngOnInit(): void {
    this.loadCollaborators();
  }

  loadCollaborators(): void {
    this.api.getEventCollaborators(this.eventId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data: unknown) => {
        const obj = data as { collaborators?: EventCollaborator[] } | null;
        if (Array.isArray(data)) {
          this.collaborators.set((data as EventCollaborator[]).map(c => ({ ...c, showPermissions: false })));
        } else if (obj && Array.isArray(obj.collaborators)) {
          this.collaborators.set(obj.collaborators.map((c: EventCollaborator) => ({ ...c, showPermissions: false })));
        } else {
          this.collaborators.set([]);
        }
      },
      error: (err) => {
        console.error('❌ Failed to load collaborators:', err);
        this.collaborators.set([]);
      }
    });
  }

  startAddingUser(): void {
    const query = this.searchQuery();
    if (!query || !query.includes('@')) return;
    this.stagedUserEmail.set(query);
    this.newCollaboratorEmail.set(query);

    this.newCollaboratorPermissions = {
      canUploadMedia: true,
      canReviewMedia: false,
      canReviewAccessRequests: false,
      canDirectUpload: false,
      canDeleteMedia: false,
      canEditEventDetails: false
    };
  }

  resetForm(): void {
    this.searchQuery.set('');
    this.stagedUserEmail.set('');
    this.newCollaboratorEmail.set('');
  }

  togglePermissions(collab: EventCollaborator): void {
    this.collaborators.update(list =>
      list.map(c => c.userId === collab.userId ? { ...c, showPermissions: !c.showPermissions } : c)
    );
  }

  addCollaborator(): void {
    const email = this.newCollaboratorEmail();
    if (!email) return;

    this.isAdding.set(true);
    const payload = {
      userEmail: email,
      permissions: {
        canUploadMedia: this.newCollaboratorPermissions.canUploadMedia,
        canReviewMedia: this.newCollaboratorPermissions.canReviewMedia,
        canReviewAccessRequests: this.newCollaboratorPermissions.canReviewAccessRequests,
        canDirectUpload: this.newCollaboratorPermissions.canDirectUpload,
        canDeleteMedia: this.newCollaboratorPermissions.canDeleteMedia,
        canEditEventDetails: this.newCollaboratorPermissions.canEditEventDetails
      }
    };

    this.api.addEventCollaborator(this.eventId, payload).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.isAdding.set(false);
        this.resetForm();
        this.currentTab.set('collaborators');
        this.loadCollaborators();
      },
      error: (err) => {
        console.error('❌ [Add Collaborator] Failed:', err);
        alert('Failed to add collaborator. Verify email matches an existing user.');
        this.isAdding.set(false);
      }
    });
  }

  removeCollaborator(userId: string): void {
    if (!userId || userId === 'null' || userId === '') {
      alert('Cannot remove this collaborator entry - corrupted data detected.');
      this.loadCollaborators();
      return;
    }

    if (confirm('Remove this collaborator?')) {
      this.api.removeEventCollaborator(this.eventId, userId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => {
          this.loadCollaborators();
        },
        error: (err) => {
          console.error('❌ Failed to remove collaborator:', err);
        }
      });
    }
  }

  updatePermissions(userId: string, collaborator: EventCollaborator): void {
    if (!userId || userId === 'null' || userId === '') {
      this.loadCollaborators();
      return;
    }

    const payload = {
      permissions: {
        canUploadMedia: collaborator.canUploadMedia,
        canReviewMedia: collaborator.canReviewMedia,
        canReviewAccessRequests: collaborator.canReviewAccessRequests,
        canDirectUpload: collaborator.canDirectUpload,
        canDeleteMedia: collaborator.canDeleteMedia,
        canEditEventDetails: collaborator.canEditEventDetails
      }
    };

    this.api.updateCollaboratorPermissions(this.eventId, userId, payload).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      error: (err) => {
        console.error('❌ Failed to update permissions:', err);
        this.loadCollaborators();
      }
    });
  }
}
