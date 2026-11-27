import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HisFixApiService, HisFixAuditEntry } from '../his-fix-api.service';
import { HisFixShellComponent } from '../shared/his-fix-shell.component';

interface DocumentStatusRow {
  docDetailId: number;
  encounterId: string;
  patientMrn: string;
  personName: string | null;
  reportName: string | null;
  createdDate: string | null;
  createdBy: string | null;
  statusCode: string;
  statusDetail: string | null;
}

const SLUG = 'document-status';
const SUBMITTED = '01';
const MARK_AS_ERROR = '107';

@Component({
  selector: 'app-document-status',
  standalone: true,
  imports: [CommonModule, FormsModule, HisFixShellComponent],
  templateUrl: './document-status.component.html'
})
export class DocumentStatusComponent {
  readonly rows = signal<DocumentStatusRow[]>([]);
  readonly selectedId = signal<number | null>(null);
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
    this.dryRun.set(false);
    this.api.search<DocumentStatusRow[]>(SLUG, { mrn }).subscribe({
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

  selected(): DocumentStatusRow | null {
    const id = this.selectedId();
    return this.rows().find((row) => row.docDetailId === id) ?? null;
  }

  select(row: DocumentStatusRow): void {
    this.selectedId.set(row.docDetailId);
    this.dryRun.set(false);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.ticketNo = '';
    this.reason = '';
  }

  targetStatus(row: DocumentStatusRow): string {
    return row.statusCode === SUBMITTED ? MARK_AS_ERROR : SUBMITTED;
  }

  actionLabel(row: DocumentStatusRow): string {
    return row.statusCode === SUBMITTED ? 'Tandakan Mark as Error' : 'Kembalikan ke Submitted';
  }

  confirmText(): string {
    const row = this.selected();
    return row
      ? `${this.actionLabel(row)} untuk dokumen ${row.docDetailId} (MRN ${this.searchedMrn}).\nstatus_code: ${row.statusCode} -> ${this.targetStatus(row)}`
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
    this.api.apply<DocumentStatusRow>(SLUG, {
      mrn: this.searchedMrn,
      docDetailId: row.docDetailId,
      expectedStatus: row.statusCode,
      ticketNo: this.ticketNo.trim(),
      reason: this.reason
    }).subscribe({
      next: (result) => {
        this.isSaving.set(false);
        this.dryRun.set(result.dryRun);
        this.ticketNo = '';
        this.reason = '';
        if (result.dryRun) {
          this.successMessage.set(`DRY RUN: status_code ${row.statusCode} -> ${this.targetStatus(row)} (tidak disimpan).`);
        } else {
          this.rows.set(this.rows().map((item) => (item.docDetailId === result.data.docDetailId ? result.data : item)));
          this.selectedId.set(null);
          this.successMessage.set('Status dokumen berjaya dikemas kini.');
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
