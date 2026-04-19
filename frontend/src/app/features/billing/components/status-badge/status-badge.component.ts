import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './status-badge.component.html'
})
export class StatusBadgeComponent {
  @Input() status: 'PENDING' | 'PAID' | 'OVERDUE' = 'PENDING';

  getStatusClasses(): string {
    const baseClasses = 'px-3 py-1 rounded-full text-xs font-semibold inline-block';
    switch (this.status) {
      case 'PAID':
        return baseClasses + ' bg-green-500/20 text-green-400 border border-green-500/30';
      case 'PENDING':
        return baseClasses + ' bg-yellow-500/20 text-yellow-400 border border-yellow-500/30';
      case 'OVERDUE':
        return baseClasses + ' bg-red-500/20 text-red-400 border border-red-500/30';
      default:
        return baseClasses;
    }
  }
}
