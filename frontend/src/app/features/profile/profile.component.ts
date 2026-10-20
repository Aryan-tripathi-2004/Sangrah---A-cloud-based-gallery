import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { SangrahApiService, User, Event, MediaItem, StorageUsage } from '../../core/api/sangrah-api.service';
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
  private api = inject(SangrahApiService);

  user = signal<User | null>(null);
  myEvents = signal<Event[]>([]);
  galleryItems = signal<MediaItem[]>([]);
  storageUsage = signal<StorageUsage | null>(null);

  activeTab = signal<ProfileTabType>('events');
  isLoading = signal(true);
  isUploadingAvatar = signal(false);
  isSavingBio = signal(false);

  feedbackMessage = signal<{ type: 'success' | 'error'; text: string } | null>(null);

  ngOnInit(): void {
    this.loadAllProfileData();
  }

  loadAllProfileData(): void {
    this.isLoading.set(true);

    forkJoin({
      profile: this.api.getProfile(),
      events: this.api.getMyEvents(),
      gallery: this.api.listGalleryMedia(),
      storage: this.api.getStorageUsageGallery(),
    }).subscribe({
      next: ({ profile, events, gallery, storage }) => {
        this.user.set(profile);
        this.myEvents.set(Array.isArray(events) ? events : []);
        this.galleryItems.set(gallery && Array.isArray(gallery.items) ? gallery.items : []);
        this.storageUsage.set(storage);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load profile data:', err);
        // Fallback: at least load profile
        this.api.getProfile().subscribe({
          next: (profile) => this.user.set(profile),
          error: () => {},
        });
        this.isLoading.set(false);
      },
    });
  }

  onAvatarSelected(file: File): void {
    this.isUploadingAvatar.set(true);
    this.showFeedback(null);

    this.api.uploadProfileAvatar(file).subscribe({
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

    this.api.updateProfile({ bio: newBio }).subscribe({
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
