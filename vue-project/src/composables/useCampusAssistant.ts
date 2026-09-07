import { computed, onMounted, ref } from 'vue';
import { chatApi } from '../api/chat';
import {
  chatSessions as fallbackChatSessions,
  initialMessages as fallbackMessages,
  knowledgeBases,
  navGroups,
  quickQuestions as fallbackQuickQuestions,
} from '../constants/mockData';
import type { ChatMessage, ChatSession, QuickQuestion } from '../types';
import { formatNow } from '../utils/date';

const getErrorMessage = (error: unknown) =>
  error instanceof Error ? error.message : '请求失败，请稍后重试';

export const useCampusAssistant = () => {
  const activeNav = ref('assistant');
  const isDark = ref(document.documentElement.classList.contains('dark'));
  const sidebarOpen = ref(false);
  const selectedKbIds = ref<number[]>(
    knowledgeBases.filter((item) => item.status === 'ready').map((item) => item.id),
  );
  const activeSessionId = ref(1);
  const input = ref('');
  const isAnswering = ref(false);
  const chatSessions = ref<ChatSession[]>([...fallbackChatSessions]);
  const messages = ref<ChatMessage[]>([...fallbackMessages]);
  const quickQuestions = ref<QuickQuestion[]>([...fallbackQuickQuestions]);

  const selectedKnowledgeBases = computed(() =>
    knowledgeBases.filter((item) => selectedKbIds.value.includes(item.id)),
  );

  const activeSession = computed<ChatSession | undefined>(() =>
    chatSessions.value.find((session) => session.id === activeSessionId.value),
  );

  const readyCount = computed(() => knowledgeBases.filter((item) => item.status === 'ready').length);

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
    if (id !== 'kb') {
      sidebarOpen.value = false;
    }
  };

  const toggleKnowledgeBase = (id: number) => {
    selectedKbIds.value = selectedKbIds.value.includes(id)
      ? selectedKbIds.value.filter((item) => item !== id)
      : [...selectedKbIds.value, id];
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
  });

  return {
    activeNav,
    activeSession,
    activeSessionId,
    askQuickQuestion,
    chatSessions,
    createSession,
    input,
    isAnswering,
    isDark,
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
