import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../../core/api.config';

interface MaintenanceRequest {
  id: number;
  title: string;
  location: string;
  status: string;
  priority: string;
  description: string;
  assignedTeam: string;
  requestDate: string;
}

@Component({
  selector: 'app-maintenance-requests',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './maintenance-requests.component.html',
  styleUrl: './maintenance-requests.component.css'
})
export class MaintenanceRequestsComponent implements OnInit {
  readonly requests = signal<MaintenanceRequest[]>([]);

  constructor(private readonly http: HttpClient) {}

  ngOnInit(): void {
    this.http.get<MaintenanceRequest[]>(`${API_BASE_URL}/requests`).subscribe((response) => {
      this.requests.set(response);
    });
  }
}
