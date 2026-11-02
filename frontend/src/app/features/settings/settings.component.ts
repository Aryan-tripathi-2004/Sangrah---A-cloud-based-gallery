import { Component, OnInit, inject, signal, ChangeDetectionStrategy, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormGroup, FormControl, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { User } from '../../core/api/models/api.models';
import { UserApiService } from '../../core/api/services/user-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-settings',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink, LayoutComponent],
  templateUrl: './settings.component.html',
})
export class SettingsComponent implements OnInit {
  private readonly userApi = inject(UserApiService);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  user = signal<User | null>(null);
  isLoading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');

  showDeleteConfirm = signal(false);
  deleteConfirmInput = signal('');
  isDeleting = signal(false);
  deleteError = signal('');

  readonly form = new FormGroup({
    email: new FormControl({ value: '', disabled: true }, { nonNullable: true }),
    displayName: new FormControl('', {
      validators: [Validators.required, Validators.maxLength(50)],
      nonNullable: true,
    }),
  });

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.userApi.getProfile()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (userData) => {
          this.user.set(userData);
          this.form.patchValue({
            email: userData.email,
            displayName: userData.displayName,
          });
        },
        error: (err) => {
          this.errorMessage.set(err.message || err.error?.message || 'Failed to load profile');
        },
      });
  }

  onSubmit(): void {
    if (this.form.invalid || !this.user()) return;

    this.isLoading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const formValue = this.form.getRawValue();
    this.userApi.updateProfile({ displayName: formValue.displayName.trim() })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.user.set(updated);
          this.successMessage.set('Account settings updated successfully!');
          this.isLoading.set(false);
          setTimeout(() => this.successMessage.set(''), 3000);
        },
        error: (err) => {
          this.isLoading.set(false);
          this.errorMessage.set(err.message || err.error?.message || 'Failed to update account settings');
        },
      });
  }

  openDeleteConfirm(): void {
    this.showDeleteConfirm.set(true);
    this.deleteConfirmInput.set('');
    this.deleteError.set('');
  }

  cancelDelete(): void {
    this.showDeleteConfirm.set(false);
  }

  confirmDelete(): void {
    const currentUser = this.user();
    if (!currentUser || this.deleteConfirmInput() !== currentUser.email) {
      this.deleteError.set('Email does not match');
      return;
    }
    this.isDeleting.set(true);
    this.userApi.deleteAccount()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.authService.logout();
        },
        error: (err) => {
          this.isDeleting.set(false);
          this.deleteError.set(err.message || err.error?.message || 'Failed to delete account. Please try again.');
        },
      });
  }
}
