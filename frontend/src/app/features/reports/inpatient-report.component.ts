import { Component, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { API_BASE_URL } from '../../core/api.config';
import { AuthService } from '../../core/auth.service';
import { ActivatedRoute } from '@angular/router';

interface WardOption {
  code: string;
  name: string;
}

interface DisciplineMetric {
  male: number;
  female: number;
}

interface DisciplineColumn {
  code: string;
  name: string;
}

interface ReportRow {
  no: string;
  item: string;
  detail?: string;
  totalMale: number;
  totalFemale: number;
  total: number;
  editable?: boolean;
  metricIndex?: number;
  disciplineMetrics: DisciplineMetric[];
}

interface InpatientReportResponse {
  reportDate: string;
  wardCode: string;
  wardName?: string;
  disciplines: DisciplineColumn[];
  rows: ReportRow[];
}

interface ReportCellUpdate {
  disciplineCode: string;
  metricIndex: number;
  male: number;
  female: number;
}

interface InpatientReportUpdateRequest {
  wardCode: string;
  reportDate: string;
  ticketNo: string;
  updates: ReportCellUpdate[];
}

@Component({
  selector: 'app-inpatient-report',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './inpatient-report.component.html',
  styleUrl: './inpatient-report.component.css'
})
export class InpatientReportComponent implements OnInit {
  readonly wards = signal<WardOption[]>([]);
  readonly reportRows = signal<ReportRow[]>([]);
  readonly draftRows = signal<ReportRow[]>([]);
  readonly disciplines = signal<DisciplineColumn[]>([]);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly isLoading = signal(false);
  readonly isSaving = signal(false);
  readonly isEditMode = signal(false);

  selectedReport = '(PD103) Bancian Harian Wad:Pin. 2/2009';
  selectedWardCode = '';
  selectedWardName = 'UNIT ICU/CCU';
  selectedDate = this.today();
  ticketNo = '';
  generatedWardName = this.selectedWardName;
  generatedDate = this.selectedDate;

  constructor(
    private readonly http: HttpClient,
    public readonly authService: AuthService,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.http.get<WardOption[]>(`${API_BASE_URL}/reports/wards`).subscribe({
      next: (wards) => {
        this.wards.set(wards);
        if (wards.length > 0) {
          const wardCodeFromRoute = this.route.snapshot.queryParamMap.get('wardCode');
          const reportDateFromRoute = this.route.snapshot.queryParamMap.get('reportDate');
          const matchedWard = wards.find((ward) => ward.code === wardCodeFromRoute) ?? wards[0];
          this.selectedWardCode = matchedWard.code;
          this.selectedWardName = matchedWard.name;
          this.generatedWardName = matchedWard.name;
          if (reportDateFromRoute) {
            this.selectedDate = reportDateFromRoute;
          }
          this.generate();
        }
      },
      error: (httpError: any) => {
        const message = typeof httpError?.error === 'string'
          ? httpError.error
          : (httpError?.error?.message ?? ('Unable to load ward list. HTTP ' + (httpError?.status ?? 'unknown')));
        this.errorMessage.set(message);
        this.wards.set([]);
      }
    });
  }

  generate(): void {
    if (!this.selectedWardCode) {
      this.errorMessage.set('Please select a ward first.');
      return;
    }

    this.isLoading.set(true);
    this.isEditMode.set(false);
    this.successMessage.set('');
    this.errorMessage.set('');

    const params = new HttpParams()
      .set('wardCode', this.selectedWardCode)
      .set('reportDate', this.selectedDate);

    this.http.get<InpatientReportResponse>(`${API_BASE_URL}/reports/inpatient`, { params }).subscribe({
      next: (response) => {
        const selectedWard = this.wards().find((ward) => ward.code === this.selectedWardCode);
        this.generatedWardName = response.wardName || selectedWard?.name || this.selectedWardName;
        this.generatedDate = response.reportDate || this.selectedDate;
        this.disciplines.set((response.disciplines ?? []).filter((discipline) => !!discipline.name?.trim()));
        this.reportRows.set(response.rows ?? []);
        this.draftRows.set(this.cloneRows(response.rows ?? []));
        this.isLoading.set(false);
      },
      error: (error) => {
        this.isLoading.set(false);
        this.errorMessage.set(error?.error?.message ?? 'Unable to generate report data.');
      }
    });
  }

  reset(): void {
    this.selectedReport = '(PD103) Bancian Harian Wad:Pin. 2/2009';
    this.selectedDate = this.today();

    const firstWard = this.wards()[0];
    this.selectedWardCode = firstWard?.code ?? '';
    this.selectedWardName = firstWard?.name ?? 'UNIT ICU/CCU';
    this.generatedWardName = this.selectedWardName;
    this.generatedDate = this.selectedDate;
    this.generate();
  }

  onWardChange(): void {
    const selectedWard = this.wards().find((ward) => ward.code === this.selectedWardCode);
    this.selectedWardName = selectedWard?.name ?? 'UNIT ICU/CCU';
  }

  printReport(): void {
    window.print();
  }

  canEditReport(): boolean {
    return this.authService.currentUser()?.role === 'ADMIN';
  }

  startEdit(): void {
    if (!this.canEditReport()) {
      this.errorMessage.set('Only ADMIN users can edit this report.');
      return;
    }
    this.successMessage.set('');
    this.errorMessage.set('');
    this.draftRows.set(this.cloneRows(this.reportRows()));
    this.ticketNo = '';
    this.isEditMode.set(true);
  }

  cancelEdit(): void {
    this.draftRows.set(this.cloneRows(this.reportRows()));
    this.ticketNo = '';
    this.isEditMode.set(false);
  }

  saveEdit(): void {
    const updates = this.buildUpdates();
    if (updates.length === 0) {
      this.successMessage.set('No changes to save.');
      this.isEditMode.set(false);
      return;
    }
    if (!this.ticketNo.trim()) {
      this.errorMessage.set('Sila masukkan No. Tiket sebelum menyimpan.');
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const payload: InpatientReportUpdateRequest = {
      wardCode: this.selectedWardCode,
      reportDate: this.selectedDate,
      ticketNo: this.ticketNo.trim(),
      updates
    };

    this.http.post<InpatientReportResponse>(`${API_BASE_URL}/reports/inpatient`, payload).subscribe({
      next: (response) => {
        this.reportRows.set(response.rows ?? []);
        this.draftRows.set(this.cloneRows(response.rows ?? []));
        this.disciplines.set((response.disciplines ?? []).filter((discipline) => !!discipline.name?.trim()));
        this.generatedWardName = response.wardName || this.generatedWardName;
        this.generatedDate = response.reportDate || this.generatedDate;
        this.isSaving.set(false);
        this.isEditMode.set(false);
        this.ticketNo = '';
        this.successMessage.set('Report data updated successfully.');
      },
      error: (error) => {
        this.isSaving.set(false);
        this.errorMessage.set(error?.status === 403 ? 'Only ADMIN users can save report changes.' : (error?.error?.message ?? 'Unable to save report changes.'));
      }
    });
  }

  visibleDisciplines(): DisciplineColumn[] {
    const disciplines = this.disciplines().slice(0, 6);
    while (disciplines.length < 6) {
      disciplines.push({ code: '', name: '' });
    }
    return disciplines;
  }

  rowsForDisplay(): ReportRow[] {
    return this.isEditMode() ? this.draftRows() : this.reportRows();
  }

  metricsForRow(row: ReportRow): DisciplineMetric[] {
    const metrics = row.disciplineMetrics.slice(0, 6);
    while (metrics.length < 6) {
      metrics.push({ male: 0, female: 0 });
    }
    return metrics;
  }

  metricDisplayValue(metric: DisciplineMetric, discipline: DisciplineColumn | undefined, gender: 'male' | 'female'): string | number {
    const value = metric[gender];
    if (!discipline?.name && value === 0) {
      return '';
    }
    return value;
  }

  editableCell(row: ReportRow, discipline: DisciplineColumn | undefined): boolean {
    return this.isEditMode() && !!row.editable && !!row.metricIndex && !!discipline?.code;
  }

  updateMetric(rowIndex: number, metricIndex: number, gender: 'male' | 'female', value: string): void {
    const rows = this.cloneRows(this.draftRows());
    const parsed = Number(value);
    rows[rowIndex].disciplineMetrics[metricIndex][gender] = isNaN(parsed) || parsed < 0 ? 0 : Math.trunc(parsed);
    this.draftRows.set(rows);
  }

  isCellChanged(rowIndex: number, metricIndex: number, gender: 'male' | 'female'): boolean {
    if (!this.isEditMode()) {
      return false;
    }
    const draftMetric = this.draftRows()[rowIndex]?.disciplineMetrics[metricIndex];
    const currentMetric = this.reportRows()[rowIndex]?.disciplineMetrics[metricIndex];
    return !!draftMetric && !!currentMetric && draftMetric[gender] !== currentMetric[gender];
  }

  shouldRenderItemCell(row: ReportRow): boolean {
    return row.no !== '6' && row.no !== '7'
      && row.no !== '12' && row.no !== '13' && row.no !== '14' && row.no !== '15'
      && row.no !== '21' && row.no !== '22';
  }

  itemRowSpan(row: ReportRow): number {
    if (row.no === '5') {
      return 3;
    }
    if (row.no === '11') {
      return 5;
    }
    if (row.no === '20') {
      return 3;
    }
    return 1;
  }

  trackByIndex(index: number): number {
    return index;
  }

  private buildUpdates(): ReportCellUpdate[] {
    const updates: ReportCellUpdate[] = [];
    const rows = this.draftRows();
    const currentRows = this.reportRows();
    const disciplines = this.disciplines();

    for (let rowIndex = 0; rowIndex < rows.length; rowIndex++) {
      const draftRow = rows[rowIndex];
      const currentRow = currentRows[rowIndex];
      if (!draftRow?.editable || !draftRow.metricIndex) {
        continue;
      }

      for (let disciplineIndex = 0; disciplineIndex < disciplines.length; disciplineIndex++) {
        const discipline = disciplines[disciplineIndex];
        const draftMetric = draftRow.disciplineMetrics[disciplineIndex];
        const currentMetric = currentRow?.disciplineMetrics[disciplineIndex];
        if (!discipline?.code || !draftMetric || !currentMetric) {
          continue;
        }

        if (draftMetric.male !== currentMetric.male || draftMetric.female !== currentMetric.female) {
          updates.push({
            disciplineCode: discipline.code,
            metricIndex: draftRow.metricIndex,
            male: draftMetric.male,
            female: draftMetric.female
          });
        }
      }
    }

    return updates;
  }

  private cloneRows(rows: ReportRow[]): ReportRow[] {
    return (rows ?? []).map((row) => ({
      ...row,
      disciplineMetrics: (row.disciplineMetrics ?? []).map((metric) => ({ ...metric }))
    }));
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
