import { API_ENDPOINTS } from './endpoints';
import { http } from './http';
import type { ChatMessage, ChatSession, QuickQuestion } from '../types';

export interface ChatSessionListParams {
  keyword?: string;
  pinned?: boolean;
}

export interface CreateChatSessionPayload {
  title?: string;
  knowledgeBaseIds?: number[];
}

export interface ChatMessagesParams {
  sessionId: number;
}

export interface SendChatMessagePayload {
  question: string;
  sessionId?: number;
  knowledgeBaseIds: number[];
}

export interface SendChatMessageResult {
  session: ChatSession;
  answer: ChatMessage;
}

export const chatApi = {
  /**
   * 获取对话历史列表。
   * 作用：替换当前 data.ts 中写死的 chatSessions，展示用户历史会话、消息数、更新时间和置顶状态。
   */
  getSessions(params?: ChatSessionListParams) {
    return http.get<ChatSession[]>(API_ENDPOINTS.chatSessions, { params });
  },

  /**
   * 创建新对话。
   * 作用：点击“新建对话”时让后端生成一个新的会话记录。
   */
  createSession(payload: CreateChatSessionPayload = {}) {
    return http.post<ChatSession, CreateChatSessionPayload>(API_ENDPOINTS.createChatSession, payload);
  },

  /**
   * 获取指定会话的消息记录。
   * 作用：用户切换历史会话时加载该会话下的完整聊天内容。
   */
  getMessages(params: ChatMessagesParams) {
    return http.get<ChatMessage[]>(API_ENDPOINTS.chatMessages, { params });
  },

  /**
   * 提交用户问题并获取 AI 回复。
   * 作用：替换 App.vue 中 setTimeout 模拟回答的逻辑，由后端完成知识库检索、模型调用和来源返回。
   */
  sendMessage(payload: SendChatMessagePayload) {
    return http.post<SendChatMessageResult, SendChatMessagePayload>(API_ENDPOINTS.sendChatMessage, payload);
  },

  /**
   * 获取快捷问题列表。
   * 作用：让后台可配置“缓考申请、转专业、奖助学金”等快捷入口。
   */
  getQuickQuestions() {
    return http.get<QuickQuestion[]>(API_ENDPOINTS.quickQuestions);
  },
};
