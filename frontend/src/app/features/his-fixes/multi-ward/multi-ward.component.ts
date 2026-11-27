import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HisFixApiService, HisFixAuditEntry } from '../his-fix-api.service';
import { HisFixShellComponent } from '../shared/his-fix-shell.component';

interface WardAdmissionRow {
  admissionId: number;
  wardCode: string | null;
  wardName: string | null;
  bedCode: string | null;
  admissionArrival: string | null;
  admissionDepart: string | null;
  admissionActive: number;
  admissionConfirmed: number;
}

interface WardTransferRow {
  transferId: number;
  admissionSource: string | null;
  admissionDestination: string | null;
  wardSource: string | null;
  wardSourceName: string | null;
  wardDestination: string | null;
  wardDestinationName: string | null;
  transferTimestamp: string | null;
  transferConfirmed: number;
}

interface EncounterOption {
  encounterId: number;
  encounterTimestamp: string | null;
  dischargeDate: string | null;
  encounterActive: number;
}

interface MultiWardResult {
  encounterId: number;
  patientMrn: string;
  encounterTimestamp: string | null;
  admissions: WardAdmissionRow[];
  transfers: WardTransferRow[];
  recentEncounters: EncounterOption[];
}

type WardTable = 'enc_admissions' | 'enc_transfers';

// One ticked row: the flags as shown (expected) and the flags the admin picked (values).
interface RowEdit {
  table: WardTable;
  id: number;
  label: string;
  expected: Record<string, number>;
  values: Record<string, number>;
}

type RowChange = RowEdit;

const SLUG = 'multi-ward';

@Component({
  selector: 'app-multi-ward',
  standalone: true,
  imports: [CommonModule, FormsModule, HisFixShellComponent],
  templateUrl: './multi-ward.component.html'
})
export class MultiWardComponent {
  readonly results = signal<MultiWardResult[]>([]);
  readonly activeIndex = signal(0);
  // Keyed by `${table}#${id}`, unique per displayed row (primary keys of two different tables).
  readonly edits = signal<Record<string, RowEdit>>({});
  readonly auditEntries = signal<HisFixAuditEntry[]>([]);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly isSearching = signal(false);
  readonly isSaving = signal(false);
  readonly hasSearched = signal(false);
  readonly dryRun = signal(false);

  readonly active = computed<MultiWardResult | null>(() => this.results()[this.activeIndex()] ?? null);

  // Only ticked rows whose chosen values differ from the shown ones are sent, and only the differing flags.
  readonly changes = computed<RowChange[]>(() => {
    const list: RowChange[] = [];
    for (const edit of Object.values(this.edits())) {
      const values: Record<string, number> = {};
      for (const [key, value] of Object.entries(edit.values)) {
        if (value !== edit.expected[key]) {
          values[key] = value;
        }
      }
      if (Object.keys(values).length > 0) {
        list.push({ table: edit.table, id: edit.id, label: edit.label, expected: { ...edit.expected }, values });
      }
    }
    return list;
  });

  mrn = '';
  ticketNo = '';
  reason = '';
  private searchedMrn = '';

  constructor(private readonly api: HisFixApiService) {}

  search(encounterId?: number): void {
    const mrn = encounterId !== undefined ? this.searchedMrn : this.mrn.trim();
    if (!mrn) {
      this.errorMessage.set('Sila masukkan MRN pesakit.');
      return;
    }
    const params: Record<string, string> = { mrn };
    if (encounterId !== undefined) {
      params['encounterId'] = String(encounterId);
    }
    this.isSearching.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.resetSelection();
    this.api.search<MultiWardResult[]>(SLUG, params).subscribe({
      next: (results) => {
        this.searchedMrn = mrn;
        this.results.set(results);
        this.activeIndex.set(0);
        this.hasSearched.set(true);
        this.isSearching.set(false);
        this.loadAudit();
      },
      error: (error) => {
        if (encounterId === undefined) {
          this.results.set([]);
          this.auditEntries.set([]);
        }
        this.hasSearched.set(true);
        this.isSearching.set(false);
        this.errorMessage.set(error?.error?.message ?? 'Carian gagal.');
      }
    });
  }

  selectPatient(index: string | number): void {
    this.activeIndex.set(Number(index));
    this.resetSelection();
  }

