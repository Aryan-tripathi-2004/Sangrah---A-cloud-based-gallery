import {
  Component,
  input,
  output,
  inject,
  ChangeDetectionStrategy,
  OnInit,
  DestroyRef,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EventService } from '../event.service';
import { EventApiService } from '../../../core/api/services/event-api.service';
import { MediaItem } from '../../../core/api/models/api.models';
import { MediaPreviewModalComponent } from '../../gallery/components/media-preview-modal.component';

export interface MediaApprovalItem {
  mediaId: string;
  uploaderId?: string;
  uploaderName?: string;
  fileName?: string;
  mimeType?: string;
  uploadedAt?: string;
  status?: string;
  reason?: string;
}

export interface UploaderGroup {
  uploaderId: string;
  uploaderName?: string;
  pendingCount: number;
  approvedCount: number;
  rejectedCount: number;
  mediaList: MediaApprovalItem[];
}

@Component({
  selector: 'app-event-media-approval',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, MediaPreviewModalComponent],
  templateUrl: './event-media-approval.component.html',
})
export class EventMediaApprovalComponent implements OnInit {
  readonly eventId = input.required<string>();
  readonly isOwner = input<boolean>(false);
  readonly mediaUpdated = output<void>();

  private readonly eventService = inject(EventService);
  private readonly api = inject(EventApiService);
  private readonly destroyRef = inject(DestroyRef);

  // Reactive state signals
  readonly filterStatusSignal = signal<'all' | 'PENDING' | 'APPROVED' | 'REJECTED'>('PENDING');
  readonly mediaFilterStatusSignal = signal<'all' | 'PENDING' | 'APPROVED' | 'REJECTED'>('all');
  readonly selectedUploaderId = signal<string | null>(null);

  readonly rejectFormIdSignal = signal<string | null>(null);
  readonly rejectionReasonSignal = signal<string>('');
  readonly processingIdSignal = signal<string | null>(null);

  // Preview modal state signals
  readonly previewOpenSignal = signal<boolean>(false);
  readonly previewMediaSignal = signal<MediaItem | null>(null);
  readonly previewFileUrlSignal = signal<string | null>(null);
  readonly tokenReadySignal = signal<boolean>(false);

  readonly failedMediaIdsSignal = signal<Set<string>>(new Set<string>());

  // Automatically recalculates whenever eventService.eventMedia() changes
  readonly uploaderGroups = computed<UploaderGroup[]>(() => {
    const items = this.eventService.eventMedia();
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

      const item: MediaApprovalItem = {
        mediaId: media.mediaId,
        uploaderId: media.uploaderId,
        uploaderName: media.uploaderName,
        fileName: media.fileName,
        mimeType: media.mimeType,
        uploadedAt: media.uploadedAt,
        status: media.status,
        reason: media.reason,
      };

      const group = grouped.get(uploaderId)!;
      group.mediaList.push(item);

      if (media.status === 'PENDING') group.pendingCount++;
      else if (media.status === 'APPROVED') group.approvedCount++;
      else if (media.status === 'REJECTED') group.rejectedCount++;
    });

