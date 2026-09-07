import { API_ENDPOINTS } from './endpoints';
import { http } from './request';

export interface MetricsOverview {
  knowledgeBaseCount: number;
  documentCount: number;
  readyKnowledgeBaseCount: number;
}

export const metricsApi = {
  /**
   * 获取首页概览指标。
   * 作用：替换页面顶部指标卡片中的静态数字，例如已接入知识库、可检索文档、可用知识库。
   */
  async getOverview(): Promise<MetricsOverview> {
    return http.get<MetricsOverview>(API_ENDPOINTS.metricsOverview);
  },
};
