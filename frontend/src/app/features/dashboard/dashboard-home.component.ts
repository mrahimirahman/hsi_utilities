import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../../core/api.config';

interface DashboardResponse {
  totalRequests: number;
  openRequests: number;
  inProgressRequests: number;
  completedRequests: number;
  recentRequests: Array<{
    id: number;
    title: string;
    location: string;
    status: string;
    priority: string;
    assignedTeam: string;
  }>;
}

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-home.component.html',
  styleUrl: './dashboard-home.component.css'
})
export class DashboardHomeComponent implements OnInit {
  readonly dashboard = signal<DashboardResponse | null>(null);

  constructor(private readonly http: HttpClient) {}

  ngOnInit(): void {
    this.http.get<DashboardResponse>(`${API_BASE_URL}/dashboard`).subscribe((response) => {
      this.dashboard.set(response);
    });
  }
}