  switchEncounter(value: string | number): void {
    const id = Number(value);
    const current = this.active();
    if (!current || !id || id === current.encounterId) {
      return;
    }
    this.search(id);
  }

  admissionKey(row: WardAdmissionRow): string {
    return `enc_admissions#${row.admissionId}`;
  }

  transferKey(row: WardTransferRow): string {
    return `enc_transfers#${row.transferId}`;
  }

  isTicked(key: string): boolean {
    return !!this.edits()[key];
  }

  edit(key: string): RowEdit | null {
    return this.edits()[key] ?? null;
  }

  toggleAdmission(row: WardAdmissionRow, checked: boolean): void {
    const flags = { admission_active: row.admissionActive ?? 0, admission_confirmed: row.admissionConfirmed ?? 0 };
    this.toggle(this.admissionKey(row), checked, {
      table: 'enc_admissions',
      id: row.admissionId,
      label: `admission ${row.admissionId} (${row.wardName || row.wardCode || '-'})`,
      expected: flags,
      values: { ...flags }
    });
  }

  toggleTransfer(row: WardTransferRow, checked: boolean): void {
    const flags = { transfer_confirmed: row.transferConfirmed ?? 0 };
    this.toggle(this.transferKey(row), checked, {
      table: 'enc_transfers',
      id: row.transferId,
      label: `transfer ${row.transferId} (${row.wardSourceName || row.wardSource || '-'} -> ${row.wardDestinationName || row.wardDestination || '-'})`,
      expected: flags,
      values: { ...flags }
    });
  }

  setValue(key: string, column: string, value: string | number): void {
    const current = this.edits()[key];
    if (!current) {
      return;
    }
    this.edits.set({ ...this.edits(), [key]: { ...current, values: { ...current.values, [column]: Number(value) } } });
    this.dryRun.set(false);
  }

  canApply(): boolean {
    return !!this.active() && this.changes().length > 0;
  }

  confirmText(): string {
    const result = this.active();
    if (!result) {
      return '';
    }
    const lines = this.changes().map((change) =>
      `- ${change.label}: ` + Object.keys(change.values)
        .map((key) => `${key} ${change.expected[key]} -> ${change.values[key]}`).join(', '));
    return `Ubah ${lines.length} baris pada encounter ${result.encounterId} (MRN ${result.patientMrn}):\n${lines.join('\n')}`;
  }

  apply(): void {
    const result = this.active();
    const changes = this.changes();
    if (!result || changes.length === 0) {
      return;
    }
    this.isSaving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.api.apply<MultiWardResult>(SLUG, {
      mrn: this.searchedMrn,
      encounterId: result.encounterId,
      changes: changes.map((change) => ({ table: change.table, id: change.id, expected: change.expected, values: change.values })),
      ticketNo: this.ticketNo.trim(),
      reason: this.reason
    }).subscribe({
      next: (response) => {
        this.isSaving.set(false);
        this.dryRun.set(response.dryRun);
        this.ticketNo = '';
        this.reason = '';
        if (response.dryRun) {
          this.successMessage.set(`DRY RUN: ${changes.length} baris disemak (tidak disimpan).`);
        } else {
          const index = this.activeIndex();
          this.results.set(this.results().map((item, i) => (i === index ? response.data : item)));
          this.edits.set({});
          this.successMessage.set(`${changes.length} baris berjaya dikemas kini.`);
          this.loadAudit();
        }
      },
      error: (error) => {
        this.isSaving.set(false);
        this.errorMessage.set(error?.error?.message ?? 'Pembetulan gagal.');
      }
    });
  }

  private toggle(key: string, checked: boolean, edit: RowEdit): void {
    const next = { ...this.edits() };
    if (checked) {
      next[key] = edit;
    } else {
      delete next[key];
    }
    this.edits.set(next);
    this.dryRun.set(false);
    this.successMessage.set('');
  }

  private resetSelection(): void {
    this.edits.set({});
    this.dryRun.set(false);
    this.ticketNo = '';
    this.reason = '';
  }

  private loadAudit(): void {
    this.api.audit(SLUG, this.searchedMrn).subscribe({
      next: (entries) => this.auditEntries.set(entries),
      error: () => this.auditEntries.set([])
    });
  }
}
