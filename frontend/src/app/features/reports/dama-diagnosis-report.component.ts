import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { API_BASE_URL } from '../../core/api.config';

interface DamaDiagnosisRow {
  patientMrn: string;
  personRegister: string;
  personName: string;
  diagnosisClinical: string;
  dischargeTypeName: string;
  dischargeWard: string;
  admissionDate: string;
  dischargeDate: string;
  personNewIc: string;
  personAge: string;
  personCurCity: string;
  personCurState: string;
}

interface CountEntry {
  label: string;
  count: number;
  percent: number;
}

interface DamaDiagnosisReportResponse {
  fromDate: string;
  toDate: string;
  totalRecords: number;
  rows: DamaDiagnosisRow[];
}

@Component({
  selector: 'app-dama-diagnosis-report',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './dama-diagnosis-report.component.html',
  styleUrl: './dama-diagnosis-report.component.css'
})
export class DamaDiagnosisReportComponent implements OnInit {
  readonly reportData = signal<DamaDiagnosisReportResponse | null>(null);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly isLoading = signal(false);
  readonly isDownloading = signal(false);

  fromDate = this.firstDayOfMonth();
  toDate = this.lastDayOfMonth();

  /** Ward breakdown computed from the loaded rows (descending by count). */
  readonly wardBreakdown = computed(() => this.countBy((row) => row.dischargeWard));

  /** Top clinical diagnoses computed from the loaded rows. */
  readonly topDiagnoses = computed(() => this.countBy((row) => row.diagnosisClinical).slice(0, 5));

  readonly topWard = computed<CountEntry | null>(() => this.wardBreakdown()[0] ?? null);

  constructor(private readonly http: HttpClient) {}

  ngOnInit(): void {
    this.generate();
  }

  generate(): void {
    if (!this.validateDates()) {
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const params = new HttpParams()
      .set('fromDate', this.fromDate)
      .set('toDate', this.toDate);

    this.http.get<DamaDiagnosisReportResponse>(`${API_BASE_URL}/reports/dama-diagnosis`, { params }).subscribe({
      next: (response) => {
        this.reportData.set(response);
        this.isLoading.set(false);
        this.successMessage.set(`Jumpa ${response.totalRecords} rekod untuk tempoh dipilih.`);
      },
      error: (error) => {
        this.isLoading.set(false);
        this.reportData.set(null);
        this.errorMessage.set(error?.error?.message ?? 'Unable to load DAMA diagnosis report.');
      }
    });
  }

  downloadExcel(): void {
    if (!this.validateDates()) {
      return;
    }

    this.isDownloading.set(true);
    this.errorMessage.set('');

    const params = new HttpParams()
      .set('fromDate', this.fromDate)
      .set('toDate', this.toDate);

    this.http.get(`${API_BASE_URL}/reports/dama-diagnosis/export`, {
      params,
      responseType: 'blob',
      observe: 'response'
    }).subscribe({
      next: (response) => {
        const blob = response.body;
        if (!blob) {
          this.errorMessage.set('Empty file received from server.');
          this.isDownloading.set(false);
          return;
        }

        const fileName = this.extractFileName(response.headers.get('content-disposition'))
          || `data-diagnosis-dama-${this.fromDate}-to-${this.toDate}.xls`;

        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        link.click();
        window.URL.revokeObjectURL(url);

        this.isDownloading.set(false);
        this.successMessage.set(`Fail ${fileName} berjaya dijana.`);
      },
      error: () => {
        this.isDownloading.set(false);
        this.errorMessage.set('Unable to download Excel file.');
      }
    });
  }

  reset(): void {
    this.fromDate = this.firstDayOfMonth();
    this.toDate = this.lastDayOfMonth();
    this.generate();
  }

  previewRows(): DamaDiagnosisRow[] {
    return this.reportData()?.rows ?? [];
  }

  private countBy(pick: (row: DamaDiagnosisRow) => string): CountEntry[] {
    const rows = this.previewRows();
    if (rows.length === 0) {
      return [];
    }
    const counts = new Map<string, number>();
    for (const row of rows) {
      const key = (pick(row) ?? '').trim() || 'Tidak dinyatakan';
      counts.set(key, (counts.get(key) ?? 0) + 1);
    }
    return Array.from(counts.entries())
      .map(([label, count]) => ({ label, count, percent: Math.round((count / rows.length) * 1000) / 10 }))
      .sort((a, b) => b.count - a.count || a.label.localeCompare(b.label));
  }

  private validateDates(): boolean {
    if (!this.fromDate || !this.toDate) {
      this.errorMessage.set('Please select both From Date and To Date.');
      return false;
    }
    if (this.fromDate > this.toDate) {
      this.errorMessage.set('From Date cannot be later than To Date.');
      return false;
    }
    return true;
  }

  private extractFileName(contentDisposition: string | null): string {
    if (!contentDisposition) {
      return '';
    }
    const match = /filename="?([^";]+)"?/i.exec(contentDisposition);
    return match?.[1] ?? '';
  }

  private firstDayOfMonth(): string {
    const today = new Date();
    return new Date(today.getFullYear(), today.getMonth(), 1).toISOString().slice(0, 10);
  }

  private lastDayOfMonth(): string {
    const today = new Date();
    return new Date(today.getFullYear(), today.getMonth() + 1, 0).toISOString().slice(0, 10);
  }
}
