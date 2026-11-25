import { Component, EventEmitter, Input, Output, ViewEncapsulation, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HisFixAuditEntry } from '../his-fix-api.service';

// Generic frame for every "Pembetulan HIS" page: header, banners, ticket/reason form, confirm step,
// audit table. The page supplies the search box, result table and change summary through slots.
@Component({
  selector: 'app-his-fix-shell',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './his-fix-shell.component.html',
  styleUrl: './his-fix-shell.component.css',
  encapsulation: ViewEncapsulation.None
})
export class HisFixShellComponent {
  @Input() title = '';
  @Input() description = '';
  @Input() icon = 'build';
  @Input() busy = false;
  @Input() errorMessage = '';
  @Input() successMessage = '';
  @Input() dryRun = false;
  @Input() auditEntries: HisFixAuditEntry[] = [];
  @Input() canApply = false;
  // Text shown in the window.confirm step, listing the change about to be made.
  @Input() confirmText = 'Teruskan perubahan ini?';

  // Shell-local validation message (not an @Input, so the parent's bound value can't leave it stale).
  readonly localError = signal('');

  @Input() ticketNo = '';
  @Output() ticketNoChange = new EventEmitter<string>();
  @Input() reason = '';
  @Output() reasonChange = new EventEmitter<string>();

  @Output() apply = new EventEmitter<void>();

  onApply(): void {
    const ticket = this.ticketNo.trim();
    if (!ticket) {
      this.localError.set('Sila masukkan No. Tiket.');
      return;
    }
    this.localError.set('');
    if (!window.confirm(`${this.confirmText}\n\nNo. Tiket: ${ticket}`)) {
      return;
    }
    this.apply.emit();
  }

  onTicketChange(value: string): void {
    this.localError.set('');
    this.ticketNoChange.emit(value);
  }

  fmt(value: string | null): string {
    if (!value) {
      return '-';
    }
    const match = /^(\d{4})-(\d{2})-(\d{2})(.*)$/.exec(value);
    return match ? `${match[3]}-${match[2]}-${match[1]}${match[4]}` : value;
  }

  // {"a":1,"b":"x"} -> 'a: 1, b: x'; falls back to the raw text when not JSON.
  summarize(json: string | null): string {
    if (!json) {
      return '-';
    }
    try {
      const parsed = JSON.parse(json);
      if (parsed && typeof parsed === 'object') {
        return Object.entries(parsed).map(([key, value]) => `${key}: ${value === null ? '-' : value}`).join(', ');
      }
      return String(parsed);
    } catch {
      return json;
    }
  }
}
