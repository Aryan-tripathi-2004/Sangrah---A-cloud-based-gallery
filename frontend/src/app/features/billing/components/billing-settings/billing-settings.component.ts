import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-billing-settings',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  templateUrl: './billing-settings.component.html'
})
export class BillingSettingsComponent {
  constructor() {}
}
