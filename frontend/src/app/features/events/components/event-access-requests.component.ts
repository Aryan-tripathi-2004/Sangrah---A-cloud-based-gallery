import { Component, Input, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SangrahApiService } from '../../../core/api/sangrah-api.service';

interface AccessRequest {
  requestId: string;
  requesterUserId: string;
  displayName?: string;
  message: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'REVOKED';
  requestedAt: string;
}

@Component({
  selector: 'app-event-access-requests',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './event-access-requests.component.html',
  styles: []
})
export class EventAccessRequestsComponent implements OnInit {
  @Input() eventId!: string;

  private api = inject(SangrahApiService);
  private cdr = inject(ChangeDetectorRef);

  requests: AccessRequest[] = [];
  filterStatus: 'PENDING' | 'APPROVED' = 'PENDING';
  processingId: string | null = null;

  approveFormId: string | null = null;
  approveDuration: 'FOREVER' | 'UNTIL_DATE' = 'FOREVER';
  approveUntilDate: string = '';
  minDate: string = '';

  rejectFormId: string | null = null;
  rejectionReason = '';

  ngOnInit() {
    // set minimum selectable date (today) for expiry inputs
    const d = new Date();
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    this.minDate = `${yyyy}-${mm}-${dd}`;
    this.loadRequests();
  }

  loadRequests() {
    this.api.listAccessRequests(this.eventId).subscribe({
      next: (data: any) => {
        // Normalize status values to uppercase so filtering works reliably
        // Handle both array response and object response structures
        let requests = data.requests || data || [];
        if (!Array.isArray(requests)) {
          requests = [];
        }
        this.requests = requests.map((r: any) => ({
          ...r,
          status: (r.status || '').toString().toUpperCase()
        }));
        console.log('✅ Loaded access requests:', this.requests);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('❌ Failed to load requests:', err);
      }
    });
  }

  getFilteredRequests(): AccessRequest[] {
    return this.requests.filter(r => r.status === this.filterStatus);
  }

  getPendingCount(): number {
    return this.requests.filter(r => r.status === 'PENDING').length;
  }

  getApprovedCount(): number {
    return this.requests.filter(r => r.status === 'APPROVED').length;
  }

  private updateRequestStatus(requestId: string, status: AccessRequest['status']): void {
    const request = this.requests.find(r => r.requestId === requestId);
    if (request) {
      request.status = status;
    }
  }

  openApproveForm(requestId: string) {
    this.approveFormId = requestId;
    this.approveDuration = 'FOREVER';
    this.approveUntilDate = '';
  }

  confirmApprove(requestId: string) {
    this.processingId = requestId;
    const payload: any = {
      approvalDuration: this.approveDuration
    };
    if (this.approveDuration === 'UNTIL_DATE' && this.approveUntilDate) {
      // Prevent past dates and set to end-of-day to avoid immediate expiry due to timezone
      const selected = new Date(this.approveUntilDate);
      const today = new Date();
      const selStart = new Date(selected.getFullYear(), selected.getMonth(), selected.getDate());
      const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate());
      if (selStart < todayStart) {
        alert('Expiry date cannot be in the past. Please select today or a future date.');
        this.processingId = null;
        return;
      }
      // Set expiry to end of selected day (23:59:59)
      const endOfDay = new Date(selected.getFullYear(), selected.getMonth(), selected.getDate(), 23, 59, 59, 999);
      payload.accessExpiresAt = endOfDay.toISOString();
    }

    this.api.approveAccessRequest(this.eventId, requestId, payload).subscribe({
      next: () => {
        console.log('✅ Request approved');
        this.approveFormId = null;
        this.processingId = null;
        this.loadRequests();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('❌ Failed to approve:', err);
        this.processingId = null;
      }
    });
  }

  openRejectForm(requestId: string) {
    this.rejectFormId = requestId;
    this.rejectionReason = '';
  }

  confirmReject(requestId: string) {
    this.processingId = requestId;
    const payload = {
      reason: this.rejectionReason || 'Request denied'
    };

    this.api.rejectAccessRequest(this.eventId, requestId, payload).subscribe({
      next: () => {
        console.log('✅ Request rejected');
        this.rejectFormId = null;
        this.processingId = null;
        this.loadRequests();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('❌ Failed to reject:', err);
        this.processingId = null;
      }
    });
  }

  // Allow owner to revoke an approved access
  openRevoke(requestId: string) {
    if (!confirm('Are you sure you want to revoke access for this user?')) return;
    this.processingId = requestId;
    this.api.revokeAccessRequest(this.eventId, requestId).subscribe({
      next: () => {
        this.updateRequestStatus(requestId, 'REVOKED');
        this.processingId = null;
        this.loadRequests();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('❌ Failed to revoke access:', err);
        alert('Error: ' + (err.error?.error || 'Failed to revoke access'));
        this.processingId = null;
        this.cdr.detectChanges();
      }
    });
  }
}
