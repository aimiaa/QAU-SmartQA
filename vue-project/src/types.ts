import type { Component } from 'vue';

export interface NavItem {
  id: string;
  label: string;
  description: string;
  icon: Component;
}

export interface NavGroup {
  id: string;
  title: string;
  items: NavItem[];
}

export interface KnowledgeBase {
  id: number;
  name: string;
  category: string;
  status: 'ready' | 'syncing' | 'review';
  documents: number;
  updatedAt: string;
  description: string;
}

export interface ChatSession {
  id: number;
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
