import { Component, Input, Output, EventEmitter, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EventService, EventMediaStatus } from '../event.service';
import { SangrahApiService, MediaItem } from '../../../core/api/sangrah-api.service';
import { MediaPreviewModalComponent } from '../../gallery/components/media-preview-modal.component';

interface UploaderGroup {
  uploaderId: string;
  uploaderName?: string;
  pendingCount: number;
  approvedCount: number;
  rejectedCount: number;
  mediaList: EventMediaStatus[];
}

@Component({
  selector: 'app-event-media-approval',
  standalone: true,
  imports: [CommonModule, FormsModule, MediaPreviewModalComponent],
  templateUrl: './event-media-approval.component.html',
})
export class EventMediaApprovalComponent implements OnInit {
  @Input() eventId!: string;
  @Input() isOwner = false;
  @Output() mediaUpdated = new EventEmitter<void>();

  private eventService = inject(EventService);
  private api = inject(SangrahApiService);

  selectedUploader: UploaderGroup | null = null;
  filterStatus: 'all' | 'PENDING' | 'APPROVED' | 'REJECTED' = 'PENDING';
  mediaFilterStatus: 'all' | 'PENDING' | 'APPROVED' | 'REJECTED' = 'all';
  rejectFormId: string | null = null;
  rejectionReason = '';
  processingId: string | null = null;
  uploaderGroups: UploaderGroup[] = [];

  // Preview modal state
  previewOpen = false;
  previewMedia: MediaItem | null = null;
  previewFileUrl: string | null = null;
  // Token readiness for media URLs
  tokenReady = false;

  ngOnInit(): void {
    this.groupMediaByUploader();
  }

  private getTokenExpiryMillis(token: string): number | null {
    try {
      const payload = token.split('.')[1];
      if (!payload) return null;
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
      const decoded = JSON.parse(atob(padded));
      if (typeof decoded.exp !== 'number') return null;
      return decoded.exp * 1000;
    } catch {
      return null;
    }
  }

  /**
   * Refresh media from API and update UI
   * This ensures changes from approval/rejection are reflected in real-time
   */
  private refreshMediaAndUI(): void {
    this.eventService.loadEventMedia(this.eventId).subscribe({
      next: () => {
        this.groupMediaByUploader();
        // If an uploader was selected, update it with fresh data
        if (this.selectedUploader) {
          const uploaderId = this.selectedUploader.uploaderId;
          const updated = this.uploaderGroups.find(g => g.uploaderId === uploaderId);
          if (updated) {
            this.selectedUploader = updated;
          }
        }
      },
      error: (error) => {
        console.error('Failed to refresh media:', error);
        // Still try to refresh UI with cached data
        this.groupMediaByUploader();
      },
    });
  }

  /**
   * Get display name for uploader, safely handling undefined cases
   */
  getUploaderDisplayName(group: UploaderGroup | null): string {
    if (!group) return 'Unknown';
    // Return name if available, otherwise return userId (never empty)
    return group.uploaderName && group.uploaderName.trim() ? group.uploaderName : group.uploaderId;
  }

  private groupMediaByUploader(): void {
    const items = this.eventService.getEventMediaValue();
    const grouped = new Map<string, UploaderGroup>();

    items.forEach((media) => {
      const uploaderId = media.uploaderId || 'unknown';
      if (!grouped.has(uploaderId)) {
        grouped.set(uploaderId, {
          uploaderId,
          uploaderName: media.uploaderName,
          pendingCount: 0,
          approvedCount: 0,
          rejectedCount: 0,
          mediaList: [],
        });
      }

      const group = grouped.get(uploaderId)!;
      group.mediaList.push(media);
      
      if (media.status === 'PENDING') group.pendingCount++;
      else if (media.status === 'APPROVED') group.approvedCount++;
      else if (media.status === 'REJECTED') group.rejectedCount++;
    });

    this.uploaderGroups = Array.from(grouped.values());
  }

  getFilteredUploaders(): UploaderGroup[] {
    return this.uploaderGroups.filter((group) => {
      if (this.filterStatus === 'all') return true;
      if (this.filterStatus === 'PENDING') return group.pendingCount > 0;
      if (this.filterStatus === 'APPROVED') return group.approvedCount > 0 && group.pendingCount === 0;
      if (this.filterStatus === 'REJECTED') return group.rejectedCount > 0;
      return true;
    });
  }

