import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';

export interface AiAssistantMessagePayload {
  role: 'user' | 'assistant';
  content: string;
}

export interface AiAssistantChatRequest {
  message: string;
  history: AiAssistantMessagePayload[];
}

export interface AiAssistantChatResponse {
  success: boolean;
  configured: boolean;
  reply: string;
  model: string;
  actionType?: string;
  actionLabel?: string;
  downloadUrl?: string;
}

@Injectable({ providedIn: 'root' })
export class AiAssistantService {
  constructor(private readonly http: HttpClient) {}

  chat(payload: AiAssistantChatRequest): Observable<AiAssistantChatResponse> {
    return this.http.post<AiAssistantChatResponse>(`${API_BASE_URL}/assistant/chat`, payload);
  }
}
