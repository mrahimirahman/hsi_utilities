import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

interface ReportMenuItem {
  title: string;
  description: string;
  route: string;
  status: 'Ready' | 'Pending';
  frequency: string;
  icon: string;
  category: string;
}

@Component({
  selector: 'app-reports-menu',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './reports-menu.component.html',
  styleUrl: './reports-menu.component.css'
})
export class ReportsMenuComponent {
  readonly reportItems: ReportMenuItem[] = [
    {
      title: 'Update PD103 Bancian Harian Wad',
      description: 'Modul sedia ada untuk semakan, cetakan, dan kemas kini data PD103.',
      route: '/reports/pd103',
      status: 'Ready',
      frequency: 'Harian',
      icon: 'bed',
      category: 'Banci Wad'
    },
    {
      title: 'Data Lean',
      description: 'Menu awal untuk report Data Lean.',
      route: '/reports/lean',
      status: 'Pending',
      frequency: 'TBC',
      icon: 'speed',
      category: 'Lean Healthcare'
    },
    {
      title: 'Data Admission (ETD) Bulanan',
      description: 'Menu awal untuk laporan admission ETD secara bulanan.',
      route: '/reports/admission-etd',
      status: 'Pending',
      frequency: 'Bulanan',
      icon: 'emergency',
      category: 'Unit ETD'
    },
    {
      title: 'Data Diagnosis DAMA Bulanan',
      description: 'Pilih tempoh tarikh dan jana fail Excel untuk kes discaj jenis DAMA.',
      route: '/reports/dama-diagnosis',
      status: 'Ready',
      frequency: 'Bulanan',
      icon: 'clinical_notes',
      category: 'Rekod Klinikal'
    },
    {
      title: 'Referral Jabatan Kecemasan ke Psikiatri',
      description: 'Menu awal untuk laporan tahunan referral dari Jabatan Kecemasan.',
      route: '/reports/referral-emergency-psychiatry',
      status: 'Pending',
      frequency: 'Tahunan',
      icon: 'psychology',
      category: 'Psikiatri & ETD'
    },
    {
      title: 'Referral Jabatan Lain ke Psikiatri',
      description: 'Menu awal untuk laporan tahunan referral dari Medical, Surgical, Ortho, O&G dan Paeds.',
      route: '/reports/referral-other-departments-psychiatry',
      status: 'Pending',
      frequency: 'Tahunan',
      icon: 'sync_alt',
      category: 'Inter-Jabatan'
    },
    {
      title: 'Data Semua UserID Aktif',
      description: 'Menu awal untuk listing semua userID yang masih aktif.',
      route: '/reports/active-user-ids',
      status: 'Pending',
      frequency: 'Semasa',
      icon: 'badge',
      category: 'Akses Pengguna'
    },
    {
      title: 'Pesakit Tanpa Discharge Summary Baharu',
      description: 'Menu awal untuk exception report selepas pembatalan discharge summary.',
      route: '/reports/discharge-summary-exception',
      status: 'Pending',
      frequency: 'Permohonan baharu',
      icon: 'assignment_late',
      category: 'Pematuhan Discaj'
    },
    {
      title: 'Kes Rujukan ke Psikiatri Mengikut Jenis Kes',
      description: 'Menu awal untuk pecahan kes baharu dan kes ulangan.',
      route: '/reports/referral-case-breakdown',
      status: 'Pending',
      frequency: 'Tahunan',
      icon: 'bar_chart',
      category: 'Banci Epidemiologi'
    }
  ];

  readonly frequencies = Array.from(new Set(this.reportItems.map((item) => item.frequency)));
  readonly readyCount = this.reportItems.filter((item) => item.status === 'Ready').length;
  readonly readyPercent = Math.round((this.readyCount / this.reportItems.length) * 100);

  readonly query = signal('');
  readonly frequencyFilter = signal('ALL');
  readonly statusFilter = signal('ALL');

  readonly visibleItems = computed(() => {
    const q = this.query().trim().toLowerCase();
    const frequency = this.frequencyFilter();
    const status = this.statusFilter();

    return this.reportItems
      .map((item, index) => ({ ...item, number: String(index + 1).padStart(2, '0') }))
      .filter((item) =>
        (!q || `${item.title} ${item.description} ${item.category}`.toLowerCase().includes(q)) &&
        (frequency === 'ALL' || item.frequency === frequency) &&
        (status === 'ALL' || item.status === status)
      );
  });

  readonly hasFilters = computed(
    () => !!this.query() || this.frequencyFilter() !== 'ALL' || this.statusFilter() !== 'ALL'
  );

  resetFilters(): void {
    this.query.set('');
    this.frequencyFilter.set('ALL');
    this.statusFilter.set('ALL');
  }
}
