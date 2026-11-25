import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { LoginComponent } from './features/auth/login.component';
import { DashboardLayoutComponent } from './layout/dashboard-layout.component';
import { DashboardHomeComponent } from './features/dashboard/dashboard-home.component';
import { MaintenanceRequestsComponent } from './features/maintenance/maintenance-requests.component';
import { InpatientReportComponent } from './features/reports/inpatient-report.component';
import { ReportAuditLogComponent } from './features/reports/report-audit-log.component';
import { ReportsMenuComponent } from './features/reports/reports-menu.component';
import { ReportPlaceholderComponent } from './features/reports/report-placeholder.component';
import { DamaDiagnosisReportComponent } from './features/reports/dama-diagnosis-report.component';
import { DischargeDateComponent } from './features/discharge-date/discharge-date.component';
import { CompleteTransferComponent } from './features/his-fixes/complete-transfer/complete-transfer.component';
import { BirthTimeComponent } from './features/his-fixes/birth-time/birth-time.component';
import { DocumentStatusComponent } from './features/his-fixes/document-status/document-status.component';
import { EncounterActiveComponent } from './features/his-fixes/encounter-active/encounter-active.component';
import { UnlockDocumentComponent } from './features/his-fixes/unlock-document/unlock-document.component';
import { TicketReportComponent } from './features/tickets/ticket-report.component';

export const appRoutes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: DashboardLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'reports' },
      { path: 'dashboard', component: DashboardHomeComponent },
      { path: 'requests', component: MaintenanceRequestsComponent },
      { path: 'reports', component: ReportsMenuComponent },
      { path: 'reports/pd103', component: InpatientReportComponent },
      {
        path: 'reports/lean',
        component: ReportPlaceholderComponent,
        data: {
          reportTitle: 'Data Lean',
          reportDescription: 'Listing atau report untuk data Lean seperti yang diminta oleh HSI.',
          reportStatus: 'Menu disediakan. Logik report belum dibina.'
        }
      },
      {
        path: 'reports/admission-etd',
        component: ReportPlaceholderComponent,
        data: {
          reportTitle: 'Data Admission (ETD) Bulanan',
          reportDescription: 'Laporan bulanan untuk data admission berdasarkan ETD.',
          reportStatus: 'Menu disediakan. Query dan output report belum dibina.'
        }
      },
      {
        path: 'reports/dama-diagnosis',
        component: DamaDiagnosisReportComponent
      },
      {
        path: 'reports/referral-emergency-psychiatry',
        component: ReportPlaceholderComponent,
        data: {
          reportTitle: 'Referral Jabatan Kecemasan ke Psikiatri',
          reportDescription: 'Laporan tahunan bilangan referral daripada Jabatan Kecemasan ke Jabatan Psikiatri.',
          reportStatus: 'Menu disediakan. Report tahunan belum dibina.'
        }
      },
      {
        path: 'reports/referral-other-departments-psychiatry',
        component: ReportPlaceholderComponent,
        data: {
          reportTitle: 'Referral Jabatan Lain ke Psikiatri',
          reportDescription: 'Laporan tahunan referral daripada Medical, Surgical, Ortho, O&G dan Paeds ke Jabatan Psikiatri.',
          reportStatus: 'Menu disediakan. Report tahunan belum dibina.'
        }
      },
      {
        path: 'reports/active-user-ids',
        component: ReportPlaceholderComponent,
        data: {
          reportTitle: 'Data Semua UserID Aktif',
          reportDescription: 'Listing semua userID yang masih aktif sehingga kini.',
          reportStatus: 'Menu disediakan. Sumber data user aktif belum disambungkan.'
        }
      },
      {
        path: 'reports/discharge-summary-exception',
        component: ReportPlaceholderComponent,
        data: {
          reportTitle: 'Pesakit Tanpa Discharge Summary Baharu',
          reportDescription: 'Senarai pesakit yang doktor tidak submit discharge summary selepas pembatalan dan penghantaran baharu.',
          reportStatus: 'Menu disediakan. Kriteria semakan exception belum dibina.'
        }
      },
      {
        path: 'reports/referral-case-breakdown',
        component: ReportPlaceholderComponent,
        data: {
          reportTitle: 'Kes Rujukan ke Psikiatri Mengikut Jenis Kes',
          reportDescription: 'Laporan tahunan referral daripada Medical, Surgical, O&G, Paed dan Ortho dengan pecahan kes baharu dan ulangan.',
          reportStatus: 'Menu disediakan. Pecahan kes baharu dan ulangan belum dibina.'
        }
      },
      { path: 'reports/audit', component: ReportAuditLogComponent },
      { path: 'discharge-date', component: DischargeDateComponent },
      { path: 'death-date', component: DischargeDateComponent, data: { mode: 'DEATH' } },
      { path: 'ticket-report', component: TicketReportComponent },
      { path: 'fix/complete-transfer', component: CompleteTransferComponent },
      { path: 'fix/unlock-document', component: UnlockDocumentComponent },
      { path: 'fix/birth-time', component: BirthTimeComponent },
      { path: 'fix/document-status', component: DocumentStatusComponent },
      { path: 'fix/encounter-active', component: EncounterActiveComponent }
    ]
  },
  { path: '**', redirectTo: '' }
];

