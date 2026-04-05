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
  template: `
    <!-- Shows if event is PROTECTED and user is not owner and doesn't have access -->
    <div *ngIf="showRequestButton" class="space-y-4">
      <!-- Status States -->
      <div *ngIf="requestStatus === 'none'" class="text-center">
        <button
          (click)="openRequestForm()"
          class="px-6 py-3 bg-orange-600 hover:bg-orange-700 rounded-lg font-semibold">
          🔓 Request Access to This Event
        </button>
        <p class="text-slate-400 mt-2">This event is protected. Contact owner for access.</p>
      </div>

      <div *ngIf="requestStatus === 'pending'" class="bg-yellow-500/10 border border-yellow-500/30 rounded-lg p-4">
        <p class="text-yellow-400 font-semibold">⏳ Pending Approval</p>
        <p class="text-sm text-slate-400">Waiting for owner to review your request</p>
        <p class="text-xs text-slate-500 mt-2" *ngIf="requestCreatedAt">Requested: {{ requestCreatedAt | date:'short' }}</p>
        <button
          (click)="withdrawRequest()"
          [disabled]="isSubmitting"
          class="mt-3 px-4 py-2 bg-red-600 hover:bg-red-700 disabled:bg-slate-600 rounded text-sm font-semibold">
          {{ isSubmitting ? '⏳ Withdrawing...' : '✕ Withdraw Request' }}
        </button>
      </div>

      <div *ngIf="requestStatus === 'approved'" class="bg-green-500/10 border border-green-500/30 rounded-lg p-4">
        <p class="text-green-400 font-semibold">✅ Access Approved</p>
        <p class="text-sm text-slate-400">You can now view and upload media to this event</p>
      </div>

      <div *ngIf="requestStatus === 'rejected'" class="bg-red-500/10 border border-red-500/30 rounded-lg p-4">
        <p class="text-red-400 font-semibold">❌ Request Denied</p>
        <p class="text-sm text-slate-400">{{ rejectionReason }}</p>
        <button
          (click)="submitRequest()"
          class="mt-3 px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded text-sm font-semibold">
          Request Again
        </button>
      </div>

      <!-- Request Form Modal -->
      <div *ngIf="showRequestForm" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
        <div class="bg-slate-800 rounded-lg p-6 max-w-md w-full mx-4">
          <h3 class="text-xl font-bold mb-4">Request Access</h3>
          <p class="text-slate-400 mb-4">Send a message with your request (optional):</p>

          <textarea
            [(ngModel)]="requestMessage"
            placeholder="Hey Owner..!! I want to Enter in your Event."
            class="w-full bg-slate-700 rounded p-3 text-white mb-4 h-24 border border-slate-600 focus:border-blue-500 outline-none"></textarea>

          <div class="flex gap-3">
            <button
              (click)="submitRequest()"
              [disabled]="isSubmitting"
              class="flex-1 bg-blue-600 hover:bg-blue-700 disabled:bg-slate-600 py-2 rounded font-semibold">
              {{ isSubmitting ? '⏳ Requesting...' : '📤 Send Request' }}
            </button>
            <button
              (click)="showRequestForm = false"
              class="flex-1 bg-slate-700 hover:bg-slate-600 py-2 rounded font-semibold">
              Cancel
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
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
