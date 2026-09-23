import { computed, onMounted, ref } from 'vue';
import { chatApi } from '../api/chat';
import {
  chatSessions as fallbackChatSessions,
  initialMessages as fallbackMessages,
  navGroups,
  quickQuestions as fallbackQuickQuestions,
} from '../constants/mockData';
import type { ChatMessage, ChatSession, QuickQuestion } from '../types';
import { formatNow } from '../utils/date';
import { useKnowledgeBases } from './useKnowledgeBases';

const getErrorMessage = (error: unknown) =>
  error instanceof Error ? error.message : '请求失败，请稍后重试';

export const useCampusAssistant = () => {
  const {
    documentCount,
    knowledgeBases,
    loadKnowledgeBases,
    readyCount,
    selectedKbIds,
    selectedKnowledgeBases,
    toggleKnowledgeBase,
  } = useKnowledgeBases();

  const activeNav = ref('assistant');
  const isDark = ref(document.documentElement.classList.contains('dark'));
  const sidebarOpen = ref(false);
  const activeSessionId = ref(1);
  const input = ref('');
  const isAnswering = ref(false);
  const chatSessions = ref<ChatSession[]>([...fallbackChatSessions]);
  const messages = ref<ChatMessage[]>([...fallbackMessages]);
  const quickQuestions = ref<QuickQuestion[]>([...fallbackQuickQuestions]);

  const activeSession = computed<ChatSession | undefined>(() =>
    chatSessions.value.find((session) => session.id === activeSessionId.value),
  );

  // 知识库管理是独立页面，其余菜单仍在问答工作台内展示。
  const isKnowledgePage = computed(() => activeNav.value === 'kb');

  const mapSession = (session: ChatSession): ChatSession => ({
    ...session,
    updatedAt: session.updatedAt
      ? new Date(session.updatedAt).toLocaleString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      })
      : '刚刚',
  });

  const toggleTheme = () => {
    isDark.value = !isDark.value;
    document.documentElement.classList.toggle('dark', isDark.value);
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light');
  };

  const selectNavigation = (id: string) => {
    activeNav.value = id;
    sidebarOpen.value = false;
  };

  const backToAssistant = () => {
    activeNav.value = 'assistant';
  };

  const upsertSession = (session: ChatSession) => {
    const index = chatSessions.value.findIndex((item) => item.id === session.id);

    if (index >= 0) {
      chatSessions.value.splice(index, 1, session);
    } else {
      chatSessions.value.unshift(session);
    }

    activeSessionId.value = session.id;
  };

  const loadSessions = async () => {
    try {
      const sessions = await chatApi.getSessions();
      if (!sessions.length) return;

      chatSessions.value = sessions.map(mapSession);
      activeSessionId.value = sessions[0].id;

      if (sessions[0].sessionId) {
        const history = await chatApi.getMessages(sessions[0].sessionId);
        if (history.length) {
          messages.value = history;
        }
      }
    } catch {
      // 后端不可用时保留本地演示数据。
    }
  };

  const loadSessionMessages = async (session: ChatSession) => {
    if (!session.sessionId) return;

    try {
      const history = await chatApi.getMessages(session.sessionId);
      messages.value = history;
    } catch {
      messages.value = [];
    }
  };

  const selectSessionAndLoad = async (id: number) => {
    activeSessionId.value = id;
    const session = chatSessions.value.find((item) => item.id === id);
    if (session) {
      await loadSessionMessages(session);
    }
  };

  const deleteSession = async (id: number) => {
    const session = chatSessions.value.find((item) => item.id === id);

    if (session?.sessionId) {
      try {
        await chatApi.deleteSession(session.sessionId);
      } catch {
        // 后端不可用时仍从本地列表移除，保证交互连续。
      }
    }

    const index = chatSessions.value.findIndex((item) => item.id === id);
    if (index >= 0) {
      chatSessions.value.splice(index, 1);
    }

    if (activeSessionId.value !== id) return;

    const next = chatSessions.value[0];
    if (next) {
      activeSessionId.value = next.id;
      await loadSessionMessages(next);
    } else {
      activeSessionId.value = 0;
      messages.value = [
        {
          id: Date.now(),
          role: 'assistant',
          content: '已删除当前会话，点击左上角新建对话继续提问。',
          time: formatNow(),
        },
      ];
    }
  };

  const createSession = async () => {
    try {
      const session = await chatApi.createSession({
        title: '新对话',
        knowledgeBaseIds: selectedKbIds.value,
      });
      upsertSession(mapSession(session));
    } catch {
      const session: ChatSession = {
        id: Date.now(),
        title: '新对话',
        scope: selectedKnowledgeBases.value.map((item) => item.name).join('、') || '未选择知识库',
        messageCount: 0,
        updatedAt: '刚刚',
      };
      upsertSession(session);
    }

    messages.value = [
      {
        id: Date.now(),
        role: 'assistant',
        content: '已开启新的问答会话，请输入你想咨询的问题。',
        time: formatNow(),
      },
    ];
  };

  const submitQuestion = async () => {
    const question = input.value.trim();
    if (!question || isAnswering.value) return;

    messages.value.push({ id: Date.now(), role: 'user', content: question, time: formatNow() });
    input.value = '';
    isAnswering.value = true;

    const assistantMsgId = Date.now() + 1;
    messages.value.push({
      id: assistantMsgId,
      role: 'assistant',
      content: '',
      time: formatNow(),
    });

    try {
      let fullAnswer = '';
      const stream = chatApi.sendMessageStream(
        { question },
        activeSession.value?.sessionId,
      );

      for await (const chunk of stream) {
        if (chunk.type === 'chunk') {
          fullAnswer += chunk.data;
          const msg = messages.value.find((m) => m.id === assistantMsgId);
          if (msg) {
            msg.content = fullAnswer;
          }
        } else if (chunk.type === 'sources') {
          try {
            const parsed = JSON.parse(chunk.data) as string[];
            const msg = messages.value.find((m) => m.id === assistantMsgId);
            if (msg && parsed.length) {
              msg.sources = parsed;
            }
          } catch {
            // 来源解析失败不影响正文展示。
          }
        }
      }

      const currentSession = activeSession.value;
      if (currentSession) {
        upsertSession({
          ...currentSession,
          messageCount: currentSession.messageCount + 2,
          updatedAt: '刚刚',
        });
      }
    } catch (error) {
      const msg = messages.value.find((m) => m.id === assistantMsgId);
      if (msg) {
        msg.content = getErrorMessage(error);
      }
    } finally {
      isAnswering.value = false;
    }
  };

  const askQuickQuestion = (question: string) => {
    input.value = question;
    void submitQuestion();
  };

  onMounted(() => {
    void loadSessions();
    void loadKnowledgeBases();
  });

  return {
    activeNav,
    activeSession,
    activeSessionId,
    askQuickQuestion,
    backToAssistant,
    chatSessions,
    createSession,
    deleteSession,
    documentCount,
    input,
    isAnswering,
    isDark,
    isKnowledgePage,
    knowledgeBases,
    messages,
    navGroups,
    quickQuestions,
    readyCount,
    selectedKbIds,
    selectedKnowledgeBases,
    selectNavigation,
    selectSession: selectSessionAndLoad,
    sidebarOpen,
    submitQuestion,
    toggleKnowledgeBase,
    toggleTheme,
  };
};
