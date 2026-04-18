import { Component, Input, Output, EventEmitter, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MediaItemComponent } from './media-item.component';
import { GalleryService } from '../gallery.service';
import { TimelineResponse, MediaItem } from '../../../core/api/sangrah-api.service';

@Component({
  selector: 'app-timeline-view',
  standalone: true,
  imports: [CommonModule, MediaItemComponent],
  template: `
    <div class="space-y-8">
      <!-- Timeline View Selector -->
      <div class="flex gap-2 sticky top-0 bg-slate-900/95 backdrop-blur z-10 p-4 rounded-lg">
        <button
          (click)="onChangeView('daily')"
          [class.bg-blue-600]="currentView === 'daily'"
          [class.bg-slate-700]="currentView !== 'daily'"
          class="px-4 py-2 rounded-lg font-medium transition hover:bg-slate-600"
        >
          Daily
        </button>
        <button
          (click)="onChangeView('monthly')"
          [class.bg-blue-600]="currentView === 'monthly'"
          [class.bg-slate-700]="currentView !== 'monthly'"
          class="px-4 py-2 rounded-lg font-medium transition hover:bg-slate-600"
        >
          Monthly
        </button>
        <button
          (click)="onChangeView('yearly')"
          [class.bg-blue-600]="currentView === 'yearly'"
          [class.bg-slate-700]="currentView !== 'yearly'"
          class="px-4 py-2 rounded-lg font-medium transition hover:bg-slate-600"
        >
          Yearly
        </button>
      </div>

      <!-- Loading State -->
      <div *ngIf="loading$ | async" class="text-center py-16">
        <div class="inline-block">
          <div class="text-4xl mb-4">⏳</div>
          <p class="text-slate-400 font-medium">Loading timeline...</p>
        </div>
      </div>

      <!-- Timeline Groups -->
      <div *ngIf="(timeline$ | async) as timeline; else empty" class="space-y-12">
        <div *ngFor="let group of timeline.groups" class="space-y-4">
          <!-- Group Header -->
          <div class="sticky top-20 bg-slate-800/80 backdrop-blur px-4 py-3 rounded-lg z-5">
            <div class="flex items-center justify-between">
              <div>
                <h2 class="text-2xl font-bold">{{ group.label }}</h2>
                <p class="text-sm text-slate-400">{{ group.count }} {{ group.count === 1 ? 'item' : 'items' }}</p>
              </div>
              <div class="text-3xl opacity-50">📅</div>
            </div>
          </div>

          <!-- Media Masonry Grid -->
          <div class="columns-1 sm:columns-2 md:columns-3 lg:columns-4 xl:columns-5 gap-4 md:gap-5">
            <app-media-item
              *ngFor="let item of group.items"
              [media]="item"
              (preview)="onPreview($event)"
              (delete)="onDelete($event)"
            ></app-media-item>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <ng-template #empty>
        <div
          *ngIf="!(loading$ | async)"
          class="text-center py-16 bg-slate-800/20 border border-slate-700/50 rounded-lg space-y-4"
        >
          <div class="text-5xl">📷</div>
          <h3 class="text-2xl font-bold">No Media Found</h3>
          <p class="text-slate-400">Your gallery is empty. Start uploading photos and videos!</p>
        </div>
      </ng-template>

      <!-- Scroll indicator -->
      <div *ngIf="(timeline$ | async) as timeline; else noScroll" [hidden]="timeline.totalGroups === 0" class="text-center pt-8 text-slate-500 text-sm">
        📍 Showing {{ timeline.totalGroups }} {{ timeline.totalGroups === 1 ? 'group' : 'groups' }}
      </div>

      <ng-template #noScroll></ng-template>
    </div>
  `,
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
