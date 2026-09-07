import { API_ENDPOINTS, LONG_REQUEST_TIMEOUT } from './endpoints';
import { http } from './request';
import type { ChatMessage, ChatSession, QuickQuestion } from '../types';
import { getClientSessionId } from '../utils/session';
import { getAuthToken } from '../utils/auth';

export interface ChatSessionListParams {
  keyword?: string;
  pinned?: boolean;
}

export interface CreateChatSessionPayload {
  title?: string;
  knowledgeBaseIds?: number[];
}

export interface SendChatMessagePayload {
  question: string;
}

export interface SendChatMessageResult {
  answer: string;
  sessionId: string;
}

export interface StreamChunk {
  type: 'chunk' | 'done';
  data: string;
}

export const chatApi = {
  /**
   * 获取对话历史列表。
   * 作用：替换当前 data.ts 中写死的 chatSessions，展示用户历史会话、消息数、更新时间和置顶状态。
   */
  async getSessions(params?: ChatSessionListParams): Promise<ChatSession[]> {
    return http.get<ChatSession[]>(API_ENDPOINTS.chatSessions, { params });
  },

  /**
   * 创建新对话。
   * 作用：点击“新建对话”时让后端生成一个新的会话记录。
   */
  async createSession(payload: CreateChatSessionPayload = {}): Promise<ChatSession> {
    return http.post<ChatSession, CreateChatSessionPayload>(API_ENDPOINTS.createChatSession, payload);
  },

  /**
   * 获取指定会话的消息记录。
   * 作用：用户切换历史会话时加载该会话下的完整聊天内容。
   */
  async getMessages(sessionId: string): Promise<ChatMessage[]> {
    return http.get<ChatMessage[]>(API_ENDPOINTS.chatMessages(sessionId));
  },

  /**
   * 提交用户问题并获取 AI 回复。
   * 作用：提交用户问题，并通过 X-Session-Id 请求头传递前端生成的业务会话标识。
   */
  async sendMessage(payload: SendChatMessagePayload, sessionId?: string): Promise<SendChatMessageResult> {
    return http.post<SendChatMessageResult, SendChatMessagePayload>(API_ENDPOINTS.sendChatMessage, payload, {
      headers: {
        'X-Session-Id': sessionId ?? getClientSessionId(),
      },
      timeout: LONG_REQUEST_TIMEOUT,
    });
  },

  /**
   * 流式提交用户问题，通过 SSE 逐字接收 AI 回复。
   */
  async *sendMessageStream(
    payload: SendChatMessagePayload,
    sessionId?: string,
  ): AsyncGenerator<StreamChunk> {
    const token = getAuthToken();
    const effectiveSessionId = sessionId ?? getClientSessionId();
    const baseURL = import.meta.env.VITE_API_BASE_URL ?? '';

    const response = await fetch(`${baseURL}${API_ENDPOINTS.sendChatMessageStream}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        'X-Session-Id': effectiveSessionId,
      },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      throw new Error(`请求失败: ${response.status}`);
    }

    const reader = response.body?.getReader();
    if (!reader) {
      throw new Error('无法读取响应流');
    }

    const decoder = new TextDecoder();
    let buffer = '';

    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() ?? '';

        let currentEvent = '';
        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim();
          } else if (line.startsWith('data:')) {
            const data = line.slice(5).trim();
            if (currentEvent === 'message') {
              yield { type: 'chunk', data };
            } else if (currentEvent === 'done') {
              yield { type: 'done', data };
            }
          }
        }
      }
    } finally {
      reader.releaseLock();
    }
  },

  /**
   * 获取快捷问题列表。
   * 作用：让后台可配置“缓考申请、转专业、奖助学金”等快捷入口。
   */
  async getQuickQuestions(): Promise<QuickQuestion[]> {
    return http.get<QuickQuestion[]>(API_ENDPOINTS.quickQuestions);
  },
};
