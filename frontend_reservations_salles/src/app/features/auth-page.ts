import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { NgIcon } from '@ng-icons/core';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-auth-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, NgIcon, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './auth-page.html',
  styleUrl: './auth-page.scss',
})
export class AuthPage {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly isRegister = this.route.snapshot.data['mode'] === 'register';
  readonly passwordVisible = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal('');
  readonly loginForm = this.formBuilder.nonNullable.group({
    email: ['sophie.martin@roomly.app', [Validators.required, Validators.email]],
    password: ['roomly2025', [Validators.required, Validators.minLength(8)]],
  });
  readonly registerForm = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    department: ['', Validators.required],
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required],
  });

  submit(): void {
    this.errorMessage.set('');
    if (this.isRegister) {
      if (this.registerForm.invalid) {
        this.registerForm.markAllAsTouched();
        return;
      }
      const { name, email, password, department, confirmPassword } = this.registerForm.getRawValue();
      if (password !== confirmPassword) {
        this.errorMessage.set('Your passwords don’t match. Please try again.');
        return;
      }
      this.submitting.set(true);
      this.auth.register({ name, email, password, department }).subscribe({
        next: () => this.finishSignIn(),
        error: () => {
          this.submitting.set(false);
          this.errorMessage.set('We couldn’t create your account. Please check your details and try again.');
        },
      });
      return;
    }

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.auth.login(this.loginForm.getRawValue()).subscribe({
      next: () => this.finishSignIn(),
      error: () => {
        this.submitting.set(false);
        this.errorMessage.set('That email and password combination didn’t work. Please try again.');
      },
    });
  }

  private finishSignIn(): void {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    const destination = returnUrl?.startsWith('/') && !returnUrl.startsWith('//') ? returnUrl : '/dashboard';
    void this.router.navigateByUrl(destination);
  }
}
