export type MessageRole = 'USER' | 'ASSISTANT';

export interface Message {
  id?: string;
  content: string;
  role: MessageRole;
  createdAt?: string;
  streaming?: boolean;
}
