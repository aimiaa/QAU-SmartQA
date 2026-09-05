export const API_ENDPOINTS = {
  // 获取左侧导航分组与菜单项；后端提供真实路径后替换这里的占位值。
  navGroups: '__NAV_GROUPS__',

  // 获取系统概览指标，例如知识库数量、文档总数、可用知识库数量。
  metricsOverview: '__METRICS_OVERVIEW__',

  // 获取知识库列表，用于右侧“选择知识库”面板。
  knowledgeBases: '__KNOWLEDGE_BASES__',

  // 保存当前用户选中的知识库范围，用于多知识库联合检索。
  selectedKnowledgeBases: '__SELECTED_KNOWLEDGE_BASES__',

  // 触发指定知识库同步或重新向量化。
  syncKnowledgeBase: '__SYNC_KNOWLEDGE_BASE__',

  // 获取对话历史列表，用于左侧“对话历史”面板。
  chatSessions: '__CHAT_SESSIONS__',

  // 获取指定会话下的消息明细。
  chatMessages: '__CHAT_MESSAGES__',

  // 创建新的问答会话。
  createChatSession: '__CREATE_CHAT_SESSION__',

  // 向 AI 问答服务提交问题，后端负责检索知识库并生成回答。
  sendChatMessage: '__SEND_CHAT_MESSAGE__',

  // 获取快捷问题配置，用于输入框上方的快捷按钮。
  quickQuestions: '__QUICK_QUESTIONS__',

  // 全局搜索政策、流程、通知、知识库文档等内容。
  globalSearch: '__GLOBAL_SEARCH__',
} as const;
