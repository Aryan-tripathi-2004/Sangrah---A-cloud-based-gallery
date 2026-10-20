import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-simple-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './simple-page.component.html'
})
export class SimplePageComponent {
  private readonly route = inject(ActivatedRoute);
  protected readonly title = this.route.snapshot.data['title'] ?? 'Page';
  protected readonly description = this.route.snapshot.data['description'] ?? 'Coming soon';
}
