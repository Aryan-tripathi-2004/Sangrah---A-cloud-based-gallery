import { Component, ChangeDetectionStrategy, signal, computed, inject, DestroyRef, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { InvoiceDTO } from '../../billing.service';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';
import { BillingService } from '../../billing.service';

@Component({
  selector: 'app-invoice-detail-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, StatusBadgeComponent],
  templateUrl: './invoice-detail-modal.component.html'
})
export class InvoiceDetailModalComponent {
  private readonly billingService = inject(BillingService);
  private readonly destroyRef = inject(DestroyRef);

  readonly invoiceInput = input.required<InvoiceDTO>({ alias: 'invoice' });
  readonly closeModal = output<void>();
  readonly payInvoice = output<string>();

  get invoice(): InvoiceDTO {
    return this.invoiceInput();
  }

  readonly isDownloadingPDF = signal<boolean>(false);

  readonly totalStorageCost = computed(() => {
    const inv = this.invoiceInput();
    return (inv.storageMetrics?.imageCost ?? 0) + (inv.storageMetrics?.videoCost ?? 0);
  });

  downloadPDF(): void {
    const currentInvoice = this.invoiceInput();
    if (!currentInvoice?.id) {
      alert('Error: Invoice ID not found');
      return;
    }

    this.isDownloadingPDF.set(true);
    console.log('📥 Starting PDF download for invoice:', currentInvoice.invoiceId);

    this.billingService.downloadInvoicePDF(currentInvoice.invoiceId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `${currentInvoice.invoiceId}.pdf`;
          document.body.appendChild(a);
          a.click();
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
          console.log('✅ PDF downloaded successfully:', currentInvoice.invoiceId);
          this.isDownloadingPDF.set(false);
        },
        error: (error) => {
          console.error('❌ PDF download failed:', error);
          alert('Failed to download PDF. Please try again.');
          this.isDownloadingPDF.set(false);
        }
      });
  }

  onPayClick(): void {
    this.payInvoice.emit(this.invoiceInput().invoiceId);
  }

  getTotalImageCost(): number {
    return this.totalStorageCost();
  }
}
