import { Component, ElementRef, ViewChild, effect, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { API_BASE_URL } from '../../core/api.config';
import { AiAssistantService, AiAssistantChatResponse } from '../../core/ai-assistant.service';

interface ChatMessage {
  role: 'assistant' | 'user';
  content: string;
  actionType?: string;
  actionLabel?: string;
  downloadUrl?: string;
}

@Component({
  selector: 'app-ai-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-assistant.component.html',
  styleUrl: './ai-assistant.component.css'
})
export class AiAssistantComponent {
  @ViewChild('messagesPanel') private readonly messagesPanel?: ElementRef<HTMLDivElement>;

  readonly isOpen = signal(false);
  readonly isSending = signal(false);
  readonly messages = signal<ChatMessage[]>([
    {
      role: 'assistant',
      content: 'Saya AI Assistant untuk HSI Utilities. Saya boleh bantu terangkan menu, report sedia ada, dan langkah asas troubleshooting atau deploy.'
    }
  ]);

  draft = '';
  modelLabel = '';

  constructor(private readonly aiAssistantService: AiAssistantService) {
    effect(() => {
      this.messages();
      queueMicrotask(() => this.scrollToBottom());
    });
  }

  toggle(): void {
    this.isOpen.set(!this.isOpen());
  }

  send(): void {
    const message = this.draft.trim();
    if (!message || this.isSending()) {
      return;
    }

    const history = this.messages().slice(0);
    this.messages.set(history.concat([{ role: 'user', content: message }]));
    this.draft = '';
    this.isSending.set(true);

    this.aiAssistantService.chat({
      message,
      history: history.filter((item) => !!item.content?.trim())
    }).subscribe({
      next: (response: AiAssistantChatResponse) => {
        this.isSending.set(false);
        this.modelLabel = response.model || '';
        this.messages.set(this.messages().concat([{
          role: 'assistant',
          content: response.reply || 'Tiada jawapan diterima.',
          actionType: response.actionType,
          actionLabel: response.actionLabel,
          downloadUrl: response.downloadUrl
        }]));
      },
      error: (error) => {
        this.isSending.set(false);
        const reply = error?.status === 401
          ? 'Session anda mungkin telah tamat. Sila login semula.'
          : error?.status === 403
            ? 'AI Assistant hanya dibenarkan untuk pengguna ADMIN.'
            : 'AI Assistant tidak dapat dihubungi sekarang. Cuba lagi sebentar.';
        this.messages.set(this.messages().concat([{ role: 'assistant', content: reply }]));
      }
    });
  }

  sendOnEnter(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  triggerDownload(url: string | undefined): void {
    if (!url) {
      return;
    }

    const absoluteUrl = this.toAbsoluteApiUrl(url);
    window.open(absoluteUrl, '_blank', 'noopener');
  }

  private toAbsoluteApiUrl(url: string): string {
    if (/^https?:\/\//i.test(url)) {
      return url;
    }

    const apiOrigin = API_BASE_URL.replace(/\/api$/, '');
    if (url.startsWith('/')) {
      return apiOrigin + url;
    }

    return apiOrigin + '/' + url;
  }

  private scrollToBottom(): void {
    const panel = this.messagesPanel?.nativeElement;
    if (!panel) {
      return;
    }
    panel.scrollTop = panel.scrollHeight;
  }
}
