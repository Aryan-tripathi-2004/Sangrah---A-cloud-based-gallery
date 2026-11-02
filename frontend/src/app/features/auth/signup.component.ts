import { Component, inject, ChangeDetectionStrategy, signal, DestroyRef } from '@angular/core';
import { FormGroup, FormControl, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly form = new FormGroup(
    {
      displayName: new FormControl('', { validators: [Validators.required], nonNullable: true }),
      email: new FormControl('', { validators: [Validators.required, Validators.email], nonNullable: true }),
      password: new FormControl('', { validators: [Validators.required, Validators.minLength(8)], nonNullable: true }),
      confirmPassword: new FormControl('', { validators: [Validators.required], nonNullable: true }),
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

    const { email, password, displayName } = this.form.getRawValue();

    this.authService.register(email.trim(), password, displayName.trim())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
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
