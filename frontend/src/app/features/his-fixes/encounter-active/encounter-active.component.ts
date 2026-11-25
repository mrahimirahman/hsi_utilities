import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HisFixApiService, HisFixAuditEntry } from '../his-fix-api.service';
import { HisFixShellComponent } from '../shared/his-fix-shell.component';

interface EncounterActiveRow {
  encounterId: string;
  patientMrn: string;
  personName: string | null;
  domainCode: string | null;
  encounterTimestamp: string | null;
  encounterActive: number;
  dischargeDate: string | null;
  locationCode: string | null;
  locationName: string | null;
}

const SLUG = 'encounter-active';

@Component({
  selector: 'app-encounter-active',
  standalone: true,
  imports: [CommonModule, FormsModule, HisFixShellComponent],
  templateUrl: './encounter-active.component.html'
})
export class EncounterActiveComponent {
  readonly rows = signal<EncounterActiveRow[]>([]);
  readonly selectedId = signal<string | null>(null);
  readonly newActive = signal<number | null>(null);
  readonly auditEntries = signal<HisFixAuditEntry[]>([]);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly isSearching = signal(false);
  readonly isSaving = signal(false);
  readonly hasSearched = signal(false);
  readonly dryRun = signal(false);

  mrn = '';
  ticketNo = '';
  reason = '';
  private searchedMrn = '';

  constructor(private readonly api: HisFixApiService) {}

  search(): void {
    const mrn = this.mrn.trim();
    if (!mrn) {
      this.errorMessage.set('Sila masukkan MRN pesakit.');
      return;
    }
    this.isSearching.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.selectedId.set(null);
    this.newActive.set(null);
    this.dryRun.set(false);
    this.api.search<EncounterActiveRow[]>(SLUG, { mrn }).subscribe({
      next: (rows) => {
        this.searchedMrn = mrn;
        this.rows.set(rows);
        this.hasSearched.set(true);
        this.isSearching.set(false);
        this.loadAudit();
      },
      error: (error) => {
        this.rows.set([]);
        this.auditEntries.set([]);
        this.hasSearched.set(true);
        this.isSearching.set(false);
        this.errorMessage.set(error?.error?.message ?? 'Carian gagal.');
      }
    });
  }

  selected(): EncounterActiveRow | null {
    const id = this.selectedId();
    return this.rows().find((row) => row.encounterId === id) ?? null;
  }

  select(row: EncounterActiveRow): void {
    this.selectedId.set(row.encounterId);
    this.newActive.set(null); // explicit choice required, no default
    this.dryRun.set(false);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.ticketNo = '';
    this.reason = '';
  }

  setNewActive(value: number): void {
    this.newActive.set(value);
    this.dryRun.set(false);
  }

  canApply(): boolean {
    const row = this.selected();
    const value = this.newActive();
    return !!row && value !== null && value !== row.encounterActive;
  }

  confirmText(): string {
    const row = this.selected();
    return row
      ? `Ubah encounter ${row.encounterId} (MRN ${this.searchedMrn}).\nencounter_active: ${row.encounterActive} -> ${this.newActive()}`
      : '';
  }

  apply(): void {
    const row = this.selected();
    const value = this.newActive();
    if (!row || value === null) {
      return;
    }
    this.isSaving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.api.apply<EncounterActiveRow>(SLUG, {
      mrn: this.searchedMrn,
      encounterId: row.encounterId,
      expectedActive: row.encounterActive,
      newActive: value,
      ticketNo: this.ticketNo.trim(),
      reason: this.reason
    }).subscribe({
      next: (result) => {
        this.isSaving.set(false);
        this.dryRun.set(result.dryRun);
        this.ticketNo = '';
        this.reason = '';
        if (result.dryRun) {
          this.successMessage.set(`DRY RUN: encounter_active ${row.encounterActive} -> ${value} (tidak disimpan).`);
        } else {
          this.rows.set(this.rows().map((item) => (item.encounterId === result.data.encounterId ? result.data : item)));
          this.selectedId.set(null);
          this.newActive.set(null);
          this.successMessage.set('Encounter active berjaya dikemas kini.');
          this.loadAudit();
        }
      },
      error: (error) => {
        this.isSaving.set(false);
        this.errorMessage.set(error?.error?.message ?? 'Pembetulan gagal.');
      }
    });
  }

  private loadAudit(): void {
    this.api.audit(SLUG, this.searchedMrn).subscribe({
      next: (entries) => this.auditEntries.set(entries),
      error: () => this.auditEntries.set([])
    });
  }
}