    return Array.from(grouped.values());
  });

  // Derived filter signal for uploaders
  readonly filteredUploaders = computed<UploaderGroup[]>(() => {
    const groups = this.uploaderGroups();
    const status = this.filterStatusSignal();
    if (status === 'all') return groups;
    if (status === 'PENDING') return groups.filter(g => g.pendingCount > 0);
    if (status === 'APPROVED') return groups.filter(g => g.approvedCount > 0 && g.pendingCount === 0);
    if (status === 'REJECTED') return groups.filter(g => g.rejectedCount > 0);
    return groups;
  });

  // Derived selected uploader
  readonly selectedUploaderComputed = computed<UploaderGroup | null>(() => {
    const id = this.selectedUploaderId();
    if (!id) return null;
    return this.uploaderGroups().find(g => g.uploaderId === id) || null;
  });

  // Derived filtered media for the selected uploader
  readonly filteredMediaFromUploaderComputed = computed<MediaApprovalItem[]>(() => {
    const selected = this.selectedUploaderComputed();
    if (!selected) return [];
    const filter = this.mediaFilterStatusSignal();
    if (filter === 'all') return selected.mediaList;
    return selected.mediaList.filter(m => m.status === filter);
  });

  // Derived total pending count
  readonly pendingCountComputed = computed<number>(() => {
    return this.uploaderGroups().reduce((sum, group) => sum + group.pendingCount, 0);
  });

  // Backward-compatible accessors for template binding
  get filterStatus(): 'all' | 'PENDING' | 'APPROVED' | 'REJECTED' {
    return this.filterStatusSignal();
  }
  set filterStatus(value: 'all' | 'PENDING' | 'APPROVED' | 'REJECTED') {
    this.filterStatusSignal.set(value);
  }

  get mediaFilterStatus(): 'all' | 'PENDING' | 'APPROVED' | 'REJECTED' {
    return this.mediaFilterStatusSignal();
  }
  set mediaFilterStatus(value: 'all' | 'PENDING' | 'APPROVED' | 'REJECTED') {
    this.mediaFilterStatusSignal.set(value);
  }

  get selectedUploader(): UploaderGroup | null {
    return this.selectedUploaderComputed();
  }
  set selectedUploader(group: UploaderGroup | null) {
    this.selectedUploaderId.set(group ? group.uploaderId : null);
  }

  get pendingCount(): number {
    return this.pendingCountComputed();
  }

  get rejectFormId(): string | null {
    return this.rejectFormIdSignal();
  }
  set rejectFormId(val: string | null) {
    this.rejectFormIdSignal.set(val);
  }

  get rejectionReason(): string {
    return this.rejectionReasonSignal();
  }
  set rejectionReason(val: string) {
    this.rejectionReasonSignal.set(val);
  }

  get processingId(): string | null {
    return this.processingIdSignal();
  }
  set processingId(val: string | null) {
    this.processingIdSignal.set(val);
  }

  get previewOpen(): boolean {
    return this.previewOpenSignal();
  }
  set previewOpen(val: boolean) {
    this.previewOpenSignal.set(val);
  }

  get previewMedia(): MediaItem | null {
    return this.previewMediaSignal();
  }
  set previewMedia(val: MediaItem | null) {
    this.previewMediaSignal.set(val);
  }

  get previewFileUrl(): string | null {
    return this.previewFileUrlSignal();
  }
  set previewFileUrl(val: string | null) {
    this.previewFileUrlSignal.set(val);
  }

  get tokenReady(): boolean {
    return this.tokenReadySignal();
  }
  set tokenReady(val: boolean) {
    this.tokenReadySignal.set(val);
  }

  ngOnInit(): void {
    const id = this.eventId();
    if (id) {
      this.eventService.loadEventMedia(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        error: (err) => console.error('Failed to load event media on init:', err)
      });
    }
  }

  getUploaderDisplayName(group: UploaderGroup | null): string {
    if (!group) return 'Unknown';
    return group.uploaderName && group.uploaderName.trim() ? group.uploaderName : group.uploaderId;
  }

  getFilteredUploaders(): UploaderGroup[] {
    return this.filteredUploaders();
  }

  getFilteredMediaFromUploader(): MediaApprovalItem[] {
    return this.filteredMediaFromUploaderComputed();
  }

  onMediaLoadError(mediaId: string): void {
    const next = new Set(this.failedMediaIdsSignal());
    next.add(mediaId);
    this.failedMediaIdsSignal.set(next);
  }

  isMediaFailed(mediaId: string): boolean {
    return this.failedMediaIdsSignal().has(mediaId);
  }

  isVideo(mimeType?: string, fileName?: string): boolean {
    const mime = (mimeType || '').toLowerCase();
    if (mime.startsWith('video/')) return true;
    const name = (fileName || '').toLowerCase();
    return /\.(mp4|webm|mov|ogg|m4v|mkv)$/i.test(name);
  }

  isImage(mimeType?: string, fileName?: string): boolean {
    const mime = (mimeType || '').toLowerCase();
    if (mime.startsWith('image/')) return true;
    const name = (fileName || '').toLowerCase();
    if (/\.(jpg|jpeg|png|gif|webp|svg|bmp|ico|avif)$/i.test(name)) return true;
    return !this.isVideo(mimeType, fileName) && !/\.(pdf|doc|docx|txt|zip|tar|gz)$/i.test(name);
  }

  getMediaUrl(mediaId: string): string {
    return this.api.getEventMediaFileUrl(this.eventId(), mediaId);
  }

  openPreview(media: MediaApprovalItem): void {
    const item: MediaItem = {
      id: media.mediaId,
      originalFileName: media.fileName || media.mediaId,
      mimeType: media.mimeType,
      sizeBytes: 0,
      type: this.isVideo(media.mimeType, media.fileName) ? 'VIDEO' : 'IMAGE',
      checksumSha256: '',
      metadata: {},
      uploadedAt: media.uploadedAt,
    } as MediaItem;

    this.previewMediaSignal.set(item);
    this.previewFileUrlSignal.set(this.getMediaUrl(media.mediaId));
    this.previewOpenSignal.set(true);
  }

  closePreview(): void {
    this.previewOpenSignal.set(false);
    this.previewMediaSignal.set(null);
    this.previewFileUrlSignal.set(null);
  }

  showRejectForm(mediaId: string): void {
    if (this.rejectFormIdSignal() === mediaId) {
      this.rejectFormIdSignal.set(null);
      this.rejectionReasonSignal.set('');
    } else {
      this.rejectFormIdSignal.set(mediaId);
      this.rejectionReasonSignal.set('');
    }
  }

  onApprove(mediaId: string): void {
    this.processingIdSignal.set(mediaId);
    this.eventService.updateEventMediaStatus(this.eventId(), mediaId, 'APPROVED').pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.processingIdSignal.set(null);
        this.mediaUpdated.emit();
      },
      error: (error: unknown) => {
        console.error('Approval failed:', error);
        this.processingIdSignal.set(null);
      },
    });
  }

  confirmReject(mediaId: string): void {
    const reason = this.rejectionReasonSignal();
    if (!reason.trim()) return;

    this.processingIdSignal.set(mediaId);
    this.eventService.updateEventMediaStatus(this.eventId(), mediaId, 'REJECTED', reason).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.processingIdSignal.set(null);
        this.rejectFormIdSignal.set(null);
        this.rejectionReasonSignal.set('');
        this.mediaUpdated.emit();
      },
      error: (error: unknown) => {
        console.error('Rejection failed:', error);
        this.processingIdSignal.set(null);
      },
    });
  }

  deleteMedia(mediaId: string): void {
    if (!confirm('Are you sure you want to delete this media? This action cannot be undone.')) {
      return;
    }

    this.processingIdSignal.set(mediaId);
    this.api.deleteEventMedia(this.eventId(), mediaId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.processingIdSignal.set(null);
        this.eventService.loadEventMedia(this.eventId()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
        this.mediaUpdated.emit();
      },
      error: (error: any) => {
        console.error('Delete failed:', error);
        this.processingIdSignal.set(null);
        alert('Failed to delete media: ' + (error.error?.error || error.message));
      },
    });
  }
}
