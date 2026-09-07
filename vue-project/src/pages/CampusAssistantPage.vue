<script setup lang="ts">
import { Bot, Menu, Moon, Search, Sparkles, Sun, X } from 'lucide-vue-next';
import ChatWorkspace from '../components/chat/ChatWorkspace.vue';
import AppSidebar from '../components/layout/AppSidebar.vue';
import MetricCard from '../components/metrics/MetricCard.vue';
import { useAuth } from '../composables/useAuth';
import { useCampusAssistant } from '../composables/useCampusAssistant';

const {
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
  selectSession,
  sidebarOpen,
  submitQuestion,
  toggleKnowledgeBase,
  toggleTheme,
} = useCampusAssistant();

const { logout } = useAuth();

const handleLogout = async () => {
  await logout();
};
</script>

<template>
  <div class="app-shell">
    <AppSidebar
      :groups="navGroups"
      :active-id="activeNav"
      :open="sidebarOpen"
      :dark="isDark"
      :knowledge-bases="knowledgeBases"
      :selected-ids="selectedKbIds"
      @select="selectNavigation"
      @toggle-knowledge-base="toggleKnowledgeBase"
      @toggle-theme="toggleTheme"
      @logout="handleLogout"
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
          @create-session="createSession"
          @update:input="input = $event"
          @submit="submitQuestion"
          @ask-quick="askQuickQuestion"
        />
      </section>

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
    </main>

    <button v-if="sidebarOpen" class="scrim" type="button" aria-label="关闭导航" @click="sidebarOpen = false">
      <X :size="20" />
    </button>
  </div>
</template>