  getFilteredMediaFromUploader(): EventMediaStatus[] {
    if (!this.selectedUploader) return [];
    
    if (this.mediaFilterStatus === 'all') {
      return this.selectedUploader.mediaList;
    }
    return this.selectedUploader.mediaList.filter((m) => m.status === this.mediaFilterStatus);
  }

  get pendingCount(): number {
    return this.uploaderGroups.reduce((sum, group) => sum + group.pendingCount, 0);
  }

  isImageFile(media: EventMediaStatus): boolean {
    const mimeType = media.mimeType || '';
    return mimeType.startsWith('image/');
  }

  isVideoFile(media: EventMediaStatus): boolean {
    const mimeType = media.mimeType || '';
    return mimeType.startsWith('video/');
  }

  getMediaUrl(media: EventMediaStatus): string {
    // Return the media file URL using the event media file endpoint
    return this.api.getEventMediaFileUrl(this.eventId, media.mediaId);
  }

  openPreview(media: EventMediaStatus): void {
    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;

    const proceed = () => {
      // Create a MediaItem-like object from EventMediaStatus for the preview modal
      const item: MediaItem = {
        id: media.mediaId,
        originalFileName: media.fileName || media.mediaId,
        mimeType: media.mimeType as string | undefined,
        sizeBytes: 0,
        type: this.isVideoFile(media) ? 'VIDEO' : 'IMAGE',
        checksumSha256: '',
        metadata: {},
        uploadedAt: media.uploadedAt,
      } as MediaItem;

      this.previewMedia = item;
      this.previewFileUrl = this.getMediaUrl(media);
      this.previewOpen = true;
    };

    // If there's a token and it's near expiry, attempt refresh first
    if (token) {
      const expiresAt = this.getTokenExpiryMillis(token);
      const now = Date.now();
      if (expiresAt !== null && expiresAt - now < 120000) {
        // Try refresh; whether it succeeds or fails, proceed to open (fallback token may still be used)
        this.api.refreshAccessToken().subscribe({
          next: () => proceed(),
          error: () => proceed(),
        });
        return;
      }
    }

    // No refresh required
    proceed();
  }

  closePreview(): void {
    this.previewOpen = false;
    this.previewMedia = null;
    this.previewFileUrl = null;
  }

  showRejectForm(mediaId: string): void {
    if (this.rejectFormId === mediaId) {
      this.rejectFormId = null;
      this.rejectionReason = '';
    } else {
      this.rejectFormId = mediaId;
      this.rejectionReason = '';
    }
  }

  onApprove(mediaId: string): void {
    this.processingId = mediaId;
    this.eventService.approveEventMedia(this.eventId, mediaId).subscribe({
      next: () => {
        this.processingId = null;
        // Refresh media from API to ensure UI updates correctly
        this.refreshMediaAndUI();
        this.mediaUpdated.emit();
      },
      error: (error) => {
        console.error('Approval failed:', error);
        this.processingId = null;
      },
    });
  }

  confirmReject(mediaId: string): void {
    if (!this.rejectionReason.trim()) return;

    this.processingId = mediaId;
    this.eventService.rejectEventMedia(this.eventId, mediaId, this.rejectionReason).subscribe({
      next: () => {
        this.processingId = null;
        this.rejectFormId = null;
        this.rejectionReason = '';
        // Refresh media from API to ensure UI updates correctly
        this.refreshMediaAndUI();
        this.mediaUpdated.emit();
      },
      error: (error) => {
        console.error('Rejection failed:', error);
        this.processingId = null;
      },
    });
  }

  deleteMedia(mediaId: string): void {
    if (!confirm('Are you sure you want to delete this media? This action cannot be undone.')) {
      return;
    }

    this.processingId = mediaId;
    this.api.deleteEventMedia(this.eventId, mediaId).subscribe({
      next: () => {
        this.processingId = null;
        // Refresh media from API to ensure UI updates correctly
        this.refreshMediaAndUI();
        this.mediaUpdated.emit();
      },
      error: (error) => {
        console.error('Delete failed:', error);
        this.processingId = null;
        alert('Failed to delete media: ' + (error.error?.error || error.message));
      },
    });
  }
}

