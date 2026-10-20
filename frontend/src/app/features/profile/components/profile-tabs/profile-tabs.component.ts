import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

export type ProfileTabType = 'events' | 'gallery' | 'saved';

@Component({
  selector: 'app-profile-tabs',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink],
  templateUrl: './profile-tabs.component.html',
})
export class ProfileTabsComponent {
  activeTab = input<ProfileTabType>('events');
  tabChanged = output<ProfileTabType>();

  selectTab(tab: ProfileTabType): void {
    this.tabChanged.emit(tab);
  }
}
