import { Component, OnInit, inject, ChangeDetectionStrategy, DestroyRef, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EventApiService } from '../../../core/api/services/event-api.service';
import { AccessStatusResponse, Event } from '../../../core/api/models/api.models';

@Component({
  selector: 'app-event-access-request',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  templateUrl: './event-access-request.component.html',
  styles: []
})
export class EventAccessRequestComponent implements OnInit {
  readonly eventId = input.required<string>();
  readonly event = input<Event | null>(null);
  readonly autoOpen = input<boolean>(false);
  readonly hideButton = input<boolean>(false);

  readonly requestSubmitted = output<void>();
  readonly requestWithdrawn = output<void>();

  private readonly api = inject(EventApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly showRequestButtonSignal = signal(false);
  get showRequestButton(): boolean { return this.showRequestButtonSignal(); }
  set showRequestButton(v: boolean) { this.showRequestButtonSignal.set(v); }

  readonly shouldShowComponentSignal = signal(false);
  get shouldShowComponent(): boolean { return this.shouldShowComponentSignal(); }
  set shouldShowComponent(v: boolean) { this.shouldShowComponentSignal.set(v); }

  readonly showRequestFormSignal = signal(false);
  get showRequestForm(): boolean { return this.showRequestFormSignal(); }
  set showRequestForm(v: boolean) { this.showRequestFormSignal.set(v); }

  readonly showReRequestFormSignal = signal(false);
  get showReRequestForm(): boolean { return this.showReRequestFormSignal(); }
  set showReRequestForm(v: boolean) { this.showReRequestFormSignal.set(v); }

  readonly requestMessageSignal = signal('');
  get requestMessage(): string { return this.requestMessageSignal(); }
  set requestMessage(v: string) { this.requestMessageSignal.set(v); }

  readonly reRequestMessageSignal = signal('');
  get reRequestMessage(): string { return this.reRequestMessageSignal(); }
  set reRequestMessage(v: string) { this.reRequestMessageSignal.set(v); }

  readonly isSubmittingSignal = signal(false);
  get isSubmitting(): boolean { return this.isSubmittingSignal(); }
  set isSubmitting(v: boolean) { this.isSubmittingSignal.set(v); }

  readonly requestStatusSignal = signal('none');
  get requestStatus(): string { return this.requestStatusSignal(); }
  set requestStatus(v: string) { this.requestStatusSignal.set(v); }

  readonly errorMessageSignal = signal('');
  get errorMessage(): string { return this.errorMessageSignal(); }
  set errorMessage(v: string) { this.errorMessageSignal.set(v); }

  readonly requestCreatedAtSignal = signal<Date | null>(null);
  get requestCreatedAt(): Date | null { return this.requestCreatedAtSignal(); }
  set requestCreatedAt(v: Date | null) { this.requestCreatedAtSignal.set(v); }

  readonly expiresAtSignal = signal<string | null>(null);
  get expiresAt(): string | null { return this.expiresAtSignal(); }
  set expiresAt(v: string | null) { this.expiresAtSignal.set(v); }

  readonly rejectionReasonSignal = signal('');
  get rejectionReason(): string { return this.rejectionReasonSignal(); }
  set rejectionReason(v: string) { this.rejectionReasonSignal.set(v); }

  readonly revokedAtSignal = signal<string | null>(null);
  get revokedAt(): string | null { return this.revokedAtSignal(); }
  set revokedAt(v: string | null) { this.revokedAtSignal.set(v); }

  ngOnInit() {
    this.showRequestForm = false;
    this.showReRequestForm = false;
    this.requestMessage = '';
    this.reRequestMessage = '';
    this.isSubmitting = false;
    this.requestStatus = 'none';
    this.requestCreatedAt = null;
    this.expiresAt = null;
    this.rejectionReason = '';
    this.revokedAt = null;
    this.errorMessage = '';

    this.checkAccessStatus();
  }

  get isModalMode(): boolean {
    return this.hideButton() || this.autoOpen();
  }

  openRequestForm() {
    this.requestMessage = 'Hey Owner..!! I want to access your event.';
    this.showRequestForm = true;
  }

  closeForm() {
    this.showRequestForm = false;
    this.showReRequestForm = false;
    if (this.hideButton()) {
      try { this.requestWithdrawn.emit(); } catch {}
    }
  }

  checkAccessStatus() {
    const id = this.eventId();
    if (!id) return;

    this.api.getAccessStatus(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (status: AccessStatusResponse) => {
        console.log('📋 Access status:', status);

        const rawStatus = (status.status || 'none').toString().toLowerCase();
        this.requestStatus = rawStatus;
        this.showRequestButton = status.showRequestButton === true || status.showRequestButton === 'true';
        this.shouldShowComponent = true;

        if (status.createdAt) {
          this.requestCreatedAt = new Date(status.createdAt);
        }
        if (status.expiresAt) {
          this.expiresAt = status.expiresAt;
        }
        if (status.rejectionReason) {
          this.rejectionReason = status.rejectionReason;
        }
        if (status.revokedAt) {
          this.revokedAt = status.revokedAt;
        }

        if (this.isModalMode && this.requestStatus !== 'pending' && this.requestStatus !== 'approved') {
          this.showRequestForm = true;
        }

        if (this.autoOpen() && this.requestStatus === 'none' && this.showRequestButton) {
          this.openRequestForm();
        }
      },
      error: (err) => {
        if (err?.status === 404) {
          console.log('ℹ️ No access requests yet - user can request access');
          this.errorMessage = '';
          this.requestStatus = 'none';
          this.showRequestButton = true;
          this.shouldShowComponent = true;
          this.showRequestForm = this.isModalMode;
          if (this.autoOpen()) {
            this.openRequestForm();
          }
        } else {
          console.error('❌ Failed to check access status:', err);
          this.errorMessage = 'Unable to verify access status. Please try again later.';
          this.requestStatus = 'error';
          this.showRequestButton = false;
          this.shouldShowComponent = false;
          this.showRequestForm = false;
          this.showReRequestForm = false;
        }
      }
    });
  }

