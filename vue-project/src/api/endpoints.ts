export const API_ENDPOINTS = {
  // 后台登录。
  adminUserLogin: '/admin/user/login',

  // 退出登录。
  adminUserLogout: '/admin/user/logout',

  // 获取左侧导航分组与菜单项；后端提供真实路径后替换这里的占位值。
  navGroups: '/api/nav/groups',

  // 获取系统概览指标，例如知识库数量、文档总数、可用知识库数量。
  metricsOverview: '/api/metrics/overview',

  // 获取知识库列表，用于右侧“选择知识库”面板。
  knowledgeBases: '/api/knowledge-bases',

  // 保存当前用户选中的知识库范围，用于多知识库联合检索。
  selectedKnowledgeBases: '/api/knowledge-bases/selected',

  // 触发指定知识库同步或重新向量化。
  syncKnowledgeBase: (id: number | string) => `/api/knowledge-bases/${id}/sync`,

  // 获取对话历史列表，用于左侧“对话历史”面板。
  chatSessions: '/api/chat/sessions',

  // 获取指定会话下的消息明细。
  chatMessages: (sessionId: number | string) => `/api/chat/sessions/${sessionId}/messages`,

  // 创建新的问答会话。
  createChatSession: '/api/chat/sessions',

  // 向 AI 问答服务提交问题，后端负责检索知识库并生成回答。
  sendChatMessage: '/api/chat/message',

  // 向 AI 问答服务提交问题（流式输出 SSE）。
  sendChatMessageStream: '/api/chat/message/stream',

  // 获取快捷问题配置，用于输入框上方的快捷按钮。
  quickQuestions: '/api/quick-questions',

  // 全局搜索政策、流程、通知、知识库文档等内容。
  globalSearch: '/api/search',
} as const;

export const LONG_REQUEST_TIMEOUT = 180_000;
