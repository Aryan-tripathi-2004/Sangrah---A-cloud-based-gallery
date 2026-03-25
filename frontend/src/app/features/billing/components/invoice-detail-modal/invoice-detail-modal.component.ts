import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InvoiceDTO } from '../../billing.service';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';

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

  downloadPDF(): void {
    alert('📥 PDF download coming in Phase 2');
  }

  onPayClick(): void {
    this.payInvoice.emit(this.invoice.invoiceId);
  }

  getTotalImageCost(): number {
    return this.invoice.storageMetrics.imageCost + this.invoice.storageMetrics.videoCost;
  }
}
