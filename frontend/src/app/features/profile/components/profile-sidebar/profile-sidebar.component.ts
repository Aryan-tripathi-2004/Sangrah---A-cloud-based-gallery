import { Component, ChangeDetectionStrategy, input, output, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { User, StorageUsage, SangrahApiService } from '../../../../core/api/sangrah-api.service';
import { AvatarUploadComponent } from '../avatar-upload/avatar-upload.component';
import { ProfileStatsComponent } from '../profile-stats/profile-stats.component';
import { BioEditComponent } from '../bio-edit/bio-edit.component';

@Component({
  selector: 'app-profile-sidebar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    AvatarUploadComponent,
    ProfileStatsComponent,
    BioEditComponent,
  ],
  templateUrl: './profile-sidebar.component.html',
})
export class ProfileSidebarComponent {
  private api = inject(SangrahApiService);

  user = input<User | null>(null);
  eventsCount = input<number>(0);
  galleryCount = input<number>(0);
  storageUsage = input<StorageUsage | null>(null);
  isUploadingAvatar = input<boolean>(false);
  isSavingBio = input<boolean>(false);

  avatarSelected = output<File>();
  bioSaved = output<string>();

  avatarDisplayUrl = computed(() => this.api.getAvatarUrl(this.user()?.avatarUrl));

  memberSince = computed(() => {
    const raw = this.user()?.createdAt;
    if (!raw) return 'Recently';
    try {
      const d = new Date(raw);
      return d.toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
    } catch {
      return 'Recently';
    }
  });

  storagePercent = computed(() => {
    const usage = this.storageUsage();
    if (!usage || !usage.totalBytesUsed) return 0;
    // Default 5GB limit (5368709120 bytes)
    const limit = 5368709120;
    return Math.min(100, Math.round((usage.totalBytesUsed / limit) * 100));
  });

  formattedStorage = computed(() => {
    const usage = this.storageUsage();
    if (usage && usage.formatted && usage.formatted.totalFormatted) {
      return usage.formatted.totalFormatted;
    }
    return '0 MB';
  });
}
