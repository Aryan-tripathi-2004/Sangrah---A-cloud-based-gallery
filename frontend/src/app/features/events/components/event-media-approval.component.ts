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
  template: `
    <div class="space-y-6">
      <!-- Approval Header -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-2xl font-bold">Media Approvals</h2>
          <p class="text-slate-400">Review and approve/reject submissions</p>
        </div>
        <div class="bg-blue-500/10 border border-blue-500/30 rounded-lg px-4 py-2">
          <p class="text-sm">
            <span class="font-semibold text-blue-400">{{ pendingCount }}</span>
            <span class="text-blue-300">pending</span>
          </p>
        </div>
      </div>

      <!-- Two Views: Uploaders List or Media List -->
      
      <!-- View 1: List of Uploaders (default view) -->
      <div *ngIf="!selectedUploader">
        <!-- Filter Tabs -->
        <div class="flex gap-3 border-b border-slate-800 mb-6">
          <button
            (click)="filterStatus = 'all'"
            [class.border-b-2]="filterStatus === 'all'"
            [class.border-blue-500]="filterStatus === 'all'"
            [class.text-blue-400]="filterStatus === 'all'"
            class="px-4 py-3 font-semibold transition hover:text-slate-100"
          >
            All Uploaders
          </button>
          <button
            (click)="filterStatus = 'PENDING'"
            [class.border-b-2]="filterStatus === 'PENDING'"
            [class.border-blue-500]="filterStatus === 'PENDING'"
            [class.text-blue-400]="filterStatus === 'PENDING'"
            class="px-4 py-3 font-semibold transition hover:text-slate-100"
          >
            ⏳ Has Pending
          </button>
          <button
            (click)="filterStatus = 'APPROVED'"
            [class.border-b-2]="filterStatus === 'APPROVED'"
            [class.border-blue-500]="filterStatus === 'APPROVED'"
            [class.text-green-400]="filterStatus === 'APPROVED'"
            class="px-4 py-3 font-semibold transition hover:text-slate-100"
          >
            ✅ All Approved
          </button>
        </div>

        <!-- Uploaders Grid -->
        <div *ngIf="getFilteredUploaders().length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div *ngFor="let group of getFilteredUploaders()"
               (click)="selectedUploader = group"
               class="rounded-lg bg-slate-800/30 border border-slate-700/50 hover:border-blue-500 cursor-pointer transition p-6 space-y-4">
            <!-- Uploader Info -->
            <div>
              <p class="text-lg font-bold">👤 {{ getUploaderDisplayName(group) }}</p>
            </div>

            <!-- Media Counts -->
            <div class="grid grid-cols-3 gap-3">
              <div class="bg-slate-900/50 rounded p-3 text-center">
                <p class="text-2xl font-bold text-yellow-400">{{ group.pendingCount }}</p>
                <p class="text-xs text-slate-400">Pending</p>
              </div>
              <div class="bg-slate-900/50 rounded p-3 text-center">
                <p class="text-2xl font-bold text-green-400">{{ group.approvedCount }}</p>
                <p class="text-xs text-slate-400">Approved</p>
              </div>
              <div class="bg-slate-900/50 rounded p-3 text-center">
                <p class="text-2xl font-bold text-red-400">{{ group.rejectedCount }}</p>
                <p class="text-xs text-slate-400">Rejected</p>
              </div>
            </div>

            <!-- Click Hint -->
            <p class="text-xs text-blue-400 text-center">Click to view media →</p>
          </div>
        </div>

        <!-- Empty State -->
        <div *ngIf="getFilteredUploaders().length === 0" class="text-center py-12 rounded-lg bg-slate-800/20 border border-slate-700/50">
          <p class="text-5xl mb-4">📭</p>
          <p class="text-slate-400">No submissions yet</p>
        </div>
      </div>

      <!-- View 2: Selected Uploader's Media -->
      <div *ngIf="selectedUploader">
        <!-- Back Button & Uploader Header -->
        <div class="flex items-center gap-4 mb-6">
          <button
            (click)="selectedUploader = null"
            class="px-4 py-2 bg-slate-700 hover:bg-slate-600 rounded-lg font-semibold transition">
            ← Back to Uploaders
          </button>
          <div>
            <h3 class="text-xl font-bold">👤 {{ getUploaderDisplayName(selectedUploader) }}</h3>
            <p class="text-sm text-slate-400">{{ selectedUploader.mediaList.length }} submissions</p>
          </div>
        </div>

        <!-- Filter Tabs for Media -->
        <div class="flex gap-3 border-b border-slate-800 mb-6">
          <button
            (click)="mediaFilterStatus = 'all'"
            [class.border-b-2]="mediaFilterStatus === 'all'"
            [class.border-blue-500]="mediaFilterStatus === 'all'"
            [class.text-blue-400]="mediaFilterStatus === 'all'"
            class="px-4 py-3 font-semibold transition hover:text-slate-100"
          >
            All ({{ selectedUploader.mediaList.length }})
          </button>
          <button
            (click)="mediaFilterStatus = 'PENDING'"
            [class.border-b-2]="mediaFilterStatus === 'PENDING'"
            [class.border-blue-500]="mediaFilterStatus === 'PENDING'"
            [class.text-yellow-400]="mediaFilterStatus === 'PENDING'"
            class="px-4 py-3 font-semibold transition hover:text-slate-100"
          >
            ⏳ Pending ({{ selectedUploader.pendingCount }})
          </button>
          <button
            (click)="mediaFilterStatus = 'APPROVED'"
            [class.border-b-2]="mediaFilterStatus === 'APPROVED'"
            [class.border-blue-500]="mediaFilterStatus === 'APPROVED'"
            [class.text-green-400]="mediaFilterStatus === 'APPROVED'"
            class="px-4 py-3 font-semibold transition hover:text-slate-100"
          >
            ✅ Approved ({{ selectedUploader.approvedCount }})
          </button>
          <button
            (click)="mediaFilterStatus = 'REJECTED'"
            [class.border-b-2]="mediaFilterStatus === 'REJECTED'"
            [class.border-blue-500]="mediaFilterStatus === 'REJECTED'"
            [class.text-red-400]="mediaFilterStatus === 'REJECTED'"
            class="px-4 py-3 font-semibold transition hover:text-slate-100"
          >
            ❌ Rejected ({{ selectedUploader.rejectedCount }})
          </button>
        </div>

        <!-- Media Items with Preview -->
        <div *ngIf="getFilteredMediaFromUploader().length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div *ngFor="let media of getFilteredMediaFromUploader()" class="rounded-lg bg-slate-800/30 border border-slate-700/50 overflow-hidden">
            <!-- Media Preview (Clickable) -->
            <div class="h-48 bg-gradient-to-br from-blue-600/20 to-purple-600/20 flex items-center justify-center relative overflow-hidden"
                 (click)="openPreview(media)">
              <img *ngIf="isImageFile(media)" [src]="getMediaUrl(media)" 
                   alt="Media preview" class="w-full h-full object-cover">
              <video *ngIf="isVideoFile(media)" class="w-full h-full object-cover" muted>
                <source [src]="getMediaUrl(media)" />
              </video>
              <p *ngIf="!isImageFile(media) && !isVideoFile(media)" class="text-4xl">📄</p>
            </div>

            <!-- Media Info -->
            <div class="p-4">
              <div class="flex items-start justify-between mb-3">
                <div class="flex-1">
                  <p class="font-semibold truncate">{{ media.fileName }}</p>
                  <p class="text-xs text-slate-400">{{ media.uploadedAt | date: 'short' }}</p>
                </div>
                <div class="ml-2">
                  <span
                    class="px-2 py-1 rounded text-xs font-semibold whitespace-nowrap"
                    [ngClass]="{
                      'bg-yellow-500/20 text-yellow-400': media.status === 'PENDING',
                      'bg-green-500/20 text-green-400': media.status === 'APPROVED',
                      'bg-red-500/20 text-red-400': media.status === 'REJECTED'
                    }"
                  >
                    {{ media.status }}
                  </span>
                </div>
              </div>

              <!-- Rejection Reason (if rejected) -->
              <div *ngIf="media.status === 'REJECTED' && media.reason" class="bg-red-500/10 border border-red-500/30 rounded p-2 mb-3">
                <p class="text-xs text-red-300"><span class="font-semibold">Reason:</span> {{ media.reason }}</p>
              </div>

              <!-- Action Buttons (only for PENDING) -->
              <div *ngIf="media.status === 'PENDING'" class="space-y-3">
                <!-- Approve Button -->
                <button
                  (click)="onApprove(media.mediaId)"
                  [disabled]="processingId === media.mediaId"
                  class="w-full px-4 py-2 bg-green-600 hover:bg-green-700 disabled:bg-slate-600 rounded-lg font-semibold transition"
                >
                  {{ processingId === media.mediaId ? 'Approving...' : '✅ Approve' }}
                </button>

                <!-- Reject Button -->
                <button
                  (click)="showRejectForm(media.mediaId)"
                  [disabled]="processingId === media.mediaId"
                  class="w-full px-4 py-2 bg-red-600 hover:bg-red-700 disabled:bg-slate-600 rounded-lg font-semibold transition"
                >
                  {{ rejectFormId === media.mediaId ? '❌ Cancel' : '❌ Reject' }}
                </button>

                <!-- Reject Form -->
                <div *ngIf="rejectFormId === media.mediaId" class="space-y-2 bg-slate-900/50 p-3 rounded">
                  <input
                    [(ngModel)]="rejectionReason"
                    type="text"
                    placeholder="Reason for rejection..."
                    class="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded text-sm placeholder-slate-500"
                  />
                  <button
                    (click)="confirmReject(media.mediaId)"
                    [disabled]="!rejectionReason.trim() || processingId === media.mediaId"
                    class="w-full px-3 py-2 bg-red-600 hover:bg-red-700 disabled:bg-slate-600 rounded text-sm font-semibold transition"
                  >
                    Confirm Rejection
                  </button>
                </div>
              </div>

              <!-- Status Info (for already processed) -->
              <div *ngIf="media.status === 'APPROVED'" class="px-4 py-2 bg-green-500/10 border border-green-500/30 rounded text-sm text-center text-green-300">
                Approved and visible
              </div>

              <div *ngIf="media.status === 'REJECTED'" class="px-4 py-2 bg-red-500/10 border border-red-500/30 rounded text-sm text-center text-red-300">
                Not visible to public
              </div>

              <!-- Delete Button (always available) -->
              <button
                (click)="deleteMedia(media.mediaId)"
                [disabled]="processingId === media.mediaId"
                class="w-full px-4 py-2 bg-red-600/20 hover:bg-red-600/30 border border-red-600/50 text-red-400 rounded-lg font-semibold transition text-sm mt-3"
              >
                {{ processingId === media.mediaId ? 'Deleting...' : 'Delete Media' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Empty State -->
        <div *ngIf="getFilteredMediaFromUploader().length === 0" class="text-center py-12 rounded-lg bg-slate-800/20 border border-slate-700/50">
          <p class="text-slate-400">No {{ mediaFilterStatus }} submissions from this uploader</p>
        </div>
      </div>

      <!-- Preview Modal -->
      <app-media-preview-modal
        [isOpen]="previewOpen"
        [media]="previewMedia"
        [fileUrlOverride]="previewFileUrl"
        (closeModal)="closePreview()"
      ></app-media-preview-modal>
    </div>
  `,
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

