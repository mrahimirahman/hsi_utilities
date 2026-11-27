import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HisFixApiService, HisFixAuditEntry } from '../his-fix-api.service';
import { HisFixShellComponent } from '../shared/his-fix-shell.component';

interface DocumentLockRow {
  docDetailAccessId: number;
  docDetailId: number;
  encounterId: string;
  patientMrn: string;
  personName: string | null;
  reportName: string | null;
  createdDate: string | null;
  uidUserid: string | null;
  datetimeOpen: string | null;
  datetimeClose: string | null;
  accessFlag: number | null;
}

const SLUG = 'unlock-document';
const MAX_IDS = 50;

@Component({
  selector: 'app-unlock-document',
  standalone: true,
  imports: [CommonModule, FormsModule, HisFixShellComponent],
  templateUrl: './unlock-document.component.html'
})
export class UnlockDocumentComponent {
  readonly rows = signal<DocumentLockRow[]>([]);
  readonly selectedIds = signal<number[]>([]);
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
    this.selectedIds.set([]);
    this.dryRun.set(false);
    this.api.search<DocumentLockRow[]>(SLUG, { mrn }).subscribe({
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

  isSelected(row: DocumentLockRow): boolean {
    return this.selectedIds().includes(row.docDetailAccessId);
  }

  toggle(row: DocumentLockRow): void {
    const current = this.selectedIds();
    if (current.includes(row.docDetailAccessId)) {
      this.selectedIds.set(current.filter((id) => id !== row.docDetailAccessId));
    } else if (current.length >= MAX_IDS) {
      this.errorMessage.set(`Maksimum ${MAX_IDS} dokumen sekali gus.`);
      return;
    } else {
      this.selectedIds.set([...current, row.docDetailAccessId]);
    }
    this.resetForm();
  }

  allSelected(): boolean {
    return this.rows().length > 0 && this.selectedIds().length === Math.min(this.rows().length, MAX_IDS);
  }

  toggleAll(): void {
    this.selectedIds.set(this.allSelected() ? [] : this.rows().slice(0, MAX_IDS).map((row) => row.docDetailAccessId));
    this.resetForm();
  }

  private resetForm(): void {
    this.dryRun.set(false);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.ticketNo = '';
    this.reason = '';
  }

  confirmText(): string {
    const count = this.selectedIds().length;
    return count > 0
      ? `Buka kunci ${count} dokumen untuk MRN ${this.searchedMrn}.\naccess_flag: 1 -> 0`
      : '';
  }

  apply(): void {
    const ids = this.selectedIds();
    if (ids.length === 0) {
      return;
    }
    this.isSaving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.api.apply<DocumentLockRow[]>(SLUG, {
      mrn: this.searchedMrn,
      docDetailAccessIds: ids,
      ticketNo: this.ticketNo.trim(),
      reason: this.reason
    }).subscribe({
      next: (result) => {
        this.isSaving.set(false);
        this.dryRun.set(result.dryRun);
        this.ticketNo = '';
        this.reason = '';
        if (result.dryRun) {
          // Nothing was saved: keep the rows pending.
          this.successMessage.set(`DRY RUN: ${ids.length} dokumen akan dibuka kunci (access_flag 1 -> 0, tidak disimpan).`);
        } else {
          this.rows.set(this.rows().filter((item) => !ids.includes(item.docDetailAccessId)));
          this.selectedIds.set([]);
          this.successMessage.set(`${ids.length} dokumen berjaya dibuka kunci.`);
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
