import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { API_BASE_URL } from '../../core/api.config';

interface LabelCount {
  label: string;
  count: number;
}

interface TicketDashboard {
  fromDate: string;
  toDate: string;
  totalTickets: number;
  totalTransactions: number;
  dischargeCount: number;
  deathCount: number;
  pd103Count: number;
  activeUsers: number;
  daily: LabelCount[];
  topUsers: LabelCount[];
}

interface TicketReportRow {
  ticketNo: string;
  module: 'DISCHARGE' | 'DEATH' | 'PD103';
  editedAt: string;
  editedBy: string;
  editedName: string | null;
  patientMrn: string | null;
  encounterId: string | null;
  wardCode: string | null;
  wardName: string | null;
  reportDate: string | null;
  changeCount: number;
  changeSummary: string | null;
  reason: string | null;
}

interface TicketReport {
  fromDate: string;
  toDate: string;
  totalRecords: number;
  rows: TicketReportRow[];
}

// Laporan Tiket: dashboard of every update registered with a Ticket No (opens on the dashboard
// for the current month), plus the full ticket list and an Excel export on demand.
@Component({
  selector: 'app-ticket-report',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ticket-report.component.html',
  styleUrl: './ticket-report.component.css'
})
export class TicketReportComponent implements OnInit {
  readonly dashboard = signal<TicketDashboard | null>(null);
  readonly report = signal<TicketReport | null>(null);
  readonly errorMessage = signal('');
  readonly isLoadingDashboard = signal(false);
  readonly isLoadingReport = signal(false);
  readonly isDownloading = signal(false);

  readonly moduleLabels: Record<string, string> = {
    DISCHARGE: 'Discharge',
    DEATH: 'Death',
    PD103: 'PD103'
  };

  fromDate = this.firstDayOfMonth();
  toDate = this.today();

  constructor(private readonly http: HttpClient) {}

  ngOnInit(): void {
    this.generateDashboard();
  }

  busy(): boolean {
    return this.isLoadingDashboard() || this.isLoadingReport() || this.isDownloading();
  }

  generateDashboard(): void {
    if (!this.validateDates()) {
      return;
    }
    this.isLoadingDashboard.set(true);
    this.errorMessage.set('');
    this.http.get<TicketDashboard>(`${API_BASE_URL}/tickets/dashboard`, { params: this.params() }).subscribe({
      next: (response) => {
        this.dashboard.set(response);
        this.isLoadingDashboard.set(false);
      },
      error: (error) => {
        this.isLoadingDashboard.set(false);
        this.errorMessage.set(this.describeError(error, 'Unable to load ticket dashboard.'));
      }
    });
  }

  generateReport(): void {
    if (!this.validateDates()) {
      return;
    }
    this.isLoadingReport.set(true);
    this.errorMessage.set('');
    this.http.get<TicketReport>(`${API_BASE_URL}/tickets/report`, { params: this.params() }).subscribe({
      next: (response) => {
        this.report.set(response);
        this.isLoadingReport.set(false);
      },
      error: (error) => {
        this.isLoadingReport.set(false);
        this.report.set(null);
        this.errorMessage.set(this.describeError(error, 'Unable to load ticket report.'));
      }
    });
  }

  downloadExcel(): void {
    if (!this.validateDates()) {
      return;
    }
    this.isDownloading.set(true);
    this.errorMessage.set('');
    this.http.get(`${API_BASE_URL}/tickets/report/export`, {
      params: this.params(),
      responseType: 'blob',
      observe: 'response'
    }).subscribe({
      next: (response) => {
        const blob = response.body;
        this.isDownloading.set(false);
        if (!blob) {
          this.errorMessage.set('Empty file received from server.');
          return;
        }
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `laporan-tiket-${this.fromDate}-to-${this.toDate}.xls`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.isDownloading.set(false);
        this.errorMessage.set('Unable to download Excel file.');
      }
    });
  }

  reset(): void {
    this.fromDate = this.firstDayOfMonth();
    this.toDate = this.today();
    this.report.set(null);
    this.generateDashboard();
  }

  modulePercent(count: number): number {
    const total = this.dashboard()?.totalTransactions ?? 0;
    return total === 0 ? 0 : Math.round((count / total) * 100);
  }

  dailyMax(): number {
    return Math.max(1, ...(this.dashboard()?.daily ?? []).map((day) => day.count));
  }

  userMax(): number {
    return Math.max(1, ...(this.dashboard()?.topUsers ?? []).map((user) => user.count));
  }

  // Label every bar when the range is short; otherwise every 5th day plus the last one.
  showDayLabel(index: number, total: number): boolean {
    return total <= 14 || index % 5 === 0 || index === total - 1;
  }

  reference(row: TicketReportRow): string {
    if (row.module === 'PD103') {
      return `${row.wardName || row.wardCode || '-'} · ${this.fmt(row.reportDate)}`;
    }
    return `${row.patientMrn || '-'} · Encounter ${row.encounterId || '-'}`;
  }

  // 'YYYY-MM-DD[ HH:mm:ss]' -> 'DD-MM-YYYY[ HH:mm:ss]' for display; '-' when empty
  fmt(value: string | null): string {
    if (!value) {
      return '-';
    }
    const match = /^(\d{4})-(\d{2})-(\d{2})(.*)$/.exec(value);
    return match ? `${match[3]}-${match[2]}-${match[1]}${match[4]}` : value;
  }

  // 'YYYY-MM-DD' -> 'DD' for the chart axis
  dayOf(value: string): string {
    return value.slice(8, 10);
  }

  private params(): HttpParams {
    return new HttpParams().set('fromDate', this.fromDate).set('toDate', this.toDate);
  }

  private validateDates(): boolean {
    if (!this.fromDate || !this.toDate) {
      this.errorMessage.set('Sila pilih From Date dan To Date.');
      return false;
    }
    if (this.fromDate > this.toDate) {
      this.errorMessage.set('From Date tidak boleh selepas To Date.');
      return false;
    }
    return true;
  }

  private describeError(error: { status?: number; error?: { message?: string } }, fallback: string): string {
    if (error?.status === 403) {
      return 'Laporan Tiket hanya untuk pengguna ADMIN.';
    }
    return error?.error?.message ?? fallback;
  }

  private today(): string {
    return this.localDate(new Date());
  }

  private firstDayOfMonth(): string {
    const now = new Date();
    return this.localDate(new Date(now.getFullYear(), now.getMonth(), 1));
  }

  // local calendar date (toISOString would shift to UTC and can land on the previous day in Malaysia)
  private localDate(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }
}
