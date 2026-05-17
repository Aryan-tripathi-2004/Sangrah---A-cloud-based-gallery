import { Component, Input, OnInit, ChangeDetectorRef, inject, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SangrahApiService } from '../../../core/api/sangrah-api.service';

interface Event {
  id: string;
  visibility: string;
  ownerUserId: string;
}

@Component({
  selector: 'app-event-access-request',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './event-access-request.component.html',
  styles: []
})
export class EventAccessRequestComponent implements OnInit {
  @Input() eventId!: string;
  @Input() event: Event | null = null;
  @Input() autoOpen: boolean = false; // when true, open request form automatically
  @Input() hideButton: boolean = false; // when true, hide the request button (modal mode)
  @Output() requestSubmitted: EventEmitter<void> = new EventEmitter();
  @Output() requestWithdrawn: EventEmitter<void> = new EventEmitter();

  private api = inject(SangrahApiService);
  private cdr = inject(ChangeDetectorRef);

  showRequestButton = false;
  shouldShowComponent = false; // NEW: Show component when user doesn't have access
  showRequestForm = false;
  showReRequestForm = false;
  requestMessage = '';
  reRequestMessage = '';
  isSubmitting = false;

  // Status: 'none' | 'pending' | 'approved' | 'expired' | 'rejected' | 'revoked'
  requestStatus: string = 'none';
  requestCreatedAt: Date | null = null;
  expiresAt: string | null = null;
  rejectionReason = '';
  revokedAt: string | null = null;

  ngOnInit() {
    // Reset all form states when component is initialized (or recreated)
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
    
    // Check access status and auto-open if needed
    this.checkAccessStatus();
  }

  get isModalMode(): boolean {
    return this.hideButton || this.autoOpen;
  }

  openRequestForm() {
    // Reset message to default when opening form
    this.requestMessage = 'Hey Owner..!! I want to access your event.';
    this.showRequestForm = true;
  }

  closeForm() {
    // Close the form and emit withdrawn event if in modal mode (hideButton=true)
    this.showRequestForm = false;
    this.showReRequestForm = false;
    if (this.hideButton) {
      try { this.requestWithdrawn.emit(); } catch {}
    }
  }

  checkAccessStatus() {
    // NEW: Use dedicated access-status endpoint instead of listing all requests
    this.api.getAccessStatus(this.eventId).subscribe({
      next: (status: any) => {
        console.log('📋 Access status:', status);

        // Normalize status to lowercase for consistent template comparisons
        const rawStatus = (status.status || 'none').toString().toLowerCase();
        this.requestStatus = rawStatus;
        // Handle both boolean and string values for showRequestButton
        this.showRequestButton = status.showRequestButton === true || status.showRequestButton === 'true';
        
        // Show component if user doesn't have access - show for ALL access-related statuses
        // (none, pending, approved, expired, rejected, revoked)
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

        // In modal mode, open the request form immediately instead of showing a status shell.
        if (this.isModalMode && this.requestStatus !== 'pending' && this.requestStatus !== 'approved') {
          this.showRequestForm = true;
        }

        // If caller requested auto-open and there's no existing request, open form
        if (this.autoOpen && this.requestStatus === 'none' && this.showRequestButton) {
          this.openRequestForm();
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.log('ℹ️ No access requests yet - user can request access');
        this.requestStatus = 'none';
        this.showRequestButton = true;
        this.shouldShowComponent = true;
        this.showRequestForm = this.isModalMode;
        if (this.autoOpen) {
          this.openRequestForm();
        }
        this.cdr.detectChanges();
      }
    });
  }

  submitRequest() {
    this.isSubmitting = true;
    const request$ = ['expired', 'revoked', 'rejected'].includes(this.requestStatus)
      ? this.api.reRequestAccess(this.eventId, this.requestMessage || undefined)
      : this.api.requestEventAccess(this.eventId, this.requestMessage || undefined);

    request$.subscribe({
      next: (response) => {
        this.requestStatus = 'pending';
        this.requestCreatedAt = new Date();
        this.showRequestForm = false;
        this.showReRequestForm = false;
        this.requestMessage = '';
        this.reRequestMessage = '';
        this.isSubmitting = false;
        // Toast: "Request sent!"
        console.log('✅ Access request sent successfully');
        // Notify parent components that a request was submitted
        try { this.requestSubmitted.emit(); } catch {}
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isSubmitting = false;
        console.error('❌ Failed to send access request:', err);
        alert('Error: ' + (err.error?.error || 'Failed to submit access request'));
        this.cdr.detectChanges();
      }
    });
  }

  // NEW: Handle re-request after expiration or revocation
  submitReRequest() {
    this.isSubmitting = true;
    this.api.reRequestAccess(this.eventId, this.reRequestMessage || undefined).subscribe({
      next: () => {
        this.requestStatus = 'pending';
        this.showReRequestForm = false;
        this.reRequestMessage = '';
        this.isSubmitting = false;
        console.log('✅ Access re-request sent successfully');
        try { this.requestSubmitted.emit(); } catch {}
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isSubmitting = false;
        console.error('❌ Failed to re-request access:', err);
        alert('Error: ' + (err.error?.error || 'Failed to submit re-request'));
      }
    });
  }

  withdrawRequest() {
    // NEW: Allow user to withdraw/unsubmit their pending request
    if (confirm('Are you sure you want to withdraw your request?')) {
      this.isSubmitting = true;
      // Call a hypothetical endpoint to withdraw the request
      // For now, we'll mark it as 'none' since the backend may not have this endpoint yet
      console.log('Withdrawing request for event:', this.eventId);

      // In a real implementation, call backend endpoint:
      // this.api.withdrawAccessRequest(this.eventId).subscribe({
      //   next: () => {
      //     this.requestStatus = 'none';
      //     this.isSubmitting = false;
      //     this.cdr.detectChanges();
      //   },
      //   error: (err) => {
      //     this.isSubmitting = false;
      //     console.error('Failed to withdraw request:', err);
      //   }
      // });

      // For now, just reset the state
      this.requestStatus = 'none';
      this.isSubmitting = false;
      try { this.requestWithdrawn.emit(); } catch {}
      this.cdr.detectChanges();
    }
  }
}
