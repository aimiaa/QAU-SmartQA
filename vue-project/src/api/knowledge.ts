import { API_ENDPOINTS, LONG_REQUEST_TIMEOUT } from './endpoints';
import { http } from './request';
import type { KnowledgeBase, KnowledgeBaseDraft, KnowledgeDocument } from '../types';

export interface KnowledgeBaseListParams {
  keyword?: string;
  status?: KnowledgeBase['status'];
  category?: string;
}

export interface UpdateSelectedKnowledgeBasesPayload {
  ids: number[];
}

export interface SyncKnowledgeBasePayload {
  id: number;
}

export interface UploadDocumentProgress {
  loaded: number;
  total?: number;
  progress: number;
}

export type CreateKnowledgeBasePayload = KnowledgeBaseDraft;

export type UpdateKnowledgeBasePayload = Partial<KnowledgeBaseDraft>;

export const knowledgeApi = {
  /**
   * 获取知识库列表。
   * 作用：给 KnowledgePanel 提供知识库名称、分类、状态、文档数、更新时间和描述。
   */
  async getKnowledgeBases(params?: KnowledgeBaseListParams): Promise<KnowledgeBase[]> {
    return http.get<KnowledgeBase[]>(API_ENDPOINTS.knowledgeBases, { params });
  },

  /**
   * 新建 RAG 知识库。
   * 作用：只创建知识库基础信息；文档通过上传接口单独加入。
   */
  async createKnowledgeBase(payload: CreateKnowledgeBasePayload): Promise<KnowledgeBase> {
    return http.post<KnowledgeBase, CreateKnowledgeBasePayload>(API_ENDPOINTS.createKnowledgeBase, payload);
  },

  /**
   * 更新知识库基本信息。
   * 作用：修改名称、分类或描述。
   */
  async updateKnowledgeBase(id: number, payload: UpdateKnowledgeBasePayload): Promise<KnowledgeBase> {
    return http.put<KnowledgeBase, UpdateKnowledgeBasePayload>(API_ENDPOINTS.updateKnowledgeBase(id), payload);
  },

  /**
   * 删除指定知识库。
   * 作用：知识库管理页面删除操作，后端需同时清理向量数据。
   */
  async deleteKnowledgeBase(id: number): Promise<void> {
    return http.delete<void>(API_ENDPOINTS.deleteKnowledgeBase(id));
  },

  /**
   * 保存当前用户选中的知识库。
   * 作用：让后端知道本次问答应该在哪些知识库范围内联合检索。
   */
  async updateSelectedKnowledgeBases(payload: UpdateSelectedKnowledgeBasesPayload): Promise<number[]> {
    return http.post<number[], UpdateSelectedKnowledgeBasesPayload>(API_ENDPOINTS.selectedKnowledgeBases, payload);
  },

  /**
   * 同步指定知识库。
   * 作用：用于知识库管理场景，触发文档重新解析、入库或向量化。
   */
  async syncKnowledgeBase(payload: SyncKnowledgeBasePayload): Promise<KnowledgeBase> {
    return http.post<KnowledgeBase>(API_ENDPOINTS.syncKnowledgeBase(payload.id), undefined, {
      timeout: LONG_REQUEST_TIMEOUT,
    });
  },

  /**
   * 向指定知识库上传文档。
   * 作用：后端保存原始文件后异步解析、切片和向量化。
   */
  async uploadDocument(
    knowledgeBaseId: number,
    file: File,
    onProgress?: (event: UploadDocumentProgress) => void,
  ): Promise<KnowledgeDocument> {
    const formData = new FormData();
    formData.append('file', file);

    return http.post<KnowledgeDocument, FormData>(
      API_ENDPOINTS.knowledgeDocuments(knowledgeBaseId),
      formData,
      {
        timeout: LONG_REQUEST_TIMEOUT,
        onUploadProgress: (event) => {
          if (!onProgress) return;

          onProgress({
            loaded: event.loaded,
            total: event.total,
            progress: event.total ? Math.min(100, Math.round((event.loaded / event.total) * 100)) : 0,
          });
        },
      },
    );
  },
};
