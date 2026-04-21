import { Component, Input, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SangrahApiService } from '../../../core/api/sangrah-api.service';

interface AccessRequest {
  requestId: string;
  requesterUserId: string;
  displayName?: string;
  message: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
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

  rejectFormId: string | null = null;
  rejectionReason = '';

  ngOnInit() {
    this.loadRequests();
  }

  loadRequests() {
    this.api.listAccessRequests(this.eventId).subscribe({
      next: (data: any) => {
        this.requests = data.requests || [];
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
      payload.accessExpiresAt = new Date(this.approveUntilDate).toISOString();
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
}
