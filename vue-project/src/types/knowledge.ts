export interface KnowledgeBase {
  id: number;
  name: string;
  category: string;
  status: 'ready' | 'syncing' | 'review';
  documents: number;
  updatedAt: string;
  description: string;
}
