import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { API_BASE_URL } from '../../core/api.config';

interface DischargeEncounterRow {
  encounterId: string;
  admissionId: string;
  patientMrn: string;
  personName: string;
  personNewIc: string;
  dischargeWard: string;
  dischargeWardName: string | null;
  encounterDate: string;
  encounterDischargeDate: string | null;
  encDischargeDate: string | null;
  preDischargeDate: string | null;
  preDischargeTime: string | null;
  deathDate: string | null;
  dischargeTypeCode: string | null;
}

interface DischargeDateAuditEntry {
  id: number;
  module: string | null;
  ticketNo: string | null;
  encounterId: string;
  patientMrn: string;
  encounterDischargeDateBefore: string | null;
  encounterDischargeDateAfter: string | null;
  encDischargeDateBefore: string | null;
  encDischargeDateAfter: string | null;
  preDischargeDateBefore: string | null;
  preDischargeDateAfter: string | null;
  preDischargeTimeBefore: string | null;
  preDischargeTimeAfter: string | null;
  deathDateBefore: string | null;
  deathDateAfter: string | null;
  reason: string | null;
  editedBy: string;
  editedName: string;
  editedAt: string;
}

@Component({
  selector: 'app-discharge-date',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './discharge-date.component.html',
  styleUrl: './discharge-date.component.css'
})
// Serves both "Update Discharge Time" and "Update Death Date"; route data { mode: 'DEATH' }
// limits the search to discharges with a death_date and adds the enc_discharges.death_date field.
export class DischargeDateComponent {
  readonly deathMode: boolean;
  readonly rows = signal<DischargeEncounterRow[]>([]);
  readonly selected = signal<DischargeEncounterRow | null>(null);
  readonly auditEntries = signal<DischargeDateAuditEntry[]>([]);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly isSearching = signal(false);
  readonly isSaving = signal(false);
  readonly hasSearched = signal(false);

  mrn = '';
  dischargeDate = '';
  preDischargeDate = '';
  preDischargeTime = '';
  deathDate = '';
  ticketNo = '';
  reason = '';

  constructor(private readonly http: HttpClient, route: ActivatedRoute) {
    this.deathMode = route.snapshot.data['mode'] === 'DEATH';
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
    this.selected.set(null);
    this.auditEntries.set([]);

    const params = new HttpParams().set('mrn', mrn).set('mode', this.deathMode ? 'DEATH' : 'DISCHARGE');
    this.http.get<DischargeEncounterRow[]>(`${API_BASE_URL}/discharge-date/encounters`, { params }).subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.hasSearched.set(true);
        this.isSearching.set(false);
      },
      error: (error) => {
        this.rows.set([]);
        this.hasSearched.set(true);
        this.isSearching.set(false);
        this.errorMessage.set(error?.error?.message ?? 'Unable to search patient encounters.');
      }
    });
  }

  select(row: DischargeEncounterRow): void {
    this.selected.set(row);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.dischargeDate = this.dateTimeLocal(row.encounterDischargeDate)
      || (row.encDischargeDate ? `${this.datePart(row.encDischargeDate)}T00:00` : '');
    this.preDischargeDate = this.datePart(row.preDischargeDate);
    this.preDischargeTime = (row.preDischargeTime ?? '').slice(0, 5);
    this.deathDate = this.dateTimeLocal(row.deathDate);
    this.ticketNo = '';
    this.reason = '';
    this.loadAudit(row.encounterId);
  }

  cancel(): void {
    this.selected.set(null);
    this.auditEntries.set([]);
  }

  save(): void {
    const row = this.selected();
    if (!row) {
      return;
    }
    if (!this.ticketNo.trim()) {
      this.errorMessage.set('Sila masukkan No. Tiket.');
      return;
    }
    if (!this.dischargeDate || !this.preDischargeDate || !this.preDischargeTime) {
      this.errorMessage.set('Sila lengkapkan Discharge Date & Time, Pre Discharge Date dan Pre Discharge Time.');
      return;
    }
    if (this.deathMode && !this.deathDate) {
      this.errorMessage.set('Sila lengkapkan Death Date & Time.');
      return;
    }
    const label = this.deathMode ? 'tarikh kematian' : 'tarikh discaj';
    if (!window.confirm(`Kemaskini ${label} untuk ${row.patientMrn} (encounter ${row.encounterId}), No. Tiket ${this.ticketNo.trim()}?`)) {
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.http.post<DischargeEncounterRow>(`${API_BASE_URL}/discharge-date/encounters`, {
      encounterId: row.encounterId,
      dischargeDate: this.dischargeDate,
      preDischargeDate: this.preDischargeDate,
      preDischargeTime: this.preDischargeTime,
      mode: this.deathMode ? 'DEATH' : 'DISCHARGE',
      deathDate: this.deathMode ? this.deathDate : null,
      ticketNo: this.ticketNo.trim(),
      reason: this.reason
    }).subscribe({
      next: (updated) => {
        const merged = { ...row, ...updated, personName: row.personName, personNewIc: row.personNewIc };
        this.rows.set(this.rows().map((item) => item.encounterId === merged.encounterId ? merged : item));
        this.selected.set(merged);
        this.ticketNo = '';
        this.reason = '';
        this.isSaving.set(false);
        this.successMessage.set(this.deathMode ? 'Tarikh kematian berjaya dikemaskini.' : 'Tarikh discaj berjaya dikemaskini.');
        this.loadAudit(merged.encounterId);
      },
      error: (error) => {
        this.isSaving.set(false);
        this.errorMessage.set(error?.error?.message ?? 'Unable to update discharge date.');
      }
    });
  }

  private loadAudit(encounterId: string): void {
    const params = new HttpParams().set('encounterId', encounterId);
    this.http.get<DischargeDateAuditEntry[]>(`${API_BASE_URL}/discharge-date/audit`, { params }).subscribe({
      next: (entries) => this.auditEntries.set(entries),
      error: () => this.auditEntries.set([])
    });
  }

  // 'YYYY-MM-DD[ HH:mm:ss]' -> 'DD-MM-YYYY[ HH:mm:ss]' for display; '-' when empty
  fmt(value: string | null): string {
    if (!value) {
      return '-';
    }
    const match = /^(\d{4})-(\d{2})-(\d{2})(.*)$/.exec(value);
    return match ? `${match[3]}-${match[2]}-${match[1]}${match[4]}` : value;
  }

  // 'YYYY-MM-DD HH:mm:ss' -> 'YYYY-MM-DDTHH:mm' for <input type="datetime-local">
  private dateTimeLocal(value: string | null): string {
    return value ? value.slice(0, 16).replace(' ', 'T') : '';
  }

  private datePart(value: string | null): string {
    return value ? value.slice(0, 10) : '';
  }
}
