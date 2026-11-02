import { Component, input, output, OnInit, inject, ChangeDetectionStrategy, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MediaItemComponent } from './media-item.component';
import { GalleryService } from '../gallery.service';
import { TimelineResponse, MediaItem } from '../../../core/api/models/api.models';

@Component({
  selector: 'app-timeline-view',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MediaItemComponent],
  templateUrl: './timeline-view.component.html',
})
export class TimelineViewComponent implements OnInit {
  private readonly gallery = inject(GalleryService);
  private readonly destroyRef = inject(DestroyRef);

  readonly media = input<MediaItem[]>([]);
  readonly preview = output<MediaItem>();
  readonly delete = output<string>();

  readonly timeline = this.gallery.timeline;
  readonly loading = this.gallery.loading;
  readonly currentView = signal<'daily' | 'monthly' | 'yearly'>('monthly');

  ngOnInit(): void {
    this.loadTimeline();
  }

  onChangeView(view: 'daily' | 'monthly' | 'yearly'): void {
    this.currentView.set(view);
    this.loadTimeline();
  }

  private loadTimeline(): void {
    this.gallery.loadTimeline(this.currentView())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        error: (error) => {
          console.error('Failed to load timeline:', error);
        },
      });
  }

  onPreview(item: MediaItem): void {
    this.preview.emit(item);
  }

  onDelete(mediaId: string): void {
    this.gallery.deleteMedia(mediaId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.delete.emit(mediaId);
          // Reload timeline after deletion
          this.loadTimeline();
        },
        error: (error) => {
          console.error('Failed to delete media:', error);
        },
      });
  }
}
