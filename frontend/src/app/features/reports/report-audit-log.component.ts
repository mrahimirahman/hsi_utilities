import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { API_BASE_URL } from '../../core/api.config';
import { AuthService } from '../../core/auth.service';
import { Router } from '@angular/router';

interface WardOption {
  code: string;
  name: string;
}

interface ReportAuditLogEntry {
  id: number;
  reportCode: string;
  wardCode: string;
  wardName?: string;
  reportDate: string;
  disciplineCode: string;
  disciplineName?: string;
  metricIndex: number;
  metricLabel: string;
  previousMale: number;
  previousFemale: number;
  newMale: number;
  newFemale: number;
  editedBy: string;
  editedName?: string;
  editedRole?: string;
  editedAt: string;
  ticketNo?: string | null;
}

@Component({
  selector: 'app-report-audit-log',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './report-audit-log.component.html',
  styleUrl: './report-audit-log.component.css'
})
export class ReportAuditLogComponent implements OnInit {
  readonly wards = signal<WardOption[]>([]);
  readonly entries = signal<ReportAuditLogEntry[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');

  /** Summary figures derived from the currently loaded entries only. */
  readonly uniqueUserCount = computed(() => new Set(this.entries().map((e) => e.editedBy)).size);
  readonly uniqueWardCount = computed(() => new Set(this.entries().map((e) => e.wardCode)).size);
  readonly uniqueReportDateCount = computed(() => new Set(this.entries().map((e) => e.reportDate)).size);

  selectedWardCode = '';
  selectedDate = '';

  constructor(
    private readonly http: HttpClient,
    public readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadWards();
  }

  loadAuditLog(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    let params = new HttpParams();
    if (this.selectedWardCode) {
      params = params.set('wardCode', this.selectedWardCode);
    }
    if (this.selectedDate) {
      params = params.set('reportDate', this.selectedDate);
    }

    this.http.get<ReportAuditLogEntry[]>(`${API_BASE_URL}/reports/audit`, { params }).subscribe({
      next: (entries) => {
        this.entries.set(entries ?? []);
        this.isLoading.set(false);
      },
      error: (error) => {
        this.isLoading.set(false);
        this.entries.set([]);
        this.errorMessage.set(error?.status === 403
          ? 'Only ADMIN users can view the audit log.'
          : (error?.error?.message ?? 'Unable to load report audit log.'));
      }
    });
  }


  openReport(entry: ReportAuditLogEntry): void {
    void this.router.navigate(['/reports/pd103'], {
      queryParams: {
        wardCode: entry.wardCode,
        reportDate: entry.reportDate
      }
    });
  }

  resetFilters(): void {
    this.selectedWardCode = '';
    this.selectedDate = '';
    this.loadAuditLog();
  }

  private loadWards(): void {
    this.http.get<WardOption[]>(`${API_BASE_URL}/reports/wards`).subscribe({
      next: (wards) => {
        this.wards.set(wards ?? []);
        this.loadAuditLog();
      },
      error: (error) => {
        this.errorMessage.set(error?.error?.message ?? 'Unable to load ward list for audit log.');
        this.loadAuditLog();
      }
    });
  }
}

