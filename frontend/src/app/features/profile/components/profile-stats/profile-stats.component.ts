import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-profile-stats',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  templateUrl: './profile-stats.component.html',
})
export class ProfileStatsComponent {
  eventsCount = input<number>(0);
  galleryCount = input<number>(0);
}
