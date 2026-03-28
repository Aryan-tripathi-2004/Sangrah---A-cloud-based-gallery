import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InvoiceDTO } from '../../billing.service';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';
import { BillingService } from '../../billing.service';

@Component({
  selector: 'app-invoice-detail-modal',
  standalone: true,
  imports: [CommonModule, StatusBadgeComponent],
  templateUrl: './invoice-detail-modal.component.html'
})
export class InvoiceDetailModalComponent {
  @Input() invoice!: InvoiceDTO;
  @Output() closeModal = new EventEmitter<void>();
  @Output() payInvoice = new EventEmitter<string>();

  isDownloadingPDF = false;

  constructor(private billingService: BillingService) {}

  downloadPDF(): void {
    if (!this.invoice?.id) {
      alert('Error: Invoice ID not found');
      return;
    }

    this.isDownloadingPDF = true;
    console.log('📥 Starting PDF download for invoice:', this.invoice.invoiceId);

    this.billingService.downloadInvoicePDF(this.invoice.invoiceId).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${this.invoice.invoiceId}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        console.log('✅ PDF downloaded successfully:', this.invoice.invoiceId);
        this.isDownloadingPDF = false;
      },
      error: (error) => {
        console.error('❌ PDF download failed:', error);
        alert('Failed to download PDF. Please try again.');
        this.isDownloadingPDF = false;
      }
    });
  }

  onPayClick(): void {
    this.payInvoice.emit(this.invoice.invoiceId);
  }

  getTotalImageCost(): number {
    return this.invoice.storageMetrics.imageCost + this.invoice.storageMetrics.videoCost;
  }
}
