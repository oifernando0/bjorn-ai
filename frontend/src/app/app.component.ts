import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription, interval } from 'rxjs';
import { Conversation } from './models/conversation';
import { Message } from './models/message';
import { ChatService } from './services/chat.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {
  conversation?: Conversation;
  messages: Message[] = [];
  currentInput = '';
  loading = false;
  errorMessage = '';
  private streamSub?: Subscription;

  constructor(private readonly chatService: ChatService) {}

  ngOnInit(): void {
    this.startConversation();
  }

  ngOnDestroy(): void {
    this.streamSub?.unsubscribe();
  }

  get hasMessages(): boolean {
    return this.messages.length > 0;
  }

  private startConversation(): void {
    const title = `Sessão ${new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}`;
    this.chatService.createConversation(title).subscribe({
      next: (conversation) => {
        this.conversation = conversation;
        this.loadMessages();
      },
      error: () => {
        this.errorMessage = 'Não foi possível iniciar a conversa. Verifique o backend e tente novamente.';
      }
    });
  }

  private loadMessages(): void {
    if (!this.conversation) {
      return;
    }

    this.chatService.listMessages(this.conversation.id).subscribe({
      next: (messages) => {
        this.messages = messages.map((msg) => ({ ...msg, streaming: false }));
      },
      error: () => {
        this.errorMessage = 'Não foi possível carregar o histórico da conversa.';
      }
    });
  }

  sendMessage(): void {
    if (!this.conversation || !this.currentInput.trim()) {
      return;
    }

    const content = this.currentInput.trim();
    this.currentInput = '';
    const userMessage: Message = {
      role: 'USER',
      content,
      createdAt: new Date().toISOString(),
      streaming: false
    };
    this.messages = [...this.messages, userMessage];
    this.loading = true;
    this.errorMessage = '';

    this.chatService.sendMessage(this.conversation.id, content).subscribe({
      next: (response) => {
        this.animateAssistantResponse(response.content ?? '');
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Não foi possível enviar a mensagem. Verifique se o backend está ativo.';
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  private animateAssistantResponse(fullContent: string): void {
    this.streamSub?.unsubscribe();
    const assistantMessage: Message = {
      role: 'ASSISTANT',
      content: '',
      createdAt: new Date().toISOString(),
      streaming: true
    };
    this.messages = [...this.messages, assistantMessage];

    const characters = [...fullContent];
    const speed = Math.min(30, Math.max(8, Math.floor(1800 / (characters.length || 1))));
    this.streamSub = interval(speed).subscribe((index) => {
      assistantMessage.content += characters[index] ?? '';
      if (index + 1 >= characters.length) {
        assistantMessage.streaming = false;
        this.streamSub?.unsubscribe();
      }
    });
  }

  trackMessage(index: number, message: Message): string {
    return message.id ?? `${message.role}-${index}`;
  }
}
