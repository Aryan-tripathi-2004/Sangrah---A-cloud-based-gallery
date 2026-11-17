import { Component, OnInit, inject, signal, ChangeDetectionStrategy, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { UserApiService } from '../../core/api/services/user-api.service';
import { EventApiService } from '../../core/api/services/event-api.service';
import { GalleryApiService } from '../../core/api/services/gallery-api.service';
import { User, Event, MediaItem, StorageUsage } from '../../core/api/models/api.models';
import { LayoutComponent } from '../../shared/layout/layout.component';
import { ProfileSidebarComponent } from './components/profile-sidebar/profile-sidebar.component';
import { ProfileTabsComponent, ProfileTabType } from './components/profile-tabs/profile-tabs.component';
import { EventsGridComponent } from './components/events-grid/events-grid.component';
import { GalleryGridComponent } from './components/gallery-grid/gallery-grid.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    LayoutComponent,
    ProfileSidebarComponent,
    ProfileTabsComponent,
    EventsGridComponent,
    GalleryGridComponent,
  ],
  templateUrl: './profile.component.html',
})
export class ProfileComponent implements OnInit {
  private static readonly PROFILE_PAGE_SIZE = 20;

  private readonly userApi = inject(UserApiService);
  private readonly eventApi = inject(EventApiService);
  private readonly galleryApi = inject(GalleryApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly user = signal<User | null>(null);
  readonly myEvents = signal<Event[]>([]);
  readonly galleryItems = signal<MediaItem[]>([]);
  readonly storageUsage = signal<StorageUsage | null>(null);
  readonly totalEventsCount = signal<number>(0);
  readonly totalGalleryCount = signal<number>(0);

  readonly eventsPage = signal<number>(0);
  readonly hasMoreEvents = signal<boolean>(false);
  readonly isLoadingMoreEvents = signal<boolean>(false);

  readonly galleryPage = signal<number>(0);
  readonly hasMoreGallery = signal<boolean>(false);
  readonly isLoadingMoreGallery = signal<boolean>(false);

  readonly activeTab = signal<ProfileTabType>('events');
  readonly isLoading = signal(true);
  readonly isUploadingAvatar = signal(false);
  readonly isSavingBio = signal(false);

  readonly feedbackMessage = signal<{ type: 'success' | 'error'; text: string } | null>(null);

  ngOnInit(): void {
    this.loadAllProfileData();
  }

  loadAllProfileData(): void {
    this.isLoading.set(true);

    forkJoin({
      profile: this.userApi.getProfile().pipe(
        catchError((err) => {
          console.error('Failed to load profile:', err);
          this.showFeedback({ type: 'error', text: 'Failed to load profile data' });
          return of(null);
        })
      ),
      events: this.eventApi.getMyEvents(0, ProfileComponent.PROFILE_PAGE_SIZE).pipe(
        catchError((err) => {
          console.error('Failed to load events:', err);
          return of([]);
        })
      ),
      gallery: this.galleryApi.listGalleryMedia(0, ProfileComponent.PROFILE_PAGE_SIZE).pipe(
        catchError((err) => {
          console.error('Failed to load gallery items:', err);
          return of({ count: 0, items: [] });
        })
      ),
      storage: this.galleryApi.getStorageUsageGallery().pipe(
        catchError((err) => {
          console.error('Failed to load storage usage:', err);
          return of(null);
        })
      ),
    })
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe({
      next: ({ profile, events, gallery, storage }) => {
        if (profile) {
          this.user.set(profile);
        }
        const evList: Event[] = Array.isArray(events) ? events : (events?.content || []);
        this.myEvents.set(evList);
        const totalEv = (events as any)?.totalElements !== undefined ? (events as any).totalElements : evList.length;
        this.totalEventsCount.set(totalEv);
        this.eventsPage.set(0);
        this.hasMoreEvents.set((events as any)?.hasNext ?? ((events as any)?.totalPages ? (events as any).totalPages > 1 : evList.length < totalEv));

        const galList: MediaItem[] = gallery && Array.isArray(gallery.items) ? gallery.items : (Array.isArray(gallery) ? gallery : []);
        this.galleryItems.set(galList);
        const galCount = storage?.fileCount !== undefined
          ? storage.fileCount
          : ((gallery as any)?.totalElements ?? (gallery as any)?.count ?? galList.length);
        this.totalGalleryCount.set(galCount);
        this.galleryPage.set(0);
        this.hasMoreGallery.set((gallery as any)?.hasNext ?? ((gallery as any)?.totalPages ? (gallery as any).totalPages > 1 : galList.length < galCount));

        this.storageUsage.set(storage);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Unexpected error loading profile data:', err);
        this.isLoading.set(false);
      },
    });
  }

  loadMoreEvents(): void {
    if (this.isLoadingMoreEvents() || !this.hasMoreEvents()) return;
    this.isLoadingMoreEvents.set(true);
    const nextPage = this.eventsPage() + 1;
    this.eventApi.getMyEvents(nextPage, ProfileComponent.PROFILE_PAGE_SIZE)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res: any) => {
          const newEvents: Event[] = Array.isArray(res) ? res : (res?.content || []);
          this.myEvents.update(current => [...current, ...newEvents]);
          this.eventsPage.set(nextPage);
          const totalPages = res?.totalPages ?? 1;
          this.hasMoreEvents.set(res?.hasNext ?? (nextPage + 1 < totalPages));
          this.isLoadingMoreEvents.set(false);
        },
        error: (err) => {
          console.error('Failed to load more events:', err);
          this.isLoadingMoreEvents.set(false);
        }
      });
  }

  loadMoreGallery(): void {
    if (this.isLoadingMoreGallery() || !this.hasMoreGallery()) return;
    this.isLoadingMoreGallery.set(true);
    const nextPage = this.galleryPage() + 1;
    this.galleryApi.listGalleryMedia(nextPage, ProfileComponent.PROFILE_PAGE_SIZE)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res: any) => {
          const newItems: MediaItem[] = res && Array.isArray(res.items) ? res.items : (Array.isArray(res) ? res : (res?.content || []));
          this.galleryItems.update(current => [...current, ...newItems]);
          this.galleryPage.set(nextPage);
          const totalPages = res?.totalPages ?? 1;
          this.hasMoreGallery.set(res?.hasNext ?? (nextPage + 1 < totalPages));
          this.isLoadingMoreGallery.set(false);
        },
        error: (err) => {
          console.error('Failed to load more gallery items:', err);
          this.isLoadingMoreGallery.set(false);
        }
      });
  }

  onAvatarSelected(file: File): void {
    this.isUploadingAvatar.set(true);
    this.showFeedback(null);

    this.userApi.uploadProfileAvatar(file)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedUser) => {
          this.user.set(updatedUser);
          this.isUploadingAvatar.set(false);
          this.showFeedback({ type: 'success', text: 'Profile photo updated successfully!' });
        },
        error: (err) => {
          this.isUploadingAvatar.set(false);
          this.showFeedback({
            type: 'error',
            text: err.message || err.error?.message || 'Failed to upload profile photo.',
          });
        },
      });
  }

  onBioSaved(newBio: string): void {
    this.isSavingBio.set(true);
    this.showFeedback(null);

    this.userApi.updateProfile({ bio: newBio })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedUser) => {
          this.user.set(updatedUser);
          this.isSavingBio.set(false);
          this.showFeedback({ type: 'success', text: 'Bio updated successfully!' });
        },
        error: (err) => {
          this.isSavingBio.set(false);
          this.showFeedback({
            type: 'error',
            text: err.message || err.error?.message || 'Failed to update bio.',
          });
        },
      });
  }

  onTabChanged(tab: ProfileTabType): void {
    this.activeTab.set(tab);
  }

  private showFeedback(feedback: { type: 'success' | 'error'; text: string } | null): void {
    this.feedbackMessage.set(feedback);
    if (feedback) {
      setTimeout(() => {
        this.feedbackMessage.set(null);
      }, 4000);
    }
  }
}
