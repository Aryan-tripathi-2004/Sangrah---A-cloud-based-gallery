import { Component, inject, ChangeDetectionStrategy, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-signup',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, LayoutComponent],
  templateUrl: './signup.component.html',
})
export class SignupComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);

  form = this.fb.group(
    {
      displayName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: this.passwordsMatchValidator }
  );

  isLoading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');

  passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;

    if (password && confirmPassword && password !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  onSubmit(): void {
    if (this.form.invalid) return;

    this.isLoading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const { email, password, displayName } = this.form.value;
    if (!email || !password || !displayName) return;

    this.authService.register(email, password, displayName).subscribe({
      next: () => {
        this.successMessage.set('Account created successfully! Redirecting...');
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/dashboard';
        setTimeout(() => {
          this.router.navigateByUrl(returnUrl);
        }, 1000);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.message || err.error?.message || 'Signup failed. Please try again.');
      },
    });
  }
}
