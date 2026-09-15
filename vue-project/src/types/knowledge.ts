export type KnowledgeBaseStatus = 'building' | 'ready' | 'syncing' | 'review' | 'failed';

export interface KnowledgeBase {
  id: number;
  name: string;
  category: string;
  status: KnowledgeBaseStatus;
  documents: number;
  updatedAt: string;
  description: string;
}

/** 新建知识库时由用户填写的信息；状态由文档上传与解析流程自动维护。 */
export interface KnowledgeBaseDraft {
  name: string;
  category: string;
  description: string;
}

export type KnowledgeDocumentStatus = 'uploaded' | 'parsing' | 'ready' | 'failed';

export interface KnowledgeDocument {
  id: number;
  knowledgeBaseId: number;
  title: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  status: KnowledgeDocumentStatus;
  uploadedAt: string;
  parsedAt?: string;
  errorMessage?: string;
}

export type UploadQueueStatus = 'waiting' | 'uploading' | 'success' | 'error';

export interface UploadQueueItem {
  uid: string;
  file: File;
  status: UploadQueueStatus;
  progress: number;
  errorMessage?: string;
  documentId?: number;
}
