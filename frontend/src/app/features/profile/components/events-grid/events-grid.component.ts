import { Component, ChangeDetectionStrategy, input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Event } from '../../../../core/api/models/api.models';
import { EventApiService } from '../../../../core/api/services/event-api.service';

@Component({
  selector: 'app-events-grid',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink],
  templateUrl: './events-grid.component.html',
})
export class EventsGridComponent {
  private readonly eventApi = inject(EventApiService);

  events = input<Event[]>([]);

  getCoverImageUrl(event: Event): string | null {
    if (event.coverImageId) {
      return this.eventApi.getMediaFileUrl(event.coverImageId);
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
