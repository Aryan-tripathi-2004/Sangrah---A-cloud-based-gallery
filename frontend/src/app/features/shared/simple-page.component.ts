import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-simple-page',
  standalone: true,
  template: `
    <section style="padding: 1rem;">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
    </section>
  `
})
export class SimplePageComponent {
  private readonly route = inject(ActivatedRoute);
  protected readonly title = this.route.snapshot.data['title'] ?? 'Page';
  protected readonly description = this.route.snapshot.data['description'] ?? 'Coming soon';
}