  submitRequest() {
    this.isSubmitting = true;
    const id = this.eventId();
    const request$ = ['expired', 'revoked', 'rejected'].includes(this.requestStatus)
      ? this.api.reRequestAccess(id, this.requestMessage || undefined)
      : this.api.requestEventAccess(id, this.requestMessage || undefined);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.requestStatus = 'pending';
        this.requestCreatedAt = new Date();
        this.showRequestForm = false;
        this.showReRequestForm = false;
        this.requestMessage = '';
        this.reRequestMessage = '';
        this.isSubmitting = false;
        console.log('✅ Access request sent successfully');
        try { this.requestSubmitted.emit(); } catch {}
      },
      error: (err) => {
        this.isSubmitting = false;
        console.error('❌ Failed to send access request:', err);
        alert('Error: ' + (err.error?.error || 'Failed to submit access request'));
      }
    });
  }

  submitReRequest() {
    this.isSubmitting = true;
    const id = this.eventId();
    this.api.reRequestAccess(id, this.reRequestMessage || undefined).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.requestStatus = 'pending';
        this.showReRequestForm = false;
        this.reRequestMessage = '';
        this.isSubmitting = false;
        console.log('✅ Access re-request sent successfully');
        try { this.requestSubmitted.emit(); } catch {}
      },
      error: (err) => {
        this.isSubmitting = false;
        console.error('❌ Failed to re-request access:', err);
        alert('Error: ' + (err.error?.error || 'Failed to submit re-request'));
      }
    });
  }

  withdrawRequest() {
    if (confirm('Are you sure you want to withdraw your request?')) {
      this.isSubmitting = true;
      this.requestStatus = 'none';
      this.isSubmitting = false;
      try { this.requestWithdrawn.emit(); } catch {}
    }
  }
}
