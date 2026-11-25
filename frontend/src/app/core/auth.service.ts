import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap, catchError, of } from 'rxjs';
import { API_BASE_URL } from './api.config';

export interface LoginPayload {
  username: string;
  password: string;
}

export interface SessionUser {
  authenticated: boolean;
  username: string;
  fullName: string;
  role: string;
  department: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly currentUser = signal<SessionUser | null>(null);

  constructor(private readonly http: HttpClient) {}

  login(payload: LoginPayload): Observable<SessionUser> {
    return this.http.post<SessionUser>(`${API_BASE_URL}/auth/login`, payload).pipe(
      tap((user) => this.currentUser.set(user))
    );
  }

  restoreSession(): Observable<boolean> {
    if (this.currentUser()) {
      return of(true);
    }

    return this.http.get<SessionUser>(`${API_BASE_URL}/auth/session`).pipe(
      tap((user) => this.currentUser.set(user)),
      map((user) => user.authenticated),
      catchError(() => of(false))
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/auth/logout`, {}).pipe(
      tap(() => this.currentUser.set(null))
    );
  }
}
