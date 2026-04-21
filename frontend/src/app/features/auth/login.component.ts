import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, LayoutComponent],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  isLoading = false;
  errorMessage = '';
  successMessage = '';

  onSubmit(): void {
    if (this.form.invalid) return;

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const { email, password } = this.form.value;
    if (!email || !password) return;

    this.authService.login(email, password).subscribe({
      next: () => {
        this.successMessage = 'Login successful! Redirecting...';
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/dashboard';
        setTimeout(() => {
          this.router.navigateByUrl(returnUrl);
        }, 1000);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Login failed. Please try again.';
      },
    });
  }
}
