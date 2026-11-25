import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HisFixApiService, HisFixAuditEntry } from '../his-fix-api.service';
import { HisFixShellComponent } from '../shared/his-fix-shell.component';

interface BedClassRow {
  admissionId: number;
  encounterId: string;
  patientMrn: string;
  wardCode: string | null;
  wardName: string | null;
  bedCode: string | null;
  classCode: string | null;
  classDesc: string | null;
  admissionActive: number | null;
  admissionConfirmed: number | null;
}

interface ClassOption {
  classCode: string;
  classDesc: string | null;
}

const SLUG = 'bed-class';

@Component({
  selector: 'app-bed-class',
  standalone: true,
  imports: [CommonModule, FormsModule, HisFixShellComponent],
  templateUrl: './bed-class.component.html'
})
export class BedClassComponent implements OnInit {
  readonly rows = signal<BedClassRow[]>([]);
  readonly classes = signal<ClassOption[]>([]);
  readonly selectedId = signal<number | null>(null);
  readonly newClass = signal('');
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

  ngOnInit(): void {
    this.api.lookup<ClassOption[]>(SLUG, 'classes').subscribe({
      next: (list) => this.classes.set(list),
      error: (error) => this.errorMessage.set(error?.error?.message ?? 'Senarai kelas gagal dimuatkan.')
    });
  }

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
    this.newClass.set('');
    this.dryRun.set(false);
    this.api.search<BedClassRow[]>(SLUG, { mrn }).subscribe({
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

  selected(): BedClassRow | null {
    const id = this.selectedId();
    return this.rows().find((row) => row.admissionId === id) ?? null;
  }

  select(row: BedClassRow): void {
    this.selectedId.set(row.admissionId);
    this.newClass.set(''); // explicit choice required, no default
    this.dryRun.set(false);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.ticketNo = '';
    this.reason = '';
  }

  setNewClass(value: string): void {
    this.newClass.set(value);
    this.dryRun.set(false);
  }

  canApply(): boolean {
    const row = this.selected();
    const value = this.newClass();
    return !!row && !!row.classCode && value !== '' && value !== row.classCode;
  }

  confirmText(): string {
    const row = this.selected();
    return row
      ? `Ubah admission ${row.admissionId} (MRN ${row.patientMrn}, katil ${row.bedCode || '-'}).\nclass_code: ${row.classCode} -> ${this.newClass()}`
      : '';
  }

  apply(): void {
    const row = this.selected();
    const value = this.newClass();
    if (!row || !row.classCode || !value) {
      return;
    }
    this.isSaving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.api.apply<BedClassRow>(SLUG, {
      mrn: this.searchedMrn,
      admissionId: row.admissionId,
      expectedClassCode: row.classCode,
      newClassCode: value,
      ticketNo: this.ticketNo.trim(),
      reason: this.reason
    }).subscribe({
      next: (result) => {
        this.isSaving.set(false);
        this.dryRun.set(result.dryRun);
        this.ticketNo = '';
        this.reason = '';
        if (result.dryRun) {
          this.successMessage.set(`DRY RUN: class_code ${row.classCode} -> ${value} (tidak disimpan).`);
        } else {
          this.rows.set(this.rows().map((item) => (item.admissionId === result.data.admissionId ? result.data : item)));
          this.selectedId.set(null);
          this.newClass.set('');
          this.successMessage.set('Kelas katil berjaya dikemas kini.');
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
