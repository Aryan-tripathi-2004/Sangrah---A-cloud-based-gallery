import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BillingService, PaymentDTO } from '../../billing.service';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-payment-history',
  standalone: true,
  imports: [CommonModule, StatusBadgeComponent],
  templateUrl: './payment-history.component.html'
})
export class PaymentHistoryComponent implements OnInit {
  payments$!: Observable<PaymentDTO[]>;

  constructor(private billingService: BillingService) {}

  ngOnInit(): void {
    this.payments$ = this.billingService.payments$;
  }

  retryPayment(paymentId: string): void {
    alert('🔄 Payment retry coming in Phase 2');
  }

  getStatusLabel(status: string): 'PAID' | 'PENDING' | 'OVERDUE' {
    if (status === 'SUCCESS') return 'PAID';
    if (status === 'FAILED') return 'OVERDUE';
    return 'PENDING';
  }
}
