import { API_ENDPOINTS } from './endpoints';
import { http } from './http';
import type { KnowledgeBase } from '../types';

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

export const knowledgeApi = {
  /**
   * 获取知识库列表。
   * 作用：给 KnowledgePanel 提供知识库名称、分类、状态、文档数、更新时间和描述。
   */
  getKnowledgeBases(params?: KnowledgeBaseListParams) {
    return http.get<KnowledgeBase[]>(API_ENDPOINTS.knowledgeBases, { params });
  },

  /**
   * 保存当前用户选中的知识库。
   * 作用：让后端知道本次问答应该在哪些知识库范围内联合检索。
   */
  updateSelectedKnowledgeBases(payload: UpdateSelectedKnowledgeBasesPayload) {
    return http.post<number[], UpdateSelectedKnowledgeBasesPayload>(API_ENDPOINTS.selectedKnowledgeBases, payload);
  },

  /**
   * 同步指定知识库。
   * 作用：用于知识库管理场景，触发文档重新解析、入库或向量化。
   */
  syncKnowledgeBase(payload: SyncKnowledgeBasePayload) {
    return http.post<KnowledgeBase, SyncKnowledgeBasePayload>(API_ENDPOINTS.syncKnowledgeBase, payload);
  },
};
