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
  @Output() requestSubmitted: EventEmitter<void> = new EventEmitter();
  @Output() requestWithdrawn: EventEmitter<void> = new EventEmitter();

  private api = inject(SangrahApiService);
  private cdr = inject(ChangeDetectorRef);

  showRequestButton = false;
  showRequestForm = false;
  requestMessage = '';
  isSubmitting = false;

 // Status: 'none' | 'pending' | 'approved' | 'rejected'
  requestStatus: string = 'none';
  requestCreatedAt: Date | null = null;
  rejectionReason = '';

  ngOnInit() {
    this.checkAccessStatus();
  }

  openRequestForm() {
    // Reset message to default when opening form
    this.requestMessage = 'Hey Owner..!! I want to Enter in your Event.';
    this.showRequestForm = true;
  }

  checkAccessStatus() {
    // Check if user has pending, approved, or rejected request
    this.api.getEventAccessRequests(this.eventId).subscribe({
      next: (requests: any[]) => {
        console.log('📋 Access requests:', requests);
        
        // Find this user's request (in real app, would filter by current user ID)
        if (requests && requests.length > 0) {
          const userRequest = requests[0]; // Simplified: just check first request
          this.requestStatus = userRequest.status?.toLowerCase() || 'none';
          this.requestCreatedAt = userRequest.createdAt ? new Date(userRequest.createdAt) : null;
          if (userRequest.status === 'REJECTED') {
            this.rejectionReason = userRequest.rejectionReason || 'Your request was not approved';
          }
        } else {
          this.requestStatus = 'none';
        }
        this.showRequestButton = true;
        // If caller requested auto-open and there's no existing request, open form
        if (this.autoOpen && this.requestStatus === 'none') {
          this.openRequestForm();
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.log('ℹ️ No access requests yet - user can request access');
        this.requestStatus = 'none';
        this.showRequestButton = true;
        if (this.autoOpen) {
          this.openRequestForm();
        }
        this.cdr.detectChanges();
      }
    });
  }

  submitRequest() {
    this.isSubmitting = true;
    this.api.requestEventAccess(this.eventId, this.requestMessage || undefined).subscribe({
      next: (response) => {
        this.requestStatus = 'pending';
        this.requestCreatedAt = new Date();
        this.showRequestForm = false;
        this.requestMessage = '';
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
        // Toast: error message
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
