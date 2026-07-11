import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { SangrahApiService, User } from '../../core/api/sangrah-api.service';
import { LayoutComponent } from '../../shared/layout/layout.component';
import {Router} from '@angular/router';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule,FormsModule, LayoutComponent],
  templateUrl: './profile.component.html',
  
})
export class ProfileComponent implements OnInit {
  private api = inject(SangrahApiService);
  private fb = inject(FormBuilder);
  private router = inject(Router);

  user: User | null = null;
  form = this.fb.group({
    email: [{ value: '', disabled: true }],
    displayName: ['', [Validators.required]],
  });

  isLoading = false;
  errorMessage = '';
  successMessage = '';

  showDeleteConfirm = false;
  deleteConfirmInput = '';
  isDeleting = false;
  deleteError = '';

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

  openDeleteConfirm(): void {
    this.showDeleteConfirm = true;
    this.deleteConfirmInput = '';
    this.deleteError = '';
  }

  cancelDelete(): void{
    this.showDeleteConfirm = false;
  }

  confirmDelete(): void {
    if (!this.user || this.deleteConfirmInput !== this.user.email){
      this.deleteError = 'Email does not match';
      return;
    }
    this.isDeleting = true;
    this.api.deleteAccount().subscribe({
      next: () => {
        this.api.logout();
        this.router.navigate(['/']);
      },
      error: () => {
        this.isDeleting = false;
        this.deleteError = 'Failed to delete account. Please try again.';
      }
    });
  }
}
