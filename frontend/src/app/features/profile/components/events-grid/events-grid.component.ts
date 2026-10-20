import { Component, ChangeDetectionStrategy, input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Event, SangrahApiService } from '../../../../core/api/sangrah-api.service';

@Component({
  selector: 'app-events-grid',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink],
  templateUrl: './events-grid.component.html',
})
export class EventsGridComponent {
  private api = inject(SangrahApiService);

  events = input<Event[]>([]);

  getCoverImageUrl(event: Event): string | null {
    if (event.coverImageId) {
      return this.api.getMediaFileUrl(event.coverImageId);
    }
    return null;
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    } catch {
      return dateStr;
    }
  }
}
