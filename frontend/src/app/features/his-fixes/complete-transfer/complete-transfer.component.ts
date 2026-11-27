import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HisFixApiService, HisFixAuditEntry } from '../his-fix-api.service';
import { HisFixShellComponent } from '../shared/his-fix-shell.component';

interface TransferRow {
  transferId: number;
  encounterId: string;
  patientMrn: string;
  personName: string | null;
  wardSource: string | null;
  wardSourceName: string | null;
  wardDestination: string | null;
  wardDestinationName: string | null;
  transferTimestamp: string | null;
  transferConfirmed: number | null;
  destinationAdmissionActive: number | null;
  destinationAdmissionConfirmed: number | null;
}

const SLUG = 'complete-transfer';

@Component({
  selector: 'app-complete-transfer',
  standalone: true,
  imports: [CommonModule, FormsModule, HisFixShellComponent],
  templateUrl: './complete-transfer.component.html'
})
export class CompleteTransferComponent {
  readonly rows = signal<TransferRow[]>([]);
  readonly selected = signal<TransferRow | null>(null);
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
    this.selected.set(null);
    this.dryRun.set(false);
    this.api.search<TransferRow[]>(SLUG, { mrn }).subscribe({
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

  select(row: TransferRow): void {
    this.selected.set(row);
    this.dryRun.set(false);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.ticketNo = '';
    this.reason = '';
  }

  confirmText(): string {
    const row = this.selected();
    return row
      ? `Sahkan pemindahan ${row.patientMrn}: ${row.wardSource ?? '-'} -> ${row.wardDestination ?? '-'} (transfer ${row.transferId}).\ntransfer_confirmed: ${row.transferConfirmed} -> 1`
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
    this.api.apply<TransferRow>(SLUG, {
      transferId: row.transferId,
      expectedTransferConfirmed: row.transferConfirmed,
      ticketNo: this.ticketNo.trim(),
      reason: this.reason
    }).subscribe({
      next: (result) => {
        this.isSaving.set(false);
        this.dryRun.set(result.dryRun);
        this.ticketNo = '';
        this.reason = '';
        if (result.dryRun) {
          // Nothing was saved: keep the row pending and show what the value would become.
          this.successMessage.set(`DRY RUN: transfer_confirmed ${row.transferConfirmed} -> ${result.data.transferConfirmed} (tidak disimpan).`);
        } else {
          this.rows.set(this.rows().filter((item) => item.transferId !== row.transferId));
          this.selected.set(null);
          this.successMessage.set('Pemindahan berjaya disahkan.');
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
