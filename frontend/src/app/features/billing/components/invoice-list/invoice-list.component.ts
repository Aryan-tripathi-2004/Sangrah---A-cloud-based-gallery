import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BillingService, InvoiceDTO } from '../../billing.service';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-invoice-list',
  standalone: true,
  imports: [CommonModule, StatusBadgeComponent],
  templateUrl: './invoice-list.component.html'
})
export class InvoiceListComponent implements OnInit {
  @Output() invoiceSelected = new EventEmitter<InvoiceDTO>();
  @Output() payInvoice = new EventEmitter<string>();

  invoices$!: Observable<InvoiceDTO[]>;
  totalPages$!: Observable<number>;
  totalPages = 1;

  currentPage = 0;
  pageSize = 10;
  selectedStatus: string | null = null;

  statuses = ['All', 'PAID', 'PENDING', 'OVERDUE'];

  constructor(private billingService: BillingService) {}

  ngOnInit(): void {
    this.invoices$ = this.billingService.invoices$;
    this.totalPages$ = this.billingService.totalPages$;
    // Subscribe to get the actual value for pagination logic
    this.totalPages$.subscribe(pages => {
      this.totalPages = pages;
    });
  }

  filterBy(status: string): void {
    this.selectedStatus = status === 'All' ? null : status;
    this.currentPage = 0;
    // TODO: Apply filter when API endpoint supports it
  }

  viewDetails(invoice: InvoiceDTO): void {
    this.invoiceSelected.emit(invoice);
  }

  downloadPDF(invoiceId: string): void {
    // TODO: Implement PDF download
    alert('📥 PDF download coming in Phase 2');
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.billingService.loadInvoices(this.currentPage, this.pageSize).subscribe();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.billingService.loadInvoices(this.currentPage, this.pageSize).subscribe();
    }
  }

  onPayClick(invoiceId: string): void {
    this.payInvoice.emit(invoiceId);
  }
}
