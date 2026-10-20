import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { SangrahApiService, User } from '../../core/api/sangrah-api.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-settings',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink, LayoutComponent],
  templateUrl: './settings.component.html',
})
export class SettingsComponent implements OnInit {
  private api = inject(SangrahApiService);
  private fb = inject(FormBuilder);
  private router = inject(Router);

  user = signal<User | null>(null);
  isLoading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');

  showDeleteConfirm = signal(false);
  deleteConfirmInput = signal('');
  isDeleting = signal(false);
  deleteError = signal('');

  form = this.fb.group({
    email: [{ value: '', disabled: true }],
    displayName: ['', [Validators.required, Validators.maxLength(50)]],
  });

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.api.getProfile().subscribe({
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

    const formValue = this.form.value as Partial<User>;
    this.api.updateProfile(formValue).subscribe({
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
    this.api.deleteAccount().subscribe({
      next: () => {
        this.api.logout();
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.isDeleting.set(false);
        this.deleteError.set(err.message || err.error?.message || 'Failed to delete account. Please try again.');
      },
    });
  }
}
