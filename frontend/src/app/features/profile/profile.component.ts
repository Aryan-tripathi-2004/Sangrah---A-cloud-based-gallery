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
  private readonly userApi = inject(UserApiService);
  private readonly eventApi = inject(EventApiService);
  private readonly galleryApi = inject(GalleryApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly user = signal<User | null>(null);
  readonly myEvents = signal<Event[]>([]);
  readonly galleryItems = signal<MediaItem[]>([]);
  readonly storageUsage = signal<StorageUsage | null>(null);

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
      events: this.eventApi.getMyEvents().pipe(
        catchError((err) => {
          console.error('Failed to load events:', err);
          return of([]);
        })
      ),
      gallery: this.galleryApi.listGalleryMedia().pipe(
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
        this.myEvents.set(Array.isArray(events) ? events : []);
        this.galleryItems.set(gallery && Array.isArray(gallery.items) ? gallery.items : []);
        this.storageUsage.set(storage);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Unexpected error loading profile data:', err);
        this.isLoading.set(false);
      },
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
