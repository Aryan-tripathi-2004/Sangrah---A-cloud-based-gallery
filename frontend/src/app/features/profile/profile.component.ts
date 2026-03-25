import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { SangrahApiService, User } from '../../core/api/sangrah-api.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LayoutComponent],
  template: `
    <app-layout>
      <div class="max-w-2xl mx-auto space-y-8">
        <!-- Header -->
        <div>
          <h1 class="text-4xl font-bold mb-2">Profile Settings</h1>
          <p class="text-slate-400">Manage your account and preferences</p>
        </div>

        <!-- Profile Card -->
        <div class="bg-slate-800/30 border border-slate-700/50 rounded-2xl p-8">
          <!-- Profile Header -->
          <div class="flex items-center gap-6 mb-8 pb-8 border-b border-slate-700">
            <div class="w-20 h-20 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-4xl">
              👤
            </div>
            <div class="flex-1">
              <h2 class="text-2xl font-bold mb-1">{{ user?.displayName || 'User' }}</h2>
              <p class="text-slate-400">{{ user?.email }}</p>
              <p class="text-sm text-slate-500 mt-2">Member since {{ user?.createdAt | date:'MMM d, y' }}</p>
            </div>
          </div>

          <!-- Form -->
          <form [formGroup]="form" (ngSubmit)="onSubmit()" class="space-y-6">
            <!-- Display Name -->
            <div>
              <label class="block text-sm font-semibold mb-2">Full Name</label>
              <input
                type="text"
                formControlName="displayName"
                class="w-full px-4 py-3 rounded-lg bg-slate-700/50 border border-slate-600 focus:border-blue-500 focus:outline-none transition text-slate-100"
              />
            </div>

            <!-- Email -->
            <div>
              <label class="block text-sm font-semibold mb-2">Email Address</label>
              <input
                type="email"
                formControlName="email"
                class="w-full px-4 py-3 rounded-lg bg-slate-700/50 border border-slate-600 text-slate-400 cursor-not-allowed"
              />
              <p class="text-xs text-slate-400 mt-1">Email cannot be changed</p>
            </div>

            <!-- Messages -->
            <div *ngIf="errorMessage" class="p-4 rounded-lg bg-red-500/10 border border-red-500/30 text-red-400">
              {{ errorMessage }}
            </div>
            <div *ngIf="successMessage" class="p-4 rounded-lg bg-green-500/10 border border-green-500/30 text-green-400">
              {{ successMessage }}
            </div>

            <!-- Submit Button -->
            <button
              type="submit"
              [disabled]="form.invalid || isLoading"
              class="w-full px-4 py-3 rounded-lg bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 disabled:opacity-50 disabled:cursor-not-allowed font-semibold transition"
            >
              {{ isLoading ? 'Saving...' : 'Save Changes' }}
            </button>
          </form>
        </div>

        <!-- Danger Zone -->
        <div class="bg-red-500/5 border border-red-500/30 rounded-2xl p-8">
          <h3 class="text-xl font-bold text-red-400 mb-4">Danger Zone</h3>
          <p class="text-slate-400 mb-6">These actions cannot be undone.</p>
          <button class="px-6 py-3 bg-red-600/20 hover:bg-red-600/30 border border-red-600 text-red-400 rounded-lg font-semibold transition">
            Delete Account
          </button>
        </div>
      </div>
    </app-layout>
  `,
})
export class ProfileComponent implements OnInit {
  private api = inject(SangrahApiService);
  private fb = inject(FormBuilder);

  user: User | null = null;
  form = this.fb.group({
    email: [{ value: '', disabled: true }],
    displayName: ['', [Validators.required]],
  });

  isLoading = false;
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.api.getProfile().subscribe({
      next: (user) => {
        this.user = user;
        this.form.patchValue(user);
      },
      error: () => {
        this.errorMessage = 'Failed to load profile';
      },
    });
  }

  onSubmit(): void {
    if (this.form.invalid || !this.user) return;

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const formValue = this.form.value as Partial<User>;
    this.api.updateProfile(formValue).subscribe({
      next: (updated) => {
        this.user = updated;
        this.successMessage = 'Profile updated successfully!';
        this.isLoading = false;
        setTimeout(() => {
          this.successMessage = '';
        }, 3000);
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Failed to update profile';
      },
    });
  }
}
