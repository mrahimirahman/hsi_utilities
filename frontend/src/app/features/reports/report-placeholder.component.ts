import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-report-placeholder',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './report-placeholder.component.html',
  styleUrl: './report-placeholder.component.css'
})
export class ReportPlaceholderComponent {
  constructor(private readonly route: ActivatedRoute) {}

  get reportTitle(): string {
    return this.route.snapshot.data['reportTitle'] ?? 'Report';
  }

  get reportDescription(): string {
    return this.route.snapshot.data['reportDescription'] ?? 'Menu placeholder untuk report yang akan dibina kemudian.';
  }

  get reportStatus(): string {
    return this.route.snapshot.data['reportStatus'] ?? 'Belum dibina';
  }
}
