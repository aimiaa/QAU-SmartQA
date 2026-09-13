export interface ChatSession {
  id: number;
  sessionId?: string;
  title: string;
  scope: string;
  messageCount: number;
  updatedAt: string;
  pinned?: boolean;
}

export interface ChatMessage {
  id: number;
  role: 'user' | 'assistant';
  content: string;
  time: string;
  sources?: string[];
}

export interface QuickQuestion {
  id: number;
  label: string;
  question: string;
}
