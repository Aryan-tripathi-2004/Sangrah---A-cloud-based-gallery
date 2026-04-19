import { Component, Input, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SangrahApiService } from '../../../core/api/sangrah-api.service';

interface EventCollaborator {
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
  imports: [CommonModule, FormsModule],
  templateUrl: './event-collaborators.component.html',
  styles: []
})
export class EventCollaboratorsComponent implements OnInit {
  @Input() eventId!: string;

  private api = inject(SangrahApiService);
  private cdr = inject(ChangeDetectorRef);

  currentTab: 'collaborators' | 'add' = 'collaborators';
  collaborators: EventCollaborator[] = [];
  
  searchQuery = '';
  stagedUserEmail = '';
  isAdding = false;

  newCollaboratorEmail = '';
  newCollaboratorPermissions = {
    canUploadMedia: true,
    canReviewMedia: false,
    canReviewAccessRequests: false,
    canDirectUpload: false,
    canDeleteMedia: false,
    canEditEventDetails: false
  };

  ngOnInit() {
    this.loadCollaborators();
  }

  loadCollaborators() {
    this.api.getEventCollaborators(this.eventId).subscribe({
      next: (data: any) => {
        if (Array.isArray(data)) {
          this.collaborators = data.map(c => ({...c, showPermissions: false}));
        } else if (data && data.collaborators) {
          this.collaborators = data.collaborators.map((c: any) => ({...c, showPermissions: false}));
        } else {
          this.collaborators = [];
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('❌ Failed to load collaborators:', err);
        this.collaborators = [];
      }
    });
  }

  startAddingUser() {
    if (!this.searchQuery || !this.searchQuery.includes('@')) return;
    this.stagedUserEmail = this.searchQuery;
    this.newCollaboratorEmail = this.searchQuery;
    
    // Default: Can Upload Media checked
    this.newCollaboratorPermissions = {
      canUploadMedia: true,
      canReviewMedia: false,
      canReviewAccessRequests: false,
      canDirectUpload: false,
      canDeleteMedia: false,
      canEditEventDetails: false
    };
  }

  resetForm() {
    this.searchQuery = '';
    this.stagedUserEmail = '';
    this.newCollaboratorEmail = '';
  }

  addCollaborator() {
    if (!this.newCollaboratorEmail) return;

    this.isAdding = true;
    const payload = {
      userEmail: this.newCollaboratorEmail,
      permissions: {
        canUploadMedia: this.newCollaboratorPermissions.canUploadMedia,
        canReviewMedia: this.newCollaboratorPermissions.canReviewMedia,
        canReviewAccessRequests: this.newCollaboratorPermissions.canReviewAccessRequests,
        canDirectUpload: this.newCollaboratorPermissions.canDirectUpload,
        canDeleteMedia: this.newCollaboratorPermissions.canDeleteMedia,
        canEditEventDetails: this.newCollaboratorPermissions.canEditEventDetails
      }
    };

    this.api.addEventCollaborator(this.eventId, payload).subscribe({
      next: (response) => {
        // Ensure UI moves to the collaborators tab first, then refresh list.
        this.currentTab = 'collaborators';
        this.isAdding = false;
        this.resetForm();
        this.loadCollaborators();
        this.cdr.detectChanges();

        // Extra safety: ensure change detection runs in next tick so any
        // asynchronous template updates settle and the Add form is hidden.
        setTimeout(() => {
          this.currentTab = 'collaborators';
          this.cdr.detectChanges();
        }, 0);
      },
      error: (err) => {
        console.error('❌ [Add Collaborator] Failed:', err);
        alert('Failed to add collaborator. Verify email matches an existing user.');
        this.isAdding = false;
        this.cdr.detectChanges();
      }
    });
  }

  removeCollaborator(userId: string) {
    if (!userId || userId === 'null' || userId === '') {
      alert('Cannot remove this collaborator entry - corrupted data detected.');
      this.loadCollaborators();
      return;
    }

    if (confirm('Remove this collaborator?')) {
      this.api.removeEventCollaborator(this.eventId, userId).subscribe({
        next: () => {
          this.loadCollaborators();
        },
        error: (err) => {
          console.error('❌ Failed to remove collaborator:', err);
        }
      });
    }
  }

  updatePermissions(userId: string, collaborator: EventCollaborator) {
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

    this.api.updateCollaboratorPermissions(this.eventId, userId, payload).subscribe({
      next: () => {
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('❌ Failed to update permissions:', err);
        this.loadCollaborators();
      }
    });
  }
}
