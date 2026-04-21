import { Component, Input, Output, EventEmitter, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MediaItemComponent } from './media-item.component';
import { GalleryService } from '../gallery.service';
import { TimelineResponse, MediaItem } from '../../../core/api/sangrah-api.service';

@Component({
  selector: 'app-timeline-view',
  standalone: true,
  imports: [CommonModule, MediaItemComponent],
  templateUrl: './timeline-view.component.html',
})
export class TimelineViewComponent implements OnInit {
  private gallery = inject(GalleryService);

  @Input() media: MediaItem[] = [];
  @Output() preview = new EventEmitter<MediaItem>();
  @Output() delete = new EventEmitter<string>();

  timeline$ = this.gallery.timeline$;
  loading$ = this.gallery.loading$;
  currentView: 'daily' | 'monthly' | 'yearly' = 'monthly';

  ngOnInit(): void {
    this.loadTimeline();
  }

  onChangeView(view: 'daily' | 'monthly' | 'yearly'): void {
    this.currentView = view;
    this.loadTimeline();
  }

  private loadTimeline(): void {
    this.gallery.loadTimeline(this.currentView).subscribe({
      error: (error) => {
        console.error('Failed to load timeline:', error);
      },
    });
  }

  onPreview(item: MediaItem): void {
    this.preview.emit(item);
  }

  onDelete(mediaId: string): void {
    this.gallery.deleteMedia(mediaId).subscribe({
      next: () => {
        this.delete.emit(mediaId);
        // Reload timeline after deletion
        this.loadTimeline();
      },
      error: (error) => {
        console.error('Failed to delete media:', error);
        alert('Failed to delete media. Please try again.');
      },
    });
  }
}
