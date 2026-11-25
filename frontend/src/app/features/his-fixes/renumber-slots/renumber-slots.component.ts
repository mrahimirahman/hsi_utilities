import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HisFixApiService, HisFixAuditEntry } from '../his-fix-api.service';
import { HisFixShellComponent } from '../shared/his-fix-shell.component';

interface SlotRow {
  reserveId: number;
  reserveIndex: number | null;
  cancelled: boolean;
  patientMrn: string | null;
  personName: string | null;
  scheduleCode: string | null;
}

interface SlotChange {
  reserveId: number;
  fromIndex: number | null;
  toIndex: number;
}

interface SlotSearchResult {
  rows: SlotRow[];
  changes: SlotChange[];
}

interface Option {
  code: string;
  name: string | null;
  hidden: boolean;
}

const SLUG = 'renumber-slots';

@Component({
  selector: 'app-renumber-slots',
  standalone: true,
  imports: [CommonModule, FormsModule, HisFixShellComponent],
  templateUrl: './renumber-slots.component.html'
})
export class RenumberSlotsComponent implements OnInit {
  readonly schedules = signal<Option[]>([]);
  readonly resources = signal<Option[]>([]);
  readonly rows = signal<SlotRow[]>([]);
  readonly changes = signal<SlotChange[]>([]);
  readonly auditEntries = signal<HisFixAuditEntry[]>([]);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly isSearching = signal(false);
  readonly isSaving = signal(false);
  readonly hasSearched = signal(false);
  readonly dryRun = signal(false);

  scheduleCode = '';
  resourceCode = '';
  date = '';
  ticketNo = '';
  reason = '';
  // Selection the displayed rows/plan belong to; apply always uses this, not the live form fields.
  private searched: { resourceCode: string; date: string; scheduleCode: string } | null = null;

  constructor(private readonly api: HisFixApiService) {}

  ngOnInit(): void {
    this.api.lookup<Option[]>(SLUG, 'schedules').subscribe({
      next: (list) => this.schedules.set(list),
      error: (error) => this.errorMessage.set(error?.error?.message ?? 'Senarai jadual gagal dimuatkan.')
    });
  }

  onScheduleChange(code: string): void {
    this.scheduleCode = code;
    this.resourceCode = '';
    this.resources.set([]);
    this.resetResults();
    if (!code) {
      return;
    }
    this.api.lookup<Option[]>(SLUG, `resources?scheduleCode=${encodeURIComponent(code)}`).subscribe({
      next: (list) => this.resources.set(list),
      error: (error) => this.errorMessage.set(error?.error?.message ?? 'Senarai sumber gagal dimuatkan.')
    });
  }

  onSelectionChange(): void {
    this.resetResults();
  }

  search(): void {
    if (!this.scheduleCode || !this.resourceCode || !this.date) {
      this.errorMessage.set('Sila pilih jadual, sumber dan tarikh.');
      return;
    }
    const selection = { resourceCode: this.resourceCode, date: this.date, scheduleCode: this.scheduleCode };
    this.isSearching.set(true);
    this.resetResults();
    this.api.search<SlotSearchResult>(SLUG, selection).subscribe({
      next: (result) => {
        this.searched = selection;
        this.rows.set(result.rows);
        this.changes.set(result.changes);
        this.hasSearched.set(true);
        this.isSearching.set(false);
        this.loadAudit();
      },
      error: (error) => {
        this.hasSearched.set(true);
        this.isSearching.set(false);
        this.errorMessage.set(error?.error?.message ?? 'Carian gagal.');
      }
    });
  }

  changeFor(row: SlotRow): SlotChange | null {
    return this.changes().find((change) => change.reserveId === row.reserveId) ?? null;
  }

  canApply(): boolean {
    return this.searched !== null && this.changes().length > 0;
  }

  confirmText(): string {
    const s = this.searched;
    return s
      ? `Susun semula ${this.changes().length} slot (sumber ${s.resourceCode}, ${s.date}, jadual ${s.scheduleCode}).`
      : '';
  }

  apply(): void {
    const s = this.searched;
    if (!s || this.changes().length === 0) {
      return;
    }
    this.isSaving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    const count = this.changes().length;
    this.api.apply<SlotSearchResult>(SLUG, {
      resourceCode: s.resourceCode,
      date: s.date,
      scheduleCode: s.scheduleCode,
      expectedChanges: this.changes(),
      ticketNo: this.ticketNo.trim(),
      reason: this.reason
    }).subscribe({
      next: (result) => {
        this.isSaving.set(false);
        this.dryRun.set(result.dryRun);
        this.ticketNo = '';
        this.reason = '';
        if (result.dryRun) {
          this.successMessage.set(`DRY RUN: ${count} slot akan disusun semula (tidak disimpan).`);
        } else {
          this.rows.set(result.data.rows);
          this.changes.set(result.data.changes);
          this.successMessage.set(`${count} slot berjaya disusun semula.`);
          this.loadAudit();
        }
      },
      error: (error) => {
        this.isSaving.set(false);
        this.errorMessage.set(error?.error?.message ?? 'Pembetulan gagal.');
      }
    });
  }

  private resetResults(): void {
    this.rows.set([]);
    this.changes.set([]);
    this.auditEntries.set([]);
    this.searched = null;
    this.hasSearched.set(false);
    this.dryRun.set(false);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.ticketNo = '';
    this.reason = '';
  }

  private loadAudit(): void {
    if (!this.searched) {
      return;
    }
    this.api.lookup<HisFixAuditEntry[]>(SLUG, `audit?resource=${encodeURIComponent(this.searched.resourceCode)}`).subscribe({
      next: (entries) => this.auditEntries.set(entries),
      error: () => this.auditEntries.set([])
    });
  }
}
