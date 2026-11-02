import { Injectable } from '@angular/core';
import { Event, EventCollaborator } from '../../core/api/models/api.models';

export interface EventPermissions {
  isEventOwner: boolean;
  isCollaborator: boolean;
  canUploadMedia: boolean;
  canDirectUpload: boolean;
  canDeleteMedia: boolean;
  canReviewMedia: boolean;
  canReviewAccessRequests: boolean;
  canEditEventDetails: boolean;
}

export const DEFAULT_EVENT_PERMISSIONS: EventPermissions = {
  isEventOwner: false,
  isCollaborator: false,
  canUploadMedia: false,
  canDirectUpload: false,
  canDeleteMedia: false,
  canReviewMedia: false,
  canReviewAccessRequests: false,
  canEditEventDetails: false,
};

@Injectable({
  providedIn: 'root'
})
export class EventPermissionsService {
  computePermissions(event: Event | null, currentUserId: string | null): EventPermissions {
    if (!event || !currentUserId) {
      return { ...DEFAULT_EVENT_PERMISSIONS };
    }

    const isOwner = currentUserId === event.ownerUserId;
    if (isOwner) {
      return {
        isEventOwner: true,
        isCollaborator: true,
        canUploadMedia: true,
        canDirectUpload: true,
        canDeleteMedia: true,
        canReviewMedia: true,
        canReviewAccessRequests: true,
        canEditEventDetails: true,
      };
    }

    const collaborator = event.collaborators?.find((c: EventCollaborator) => c.userId === currentUserId);
    if (collaborator) {
      return {
        isEventOwner: false,
        isCollaborator: true,
        canUploadMedia: collaborator.canUploadMedia === true,
        canDirectUpload: collaborator.canDirectUpload === true,
        canDeleteMedia: collaborator.canDeleteMedia === true,
        canReviewMedia: collaborator.canReviewMedia === true,
        canReviewAccessRequests: collaborator.canReviewAccessRequests === true,
        canEditEventDetails: collaborator.canEditEventDetails === true,
      };
    }

    return { ...DEFAULT_EVENT_PERMISSIONS };
  }
}
