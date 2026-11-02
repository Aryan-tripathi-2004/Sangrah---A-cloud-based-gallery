import { Component, ChangeDetectionStrategy, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

const BASE_BADGE_CLASSES = 'px-3 py-1 rounded-full text-xs font-semibold inline-block';

export const STATUS_BADGE_STYLES: Record<string, string> = {
  PAID: `${BASE_BADGE_CLASSES} bg-green-500/20 text-green-400 border border-green-500/30`,
  SUCCESS: `${BASE_BADGE_CLASSES} bg-green-500/20 text-green-400 border border-green-500/30`,
  PENDING: `${BASE_BADGE_CLASSES} bg-yellow-500/20 text-yellow-400 border border-yellow-500/30`,
  OVERDUE: `${BASE_BADGE_CLASSES} bg-red-500/20 text-red-400 border border-red-500/30`,
  FAILED: `${BASE_BADGE_CLASSES} bg-red-500/20 text-red-400 border border-red-500/30`,
  REFUNDED: `${BASE_BADGE_CLASSES} bg-purple-500/20 text-purple-400 border border-purple-500/30`,
  CANCELLED: `${BASE_BADGE_CLASSES} bg-slate-500/20 text-slate-400 border border-slate-500/30`,
};

const DEFAULT_BADGE_CLASS = `${BASE_BADGE_CLASSES} bg-slate-500/20 text-slate-400 border border-slate-500/30`;

@Component({
  selector: 'app-status-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  templateUrl: './status-badge.component.html'
})
export class StatusBadgeComponent {
  readonly statusInput = input<string>('PENDING', { alias: 'status' });

  readonly statusClasses = computed(() => {
    const key = (this.statusInput() || '').toUpperCase();
    return STATUS_BADGE_STYLES[key] ?? DEFAULT_BADGE_CLASS;
  });

  get status(): string {
    return this.statusInput();
  }

  getStatusClasses(): string {
    return this.statusClasses();
  }
}
