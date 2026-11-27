import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HisFixApiService, HisFixAuditEntry, HisFixResult } from '../his-fix-api.service';
import { HisFixShellComponent } from '../shared/his-fix-shell.component';

interface ScheduleRow {
  scheduleCode: string;
  scheduleName: string | null;
  hidden: boolean;
  reservationDates: string | null;
  patientMrn: string | null;
}

interface OpenedSchedule {
  scheduleCode: string;
  scheduleName: string | null;
  openedBy: string;
  openedAt: string;
}

// The toggle the admin picked on one row; applied through the shell's ticket/reason form.
interface Pending {
  row: ScheduleRow;
  newHidden: boolean;
}

const SLUG = 'schedule-visibility';

@Component({
  selector: 'app-schedule-visibility',
  standalone: true,
  imports: [CommonModule, FormsModule, HisFixShellComponent],
  templateUrl: './schedule-visibility.component.html'
})
export class ScheduleVisibilityComponent implements OnInit {
  readonly rows = signal<ScheduleRow[]>([]);
  readonly opened = signal<OpenedSchedule[]>([]);
  readonly pending = signal<Pending | null>(null);
  readonly auditEntries = signal<HisFixAuditEntry[]>([]);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly isSearching = signal(false);
  readonly isSaving = signal(false);
  readonly hasSearched = signal(false);
  readonly dryRun = signal(false);

  mode: 'MRN' | 'NAME' = 'MRN';
  mrn = '';
  name = '';
  ticketNo = '';
  reason = '';

  constructor(private readonly api: HisFixApiService) {}

  ngOnInit(): void {
    this.loadOpened();
  }

  setMode(mode: 'MRN' | 'NAME'): void {
    if (this.mode === mode) {
      return;
    }
    this.mode = mode;
    this.resetResults();
  }

  onInputChange(): void {
    this.resetResults();
  }

  search(): void {
    const value = (this.mode === 'MRN' ? this.mrn : this.name).trim();
    if (!value) {
      this.errorMessage.set(this.mode === 'MRN' ? 'Sila masukkan MRN pesakit.' : 'Sila masukkan nama jadual.');
      return;
    }
    this.isSearching.set(true);
    this.resetResults();
    const params: Record<string, string> = this.mode === 'MRN' ? { mrn: value } : { name: value };
    this.api.search<ScheduleRow[]>(SLUG, params).subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.hasSearched.set(true);
        this.isSearching.set(false);
        if (this.mode === 'MRN') {
          this.loadAudit(`audit?mrn=${encodeURIComponent(value)}`);
        }
      },
      error: (error) => {
        this.hasSearched.set(true);
        this.isSearching.set(false);
        this.errorMessage.set(error?.error?.message ?? 'Carian gagal.');
      }
    });
  }

  choose(row: ScheduleRow): void {
    // Direction-labelled: a hidden schedule is shown (newHidden = false) and vice versa.
    this.pending.set({ row, newHidden: !row.hidden });
    this.dryRun.set(false);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  isPending(row: ScheduleRow): boolean {
    return this.pending()?.row.scheduleCode === row.scheduleCode;
  }

  canApply(): boolean {
    return this.pending() !== null || this.opened().length > 0;
  }

  confirmText(): string {
    const p = this.pending();
    if (!p) {
      return 'Pilih jadual pada senarai untuk dipaparkan/disembunyikan, atau tekan "Sembunyikan semula semua".';
    }
    return `${p.newHidden ? 'Sembunyikan' : 'Paparkan'} jadual ${p.row.scheduleName || ''} (${p.row.scheduleCode}).`;
  }

  apply(): void {
    const p = this.pending();
    if (!p) {
      this.errorMessage.set('Pilih jadual pada senarai dahulu.');
      return;
    }
    this.isSaving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.api.apply<ScheduleRow>(SLUG, {
      scheduleCode: p.row.scheduleCode,
      expectedHidden: p.row.hidden ? 1 : 0,
      newHidden: p.newHidden ? 1 : 0,
      mrn: p.row.patientMrn,
      ticketNo: this.ticketNo.trim(),
      reason: this.reason
    }).subscribe({
      next: (result) => {
        this.isSaving.set(false);
        this.dryRun.set(result.dryRun);
        this.ticketNo = '';
        this.reason = '';
        this.pending.set(null);
        if (result.dryRun) {
          this.successMessage.set(`DRY RUN: jadual ${p.row.scheduleCode} akan ${p.newHidden ? 'disembunyikan' : 'dipaparkan'} (tidak disimpan).`);
          return;
        }
        // Replace by scheduleCode, which is unique per displayed row; keep the searched MRN and dates.
        this.rows.set(this.rows().map((r) =>
          r.scheduleCode === result.data.scheduleCode
            ? { ...r, hidden: result.data.hidden, scheduleName: result.data.scheduleName ?? r.scheduleName }
            : r));
        this.successMessage.set(`Jadual ${p.row.scheduleCode} berjaya ${p.newHidden ? 'disembunyikan' : 'dipaparkan'}.`);
        this.loadOpened();
        this.loadAudit(p.row.patientMrn
          ? `audit?mrn=${encodeURIComponent(p.row.patientMrn)}`
          : `audit?scheduleCode=${encodeURIComponent(p.row.scheduleCode)}`);
      },
      error: (error) => {
        this.isSaving.set(false);
        this.errorMessage.set(error?.error?.message ?? 'Pembetulan gagal.');
      }
    });
  }

  rehideAll(): void {
    const ticket = this.ticketNo.trim();
    if (!ticket) {
      this.errorMessage.set('Sila masukkan No. Tiket.');
      return;
    }
    const count = this.opened().length;
    if (!window.confirm(`Sembunyikan semula ${count} jadual yang dibuka hari ini?\n\nNo. Tiket: ${ticket}`)) {
      return;
    }
    this.isSaving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.api.rehide<OpenedSchedule[]>(SLUG, { ticketNo: ticket, reason: this.reason }).subscribe({
      next: (result: HisFixResult<OpenedSchedule[]>) => {
        this.isSaving.set(false);
        this.dryRun.set(result.dryRun);
        this.ticketNo = '';
        this.reason = '';
        this.pending.set(null);
        if (result.dryRun) {
          // Nothing persisted: leave the panel and rows as they are.
          this.successMessage.set(`DRY RUN: ${result.data.length} jadual akan disembunyikan semula (tidak disimpan).`);
          return;
        }
        const hiddenCodes = result.data.map((o) => o.scheduleCode);
        this.rows.set(this.rows().map((r) => (hiddenCodes.includes(r.scheduleCode) ? { ...r, hidden: true } : r)));
        this.successMessage.set(`${result.data.length} jadual berjaya disembunyikan semula.`);
        this.loadOpened();
      },
      error: (error) => {
        this.isSaving.set(false);
        this.errorMessage.set(error?.error?.message ?? 'Sembunyi semula gagal.');
      }
    });
  }

  private resetResults(): void {
    this.rows.set([]);
    this.pending.set(null);
    this.auditEntries.set([]);
    this.hasSearched.set(false);
    this.dryRun.set(false);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.ticketNo = '';
    this.reason = '';
  }

  private loadOpened(): void {
    this.api.lookup<OpenedSchedule[]>(SLUG, 'opened-today').subscribe({
      next: (list) => this.opened.set(list),
      error: () => this.opened.set([])
    });
  }

  private loadAudit(path: string): void {
    this.api.lookup<HisFixAuditEntry[]>(SLUG, path).subscribe({
      next: (entries) => this.auditEntries.set(entries),
      error: () => this.auditEntries.set([])
    });
  }
}
