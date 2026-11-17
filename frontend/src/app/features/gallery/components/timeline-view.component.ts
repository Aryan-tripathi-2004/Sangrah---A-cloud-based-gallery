import {
  Component,
  input,
  output,
  OnInit,
  AfterViewInit,
  OnDestroy,
  inject,
  ChangeDetectionStrategy,
  signal,
  DestroyRef,
  ElementRef,
  viewChild,
  PLATFORM_ID
} from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MediaItemComponent } from './media-item.component';
import { GalleryService } from '../gallery.service';
import { TimelineResponse, MediaItem, TimelineGroup } from '../../../core/api/models/api.models';

@Component({
  selector: 'app-timeline-view',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MediaItemComponent],
  templateUrl: './timeline-view.component.html',
})
export class TimelineViewComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly gallery = inject(GalleryService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly platformId = inject(PLATFORM_ID);

  readonly media = input<MediaItem[]>([]);
  readonly preview = output<MediaItem>();
  readonly delete = output<string>();

  readonly scrollSentinel = viewChild<ElementRef<HTMLElement>>('scrollSentinel');

  readonly timeline = this.gallery.timeline;
  readonly timelineGroups = this.gallery.timelineGroups;
  readonly loading = this.gallery.loading;
  readonly isLoadingMore = this.gallery.isLoadingMoreTimeline;
  readonly hasMoreGroups = this.gallery.hasMoreTimeline;
  readonly currentView = signal<'daily' | 'monthly' | 'yearly'>('monthly');
  readonly groupLoading = signal<{ [key: string]: boolean }>({});

  private observer: IntersectionObserver | null = null;

  ngOnInit(): void {
    this.loadTimeline();
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.setupIntersectionObserver();
    }
  }

  ngOnDestroy(): void {
    this.cleanupObserver();
  }

  onChangeView(view: 'daily' | 'monthly' | 'yearly'): void {
    this.currentView.set(view);
    this.loadTimeline();
  }

  private loadTimeline(): void {
    this.gallery.loadTimeline(this.currentView(), 0)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        error: (error) => {
          console.error('Failed to load timeline:', error);
        },
      });
  }

  private setupIntersectionObserver(): void {
    const sentinelEl = this.scrollSentinel()?.nativeElement;
    if (!sentinelEl) {
      setTimeout(() => this.setupIntersectionObserver(), 100);
      return;
    }

    this.cleanupObserver();

    this.observer = new IntersectionObserver((entries) => {
      const entry = entries[0];
      if (entry.isIntersecting && this.hasMoreGroups() && !this.isLoadingMore() && !this.loading()) {
        this.gallery.loadMoreTimeline()
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            error: (err) => console.error('Failed to load more timeline groups:', err)
          });
      }
    }, {
      rootMargin: '300px 0px',
      threshold: 0.1
    });

    this.observer.observe(sentinelEl);
  }

  private cleanupObserver(): void {
    if (this.observer) {
      this.observer.disconnect();
      this.observer = null;
    }
  }

  loadMoreGroupItems(group: TimelineGroup): void {
    const nextGroupPage = (group.page ?? 0) + 1;
    const groupKey = group.label;
    this.groupLoading.update(current => ({ ...current, [groupKey]: true }));
    const groupByParam = (group.groupType || this.currentView().toUpperCase()) as 'DAILY' | 'MONTHLY' | 'YEARLY';
    const groupDate = group.date || group.label;

    this.gallery.loadMoreGroupItems(groupByParam, groupDate, nextGroupPage, 20)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (pageRes) => {
          this.groupLoading.update(current => ({ ...current, [groupKey]: false }));
          if (pageRes && pageRes.content) {
            this.gallery.timelineGroups.update(groups =>
              groups.map(g =>
                g.label === groupKey
                  ? {
                      ...g,
                      items: [...g.items, ...pageRes.content],
                      page: nextGroupPage,
                      hasMore: !pageRes.last
                    }
                  : g
              )
            );
          }
        },
        error: (err) => {
          this.groupLoading.update(current => ({ ...current, [groupKey]: false }));
          console.error('Failed to load more items for group:', groupKey, err);
        }
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
        },
        error: (error) => {
          console.error('Failed to delete media:', error);
        },
      });
  }
}
