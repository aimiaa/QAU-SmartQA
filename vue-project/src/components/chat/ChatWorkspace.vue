<script setup lang="ts">
import { FileText, Loader2, MessageSquare, Pin, Plus, Send, Sparkles, Trash2 } from 'lucide-vue-next';
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
  'delete-session': [id: number];
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
          <span class="panel-kicker">Conversation</span>
          <h2>对话历史</h2>
          <span>{{ sessions.length }} 个会话</span>
        </div>
        <button class="icon-button add-session-button" type="button" aria-label="新建对话" @click="$emit('create-session')">
          <Plus :size="18" />
        </button>
      </div>

      <div class="session-list">
        <div
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: session.id === activeSessionId }"
          role="button"
          tabindex="0"
          @click="$emit('select-session', session.id)"
          @keydown.enter="$emit('select-session', session.id)"
        >
          <span class="session-title">
            <span class="session-name">
              <Pin v-if="session.pinned" :size="14" />
              {{ session.title }}
            </span>
            <small>{{ session.scope }}</small>
          </span>
          <span class="session-meta">
            <span>{{ session.messageCount }} 条消息</span>
            <span>{{ session.updatedAt }}</span>
          </span>
          <button
            class="session-delete"
            type="button"
            aria-label="删除会话"
            @click.stop="$emit('delete-session', session.id)"
          >
            <Trash2 :size="15" />
          </button>
        </div>
      </div>
    </aside>

    <section class="chat-panel panel" aria-label="问答窗口">
      <div class="chat-header">
        <div class="chat-title-group">
          <span class="chat-status-dot" aria-hidden="true"></span>
          <div>
            <h2>{{ activeSession?.title ?? '新对话' }}</h2>
            <p>自动匹配校内政策、流程和通知知识</p>
          </div>
        </div>
        <div class="source-pills" aria-label="当前检索范围">
          <span class="route-pill">RAG 自动路由</span>
          <span>{{ selectedKnowledgeBases.length }} 个知识库已启用</span>
        </div>
      </div>

      <div class="quick-row" aria-label="快捷问题">
        <span class="quick-label"><Sparkles :size="15" />常用问题</span>
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
            <p v-if="message.content">{{ message.content }}</p>
            <p v-else class="message-empty">正在组织回答...</p>
            <div class="message-meta">
              <span>{{ message.time }}</span>
              <span v-if="message.sources?.length" class="message-sources">
                <span v-for="source in message.sources" :key="source" class="source-tag">
                  <FileText :size="13" />{{ source }}
                </span>
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
        <div class="composer-input">
          <textarea
            :value="input"
            autofocus
            rows="3"
            placeholder="输入你的问题，例如：如何申请缓考、奖学金材料有哪些、校园卡如何补办..."
            @input="emit('update:input', ($event.target as HTMLTextAreaElement).value)"
            @keydown="handleKeydown"
          />
        </div>
        <button class="send-button" type="button" :disabled="!input.trim() || answering" @click="emit('submit')">
          <Send :size="18" />
          <span>{{ answering ? '生成中' : '发送' }}</span>
        </button>
      </form>
    </section>
  </div>
</template>

<style scoped>
.session-item {
  position: relative;
}

.session-delete {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: inherit;
  opacity: 0;
  cursor: pointer;
  transition: opacity 0.15s ease, background 0.15s ease, color 0.15s ease;
}

.session-item:hover .session-delete,
.session-delete:focus-visible {
  opacity: 0.65;
}

.session-delete:hover {
  opacity: 1;
  background: rgba(239, 68, 68, 0.12);
  color: #ef4444;
}
</style>
