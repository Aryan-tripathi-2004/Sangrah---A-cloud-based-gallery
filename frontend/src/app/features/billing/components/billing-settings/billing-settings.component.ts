import { Component, ChangeDetectionStrategy, inject, OnInit, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UserApiService } from '../../../../core/api/services/user-api.service';
import { User } from '../../../../core/api/models/api.models';

@Component({
  selector: 'app-billing-settings',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  templateUrl: './billing-settings.component.html'
})
export class BillingSettingsComponent implements OnInit {
  private readonly userApi = inject(UserApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly user = signal<User | null>(null);

  ngOnInit(): void {
    this.userApi.getProfile().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (profile) => this.user.set(profile),
      error: (err) => console.warn('Failed to load profile for billing settings:', err)
    });
  }
}
