import { Component, EventEmitter, Input, Output, ViewEncapsulation } from '@angular/core';
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

  @Input() ticketNo = '';
  @Output() ticketNoChange = new EventEmitter<string>();
  @Input() reason = '';
  @Output() reasonChange = new EventEmitter<string>();

  @Output() apply = new EventEmitter<void>();

  onApply(): void {
    const ticket = this.ticketNo.trim();
    if (!ticket) {
      this.errorMessage = 'Sila masukkan No. Tiket.';
      return;
    }
    if (!window.confirm(`${this.confirmText}\n\nNo. Tiket: ${ticket}`)) {
      return;
    }
    this.apply.emit();
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
