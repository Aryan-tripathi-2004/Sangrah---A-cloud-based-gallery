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
  template: `
    <div class="space-y-6">
      <h2 class="text-2xl font-bold">Access Requests</h2>

      <!-- Request Tabs -->
      <div class="flex gap-3">
        <button
          (click)="filterStatus = 'PENDING'"
          [class.bg-blue-600]="filterStatus === 'PENDING'"
          [class.bg-slate-700]="filterStatus !== 'PENDING'"
          class="px-4 py-2 rounded-lg font-medium transition">
          ⏳ Pending ({{ getPendingCount() }})
        </button>
        <button
          (click)="filterStatus = 'APPROVED'"
          [class.bg-blue-600]="filterStatus === 'APPROVED'"
          [class.bg-slate-700]="filterStatus !== 'APPROVED'"
          class="px-4 py-2 rounded-lg font-medium transition">
          ✅ Approved ({{ getApprovedCount() }})
        </button>
      </div>

      <!-- Request List -->
      <div *ngIf="getFilteredRequests().length > 0" class="space-y-3">
        <div *ngFor="let req of getFilteredRequests()" class="bg-slate-800/30 border border-slate-700/50 rounded-lg p-4">
          <div class="flex items-start justify-between mb-3">
            <div>
              <p class="font-semibold">{{ req.displayName || req.requesterUserId }}</p>
              <p class="text-sm text-slate-400">Requested: {{ req.requestedAt | date:'short' }}</p>
            </div>
            <span
              [ngClass]="req.status === 'PENDING' ? 'bg-yellow-500/20 text-yellow-400' : 'bg-green-500/20 text-green-400'"
              class="px-3 py-1 rounded text-xs font-semibold">
              {{ req.status }}
            </span>
          </div>

          <p *ngIf="req.message" class="text-slate-300 text-sm mb-4 italic border-l-2 border-slate-600 pl-3">
            "{{ req.message }}"
          </p>

          <div *ngIf="req.status === 'PENDING'" class="flex gap-3">
            <button
              (click)="openApproveForm(req.requestId)"
              [disabled]="processingId === req.requestId"
              class="flex-1 bg-green-600 hover:bg-green-700 disabled:bg-slate-600 px-4 py-2 rounded font-semibold text-sm transition">
              {{ processingId === req.requestId ? '⏳' : '✅ Approve' }}
            </button>
            <button
              (click)="openRejectForm(req.requestId)"
              [disabled]="processingId === req.requestId"
              class="flex-1 bg-red-600 hover:bg-red-700 disabled:bg-slate-600 px-4 py-2 rounded font-semibold text-sm transition">
              {{ processingId === req.requestId ? '⏳' : '❌ Reject' }}
            </button>
          </div>

          <!-- Approve Form -->
          <div *ngIf="approveFormId === req.requestId" class="mt-4 p-3 bg-slate-800 rounded border border-green-500/30">
            <p class="text-sm mb-3">Choose access duration:</p>
            <div class="space-y-2 mb-3">
              <label class="flex items-center gap-2">
                <input type="radio" [(ngModel)]="approveDuration" value="FOREVER" class="w-4 h-4">
                <span>Forever (no expiration)</span>
              </label>
              <label class="flex items-center gap-2">
                <input type="radio" [(ngModel)]="approveDuration" value="UNTIL_DATE" class="w-4 h-4">
                <span>Until a specific date:</span>
              </label>
              <input
                *ngIf="approveDuration === 'UNTIL_DATE'"
                type="date"
                [(ngModel)]="approveUntilDate"
                class="w-full bg-slate-700 rounded p-2 text-white mt-2 border border-slate-600">
            </div>
            <div class="flex gap-2">
              <button
                (click)="confirmApprove(req.requestId)"
                class="flex-1 bg-green-600 hover:bg-green-700 py-2 rounded text-sm font-semibold transition">
                Confirm Approval
              </button>
              <button
                (click)="approveFormId = null"
                class="px-4 py-2 bg-slate-700 hover:bg-slate-600 rounded text-sm transition">
                Cancel
              </button>
            </div>
          </div>

          <!-- Reject Form -->
          <div *ngIf="rejectFormId === req.requestId" class="mt-4 p-3 bg-slate-800 rounded border border-red-500/30">
            <textarea
              [(ngModel)]="rejectionReason"
              placeholder="Reason for rejection (optional)"
              class="w-full bg-slate-700 rounded p-2 text-white text-sm mb-2 border border-slate-600">
            </textarea>
            <div class="flex gap-2">
              <button
                (click)="confirmReject(req.requestId)"
                class="flex-1 bg-red-600 hover:bg-red-700 py-2 rounded text-sm font-semibold transition">
                Confirm Reject
              </button>
              <button
                (click)="rejectFormId = null"
                class="px-4 py-2 bg-slate-700 hover:bg-slate-600 rounded text-sm transition">
                Cancel
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div *ngIf="getFilteredRequests().length === 0" class="text-center py-8">
        <p class="text-slate-400">No {{ filterStatus }} requests</p>
      </div>
    </div>
  `,
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
