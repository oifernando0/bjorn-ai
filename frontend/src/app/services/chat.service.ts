import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Conversation } from '../models/conversation';
import { Message } from '../models/message';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  createConversation(title: string, knowledgeBaseId?: number): Observable<Conversation> {
    return this.http.post<Conversation>(`${this.baseUrl}/api/conversations`, {
      title,
      knowledgeBaseId
    });
  }

  listMessages(conversationId: string): Observable<Message[]> {
    return this.http.get<Message[]>(`${this.baseUrl}/api/conversations/${conversationId}/messages`);
  }

  sendMessage(conversationId: string, content: string, metadata?: Record<string, unknown>): Observable<Message> {
    return this.http.post<Message>(`${this.baseUrl}/api/conversations/${conversationId}/messages`, {
      content,
      metadata
    });
  }
}
