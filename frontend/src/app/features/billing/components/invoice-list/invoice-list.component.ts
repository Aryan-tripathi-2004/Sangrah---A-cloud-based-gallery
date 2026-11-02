import {
  Component,
  output,
  ChangeDetectionStrategy,
  inject,
  DestroyRef,
  signal,
  computed,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { BillingService, InvoiceDTO } from '../../billing.service';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';

@Component({
  selector: 'app-invoice-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, StatusBadgeComponent],
  templateUrl: './invoice-list.component.html'
})
export class InvoiceListComponent {
  private readonly billingService = inject(BillingService);
  private readonly destroyRef = inject(DestroyRef);

  readonly invoiceSelected = output<InvoiceDTO>();
  readonly payInvoice = output<string>();

  readonly totalPages = this.billingService.totalPages;

  readonly selectedStatusSignal = signal<string | null>(null);
  readonly currentPageSignal = signal<number>(0);
  readonly pageSize = 10;

  readonly statuses = ['All', 'PAID', 'PENDING', 'OVERDUE'];

  readonly invoices = computed<InvoiceDTO[]>(() => {
    const list = this.billingService.invoices();
    const status = this.selectedStatusSignal();
    if (!status) return list;
    return list.filter((inv) => inv.status === status);
  });

  get selectedStatus(): string | null {
    return this.selectedStatusSignal();
  }
  set selectedStatus(val: string | null) {
    this.selectedStatusSignal.set(val);
  }

  get currentPage(): number {
    return this.currentPageSignal();
  }
  set currentPage(val: number) {
    this.currentPageSignal.set(val);
  }

  filterBy(status: string): void {
    this.selectedStatusSignal.set(status === 'All' ? null : status);
    this.currentPageSignal.set(0);
  }

  viewDetails(invoice: InvoiceDTO): void {
    this.invoiceSelected.emit(invoice);
  }

  downloadPDF(invoiceId: string): void {
    console.log('📥 Starting PDF download for invoice:', invoiceId);
    this.billingService.downloadInvoicePDF(invoiceId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `${invoiceId}.pdf`;
          document.body.appendChild(a);
          a.click();
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
          console.log('✅ PDF downloaded successfully:', invoiceId);
        },
        error: (error) => {
          console.error('❌ PDF download failed:', error);
          alert('Failed to download PDF. Please try again.');
        }
      });
  }

  previousPage(): void {
    const page = this.currentPageSignal();
    if (page > 0) {
      this.currentPageSignal.set(page - 1);
      this.billingService.loadInvoices(page - 1, this.pageSize)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe();
    }
  }

  nextPage(): void {
    const page = this.currentPageSignal();
    if (page < this.totalPages() - 1) {
      this.currentPageSignal.set(page + 1);
      this.billingService.loadInvoices(page + 1, this.pageSize)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe();
    }
  }

  onPayClick(invoiceId: string): void {
    this.payInvoice.emit(invoiceId);
  }
}
