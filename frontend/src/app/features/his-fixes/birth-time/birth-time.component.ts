import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HisFixApiService, HisFixAuditEntry } from '../his-fix-api.service';
import { HisFixShellComponent } from '../shared/his-fix-shell.component';

interface BirthTimeRow {
  personId: number;
  patientMrn: string;
  personName: string | null;
  personBirthdate: string | null;
  personBirthtime: string;
}

const SLUG = 'birth-time';

@Component({
  selector: 'app-birth-time',
  standalone: true,
  imports: [CommonModule, FormsModule, HisFixShellComponent],
  templateUrl: './birth-time.component.html'
})
export class BirthTimeComponent {
  readonly rows = signal<BirthTimeRow[]>([]);
  readonly selected = signal<BirthTimeRow | null>(null);
  readonly auditEntries = signal<HisFixAuditEntry[]>([]);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly isSearching = signal(false);
  readonly isSaving = signal(false);
  readonly hasSearched = signal(false);
  readonly dryRun = signal(false);

  mrn = '';
  newBirthTime = '';
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
    this.selected.set(null);
    this.dryRun.set(false);
    this.api.search<BirthTimeRow[]>(SLUG, { mrn }).subscribe({
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

  select(row: BirthTimeRow): void {
    this.selected.set(row);
    this.dryRun.set(false);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.newBirthTime = row.personBirthtime ?? '';
    this.ticketNo = '';
    this.reason = '';
  }

  canApply(): boolean {
    const row = this.selected();
    return !!row && !!this.newBirthTime && this.newBirthTime !== (row.personBirthtime ?? '');
  }

  confirmText(): string {
    const row = this.selected();
    return row
      ? `Ubah masa lahir ${row.patientMrn} (person ${row.personId}).\nperson_birthtime: ${row.personBirthtime || '(kosong)'} -> ${this.newBirthTime}`
      : '';
  }

  apply(): void {
    const row = this.selected();
    if (!row) {
      return;
    }
    this.isSaving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.api.apply<BirthTimeRow>(SLUG, {
      personId: row.personId,
      expectedBirthTime: row.personBirthtime ?? '',
      newBirthTime: this.newBirthTime,
      ticketNo: this.ticketNo.trim(),
      reason: this.reason
    }).subscribe({
      next: (result) => {
        this.isSaving.set(false);
        this.dryRun.set(result.dryRun);
        this.ticketNo = '';
        this.reason = '';
        if (result.dryRun) {
          this.successMessage.set(`DRY RUN: person_birthtime ${row.personBirthtime || '(kosong)'} -> ${result.data.personBirthtime} (tidak disimpan).`);
        } else {
          this.rows.set(this.rows().map((item) => (item.personId === row.personId ? result.data : item)));
          this.selected.set(null);
          this.successMessage.set('Masa lahir berjaya dikemas kini.');
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
