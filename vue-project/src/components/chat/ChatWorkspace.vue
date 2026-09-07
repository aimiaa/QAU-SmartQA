<script setup lang="ts">
import { FileText, Loader2, MessageSquare, Pin, Plus, Send } from 'lucide-vue-next';
import type { ChatMessage, ChatSession, KnowledgeBase, QuickQuestion } from '../../types';

defineProps<{
  sessions: ChatSession[];
  activeSessionId: number;
  activeSession?: ChatSession;
  messages: ChatMessage[];
  quickQuestions: QuickQuestion[];
  selectedKnowledgeBases: KnowledgeBase[];
  input: string;
  answering: boolean;
}>();

const emit = defineEmits<{
  'select-session': [id: number];
  'create-session': [];
  'update:input': [value: string];
  submit: [];
  'ask-quick': [question: string];
}>();

const handleKeydown = (event: KeyboardEvent) => {
  if (event.isComposing) return;

  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    emit('submit');
  }
};
</script>

<template>
  <div class="chat-layout">
    <aside class="history-panel panel">
      <div class="panel-heading">
        <div>
          <h2>对话历史</h2>
          <span>{{ sessions.length }} 个会话</span>
        </div>
        <button class="icon-button" type="button" aria-label="新建对话" @click="$emit('create-session')">
          <Plus :size="18" />
        </button>
      </div>

      <div class="session-list">
        <button
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: session.id === activeSessionId }"
          type="button"
          @click="$emit('select-session', session.id)"
        >
          <span class="session-title">
            <Pin v-if="session.pinned" :size="14" />
            {{ session.title }}
          </span>
          <small>{{ session.messageCount }} 条消息 · {{ session.updatedAt }}</small>
        </button>
      </div>
    </aside>

    <section class="chat-panel panel" aria-label="问答窗口">
      <div class="chat-header">
        <div>
          <h2>{{ activeSession?.title ?? '新对话' }}</h2>
          <p>自动匹配校内政策、流程和通知知识</p>
        </div>
        <div class="source-pills" aria-label="当前检索范围">
          <span>RAG 自动路由</span>
          <span>{{ selectedKnowledgeBases.length }} 个知识库已启用</span>
        </div>
      </div>

      <div class="quick-row" aria-label="快捷问题">
        <button
          v-for="item in quickQuestions"
          :key="item.id"
          type="button"
          @click="$emit('ask-quick', item.question)"
        >
          {{ item.label }}
        </button>
      </div>

      <div class="message-list" aria-live="polite">
        <article
          v-for="message in messages"
          :key="message.id"
          class="message-bubble"
          :class="message.role"
        >
          <div class="message-avatar">
            <MessageSquare v-if="message.role === 'assistant'" :size="17" />
            <span v-else>我</span>
          </div>
          <div class="message-content">
            <p>{{ message.content }}</p>
            <div class="message-meta">
              <span>{{ message.time }}</span>
              <span v-if="message.sources?.length" class="message-sources">
                <FileText :size="14" />{{ message.sources.join('、') }}
              </span>
            </div>
          </div>
        </article>
        <article v-if="answering" class="message-bubble assistant pending">
          <div class="message-avatar"><MessageSquare :size="17" /></div>
          <div class="message-content typing">
            <Loader2 :size="17" />正在检索校内知识库...
          </div>
        </article>
      </div>

      <form class="composer" @submit.prevent="emit('submit')">
        <textarea
          :value="input"
          autofocus
          rows="3"
          placeholder="输入你的问题，例如：如何申请缓考、奖学金材料有哪些、校园卡如何补办..."
          @input="emit('update:input', ($event.target as HTMLTextAreaElement).value)"
          @keydown="handleKeydown"
        />
        <button class="send-button" type="button" :disabled="!input.trim() || answering" @click="emit('submit')">
          <Send :size="18" />
          <span>{{ answering ? '生成中' : '发送' }}</span>
        </button>
      </form>
    </section>
  </div>
</template>
