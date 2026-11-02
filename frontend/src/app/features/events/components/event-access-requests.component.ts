import { Component, OnInit, inject, ChangeDetectionStrategy, DestroyRef, input, signal, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EventApiService } from '../../../core/api/services/event-api.service';
import { AccessRequestResponse, AccessRequestsResponse } from '../../../core/api/models/api.models';

@Component({
  selector: 'app-event-access-requests',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  templateUrl: './event-access-requests.component.html',
  styles: []
})
export class EventAccessRequestsComponent implements OnInit {
  readonly eventId = input.required<string>();

  private readonly api = inject(EventApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly requests = signal<AccessRequestResponse[]>([]);

  readonly filterStatusSignal = signal<'PENDING' | 'APPROVED'>('PENDING');
  get filterStatus(): 'PENDING' | 'APPROVED' { return this.filterStatusSignal(); }
  set filterStatus(val: 'PENDING' | 'APPROVED') { this.filterStatusSignal.set(val); }

  readonly processingIdSignal = signal<string | null>(null);
  get processingId(): string | null { return this.processingIdSignal(); }
  set processingId(val: string | null) { this.processingIdSignal.set(val); }

  readonly approveFormIdSignal = signal<string | null>(null);
  get approveFormId(): string | null { return this.approveFormIdSignal(); }
  set approveFormId(val: string | null) { this.approveFormIdSignal.set(val); }

  readonly approveDurationSignal = signal<'FOREVER' | 'UNTIL_DATE'>('FOREVER');
  get approveDuration(): 'FOREVER' | 'UNTIL_DATE' { return this.approveDurationSignal(); }
  set approveDuration(val: 'FOREVER' | 'UNTIL_DATE') { this.approveDurationSignal.set(val); }

  readonly approveUntilDateSignal = signal<string>('');
  get approveUntilDate(): string { return this.approveUntilDateSignal(); }
  set approveUntilDate(val: string) { this.approveUntilDateSignal.set(val); }

  readonly minDateSignal = signal<string>('');
  get minDate(): string { return this.minDateSignal(); }
  set minDate(val: string) { this.minDateSignal.set(val); }

  readonly rejectFormIdSignal = signal<string | null>(null);
  get rejectFormId(): string | null { return this.rejectFormIdSignal(); }
  set rejectFormId(val: string | null) { this.rejectFormIdSignal.set(val); }

  readonly rejectionReasonSignal = signal<string>('');
  get rejectionReason(): string { return this.rejectionReasonSignal(); }
  set rejectionReason(val: string) { this.rejectionReasonSignal.set(val); }

  readonly filteredRequests = computed(() =>
    this.requests().filter(r => r.status === this.filterStatusSignal())
  );

  readonly pendingCount = computed(() =>
    this.requests().filter(r => r.status === 'PENDING').length
  );

  readonly approvedCount = computed(() =>
    this.requests().filter(r => r.status === 'APPROVED').length
  );

  ngOnInit() {
    const d = new Date();
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    this.minDate = `${yyyy}-${mm}-${dd}`;
    this.loadRequests();
  }

  loadRequests() {
    const id = this.eventId();
    if (!id) return;
    this.api.listAccessRequests(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data: unknown) => {
        const obj = data as AccessRequestsResponse | null;
        const rawList: AccessRequestResponse[] = Array.isArray(data)
          ? (data as AccessRequestResponse[])
          : (obj?.requests || []);
        this.requests.set(rawList.map((r: AccessRequestResponse) => ({
          ...r,
          status: (r.status || '').toString().toUpperCase() as AccessRequestResponse['status']
        })));
      },
      error: (err) => {
        console.error('❌ Failed to load requests:', err);
      }
    });
  }

  getFilteredRequests(): AccessRequestResponse[] {
    return this.filteredRequests();
  }

  getPendingCount(): number {
    return this.pendingCount();
  }

  getApprovedCount(): number {
    return this.approvedCount();
  }

  openApproveForm(requestId: string) {
    this.approveFormId = requestId;
    this.approveDuration = 'FOREVER';
    this.approveUntilDate = '';
  }

  confirmApprove(requestId: string) {
    this.processingId = requestId;
    const payload: { approvalDuration: string; accessExpiresAt?: string } = {
      approvalDuration: this.approveDuration
    };
    if (this.approveDuration === 'UNTIL_DATE' && this.approveUntilDate) {
      const selected = new Date(this.approveUntilDate);
      const today = new Date();
      const selStart = new Date(selected.getFullYear(), selected.getMonth(), selected.getDate());
      const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate());
      if (selStart < todayStart) {
        alert('Expiry date cannot be in the past. Please select today or a future date.');
        this.processingId = null;
        return;
      }
      const endOfDay = new Date(selected.getFullYear(), selected.getMonth(), selected.getDate(), 23, 59, 59, 999);
      payload.accessExpiresAt = endOfDay.toISOString();
    }

    this.api.updateAccessRequestStatus(this.eventId(), requestId, 'APPROVED', payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.approveFormId = null;
          this.processingId = null;
          this.loadRequests();
        },
        error: (err) => {
          console.error('Failed to approve request:', err);
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

    this.api.updateAccessRequestStatus(this.eventId(), requestId, 'REJECTED', payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.rejectFormId = null;
          this.processingId = null;
          this.loadRequests();
        },
        error: (err) => {
          console.error('Failed to reject request:', err);
          this.processingId = null;
        }
      });
  }

  openRevoke(requestId: string) {
    this.processingId = requestId;
    this.api.updateAccessRequestStatus(this.eventId(), requestId, 'REVOKED')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.processingId = null;
          this.loadRequests();
        },
        error: (err) => {
          console.error('Failed to revoke request:', err);
          this.processingId = null;
        }
      });
  }
}
