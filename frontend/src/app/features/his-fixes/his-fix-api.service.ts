import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, shareReplay } from 'rxjs';
import { API_BASE_URL } from '../../core/api.config';

export interface HisFixAuditEntry {
  id: number;
  module: string;
  ticketNo: string;
  patientMrn: string | null;
  encounterId: string | null;
  targetTable: string;
  targetKey: string;
  beforeJson: string | null;
  afterJson: string | null;
  reason: string | null;
  editedBy: string;
  editedName: string | null;
  editedAt: string;
}

export interface HisFixMode {
  dryRun: boolean;
}

export interface HisFixResult<T> {
  data: T;
  dryRun: boolean;
}

@Injectable({ providedIn: 'root' })
export class HisFixApiService {
  private readonly base = `${API_BASE_URL}/his-fix/`;

  private mode$: Observable<HisFixMode> | null = null;

  constructor(private readonly http: HttpClient) {}

  // Fetched once per app session and cached (the server setting only changes on restart).
  mode(): Observable<HisFixMode> {
    if (!this.mode$) {
      this.mode$ = this.http.get<HisFixMode>(`${this.base}mode`).pipe(shareReplay(1));
    }
    return this.mode$;
  }

  search<T>(slug: string, params: Record<string, string>): Observable<T> {
    return this.http.get<T>(`${this.base}${slug}/search`, { params: new HttpParams({ fromObject: params }) });
  }

  lookup<T>(slug: string, path: string): Observable<T> {
    return this.http.get<T>(`${this.base}${slug}/${path}`);
  }

  apply<T>(slug: string, body: object): Observable<HisFixResult<T>> {
    return this.http.post<HisFixResult<T>>(`${this.base}${slug}/apply`, body);
  }

  rehide<T>(slug: string, body: object): Observable<HisFixResult<T>> {
    return this.http.post<HisFixResult<T>>(`${this.base}${slug}/rehide-all`, body);
  }

  audit(slug: string, mrn: string): Observable<HisFixAuditEntry[]> {
    return this.http.get<HisFixAuditEntry[]>(`${this.base}${slug}/audit`, { params: new HttpParams().set('mrn', mrn) });
  }
}
