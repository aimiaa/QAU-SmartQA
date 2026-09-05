import { API_ENDPOINTS } from './endpoints';
import { http } from './http';

export interface GlobalSearchParams {
  keyword: string;
  knowledgeBaseIds?: number[];
}

export interface GlobalSearchResult {
  id: number | string;
  title: string;
  summary: string;
  source: string;
  updatedAt?: string;
}

export const searchApi = {
  /**
   * 全局搜索。
   * 作用：给顶部搜索框提供真实检索能力，搜索政策、流程、通知和知识库文档。
   */
  globalSearch(params: GlobalSearchParams) {
    return http.get<GlobalSearchResult[]>(API_ENDPOINTS.globalSearch, { params });
  },
};
