<script setup lang="ts">
import { computed, ref } from 'vue';
import { Bot, Menu, Moon, Search, Sparkles, Sun, X } from 'lucide-vue-next';
import AppSidebar from './components/AppSidebar.vue';
import ChatWorkspace from './components/ChatWorkspace.vue';
import KnowledgePanel from './components/KnowledgePanel.vue';
import MetricCard from './components/MetricCard.vue';
import { chatSessions, initialMessages, knowledgeBases, navGroups, quickQuestions } from './data';
import type { ChatMessage, ChatSession } from './types';

const activeNav = ref('assistant');
const isDark = ref(document.documentElement.classList.contains('dark'));
const sidebarOpen = ref(false);
const selectedKbIds = ref<number[]>([1, 2, 3]);
const activeSessionId = ref(1);
const input = ref('');
const isAnswering = ref(false);
const messages = ref<ChatMessage[]>([...initialMessages]);

const selectedKnowledgeBases = computed(() =>
  knowledgeBases.filter((item) => selectedKbIds.value.includes(item.id)),
);

const activeSession = computed<ChatSession | undefined>(() =>
  chatSessions.find((session) => session.id === activeSessionId.value),
);

const readyCount = computed(() => knowledgeBases.filter((item) => item.status === 'ready').length);

const toggleTheme = () => {
  isDark.value = !isDark.value;
  document.documentElement.classList.toggle('dark', isDark.value);
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light');
};

const selectNavigation = (id: string) => {
  activeNav.value = id;
  sidebarOpen.value = false;
};

const toggleKnowledgeBase = (id: number) => {
  selectedKbIds.value = selectedKbIds.value.includes(id)
    ? selectedKbIds.value.filter((item) => item !== id)
    : [...selectedKbIds.value, id];
};

const selectSession = (id: number) => {
  activeSessionId.value = id;
  const session = chatSessions.find((item) => item.id === id);
  messages.value = [
    {
      id: Date.now(),
      role: 'assistant',
      content: session
        ? `已切换到「${session.title}」。我会优先基于「${session.scope}」进行检索回答。`
        : '已开启新的问答会话。',
      time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
      sources: session ? [session.scope] : [],
    },
  ];
};

const createAnswer = (question: string) => {
  const sourceNames = selectedKnowledgeBases.value.map((item) => item.name);
  const sourceText = sourceNames.length ? sourceNames.join('、') : '当前公开知识库';

  return `已根据「${sourceText}」检索到相关材料。针对「${question}」，建议先确认所属学院或职能部门要求，再按线上系统提交申请；如果涉及证明材料，请保留原件并上传清晰扫描件。`;
};

const submitQuestion = () => {
  const question = input.value.trim();
  if (!question || isAnswering.value) return;

  const now = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  messages.value.push({ id: Date.now(), role: 'user', content: question, time: now });
  input.value = '';
  isAnswering.value = true;

  window.setTimeout(() => {
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: createAnswer(question),
      time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
      sources: selectedKnowledgeBases.value.slice(0, 3).map((item) => item.name),
    });
    isAnswering.value = false;
  }, 520);
};

const askQuickQuestion = (question: string) => {
  input.value = question;
  submitQuestion();
};
</script>

<template>
  <div class="app-shell">
    <AppSidebar
      :groups="navGroups"
      :active-id="activeNav"
      :open="sidebarOpen"
      :dark="isDark"
      @select="selectNavigation"
      @toggle-theme="toggleTheme"
      @close="sidebarOpen = false"
    />

    <main class="main-area">
      <header class="topbar">
        <button class="icon-button mobile-only" type="button" aria-label="打开导航" @click="sidebarOpen = true">
          <Menu :size="20" />
        </button>
        <div class="page-title">
          <div class="title-icon">
            <Sparkles :size="22" />
          </div>
          <div>
            <p class="eyebrow">QAU AI CAMPUS ASSISTANT</p>
            <h1>青岛农业大学智能问答系统</h1>
          </div>
        </div>

        <div class="topbar-actions">
          <label class="search-box" aria-label="全局搜索">
            <Search :size="18" />
            <input type="search" placeholder="搜索政策、流程、通知" />
          </label>
          <button class="theme-button" type="button" @click="toggleTheme">
            <Sun v-if="isDark" :size="18" />
            <Moon v-else :size="18" />
            <span>{{ isDark ? '浅色模式' : '深色模式' }}</span>
          </button>
        </div>
      </header>

      <section class="hero-band" aria-label="系统概览">
        <div class="hero-copy">
          <div class="hero-badge"><Bot :size="16" />校内知识库实时检索</div>
          <h2>把教务、科研、后勤和招生就业问题集中到一个 AI 问答入口。</h2>
          <p>面向学生、教师和管理人员，支持多知识库联合检索、来源追踪、连续追问和办事建议。</p>
        </div>
        <div class="metric-grid">
          <MetricCard label="已接入知识库" :value="knowledgeBases.length.toString()" detail="覆盖教务、科研、服务" tone="primary" />
          <MetricCard label="可检索文档" value="336" detail="制度、指南、通知" tone="green" />
          <MetricCard label="可用知识库" :value="readyCount.toString()" detail="当前可参与回答" tone="orange" />
        </div>
      </section>

      <section class="workspace-grid" aria-label="智能问答工作台">
        <ChatWorkspace
          :sessions="chatSessions"
          :active-session-id="activeSessionId"
          :active-session="activeSession"
          :messages="messages"
          :quick-questions="quickQuestions"
          :selected-knowledge-bases="selectedKnowledgeBases"
          :input="input"
          :answering="isAnswering"
          @select-session="selectSession"
          @update:input="input = $event"
          @submit="submitQuestion"
          @ask-quick="askQuickQuestion"
        />

        <KnowledgePanel
          :knowledge-bases="knowledgeBases"
          :selected-ids="selectedKbIds"
          @toggle="toggleKnowledgeBase"
        />
      </section>
    </main>

    <button v-if="sidebarOpen" class="scrim" type="button" aria-label="关闭导航" @click="sidebarOpen = false">
      <X :size="20" />
    </button>
  </div>
</template>
