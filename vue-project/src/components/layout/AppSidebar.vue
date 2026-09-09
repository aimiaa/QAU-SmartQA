<script setup lang="ts">
import { ChevronRight, LogOut, Moon, Sparkles, Sun, X } from 'lucide-vue-next';
import KnowledgePanel from '../knowledge/KnowledgePanel.vue';
import type { KnowledgeBase, NavGroup } from '../../types';

defineProps<{
  groups: NavGroup[];
  activeId: string;
  open: boolean;
  dark: boolean;
  knowledgeBases: KnowledgeBase[];
  selectedIds: number[];
}>();

defineEmits<{
  select: [id: string];
  'toggle-knowledge-base': [id: number];
  'toggle-theme': [];
  logout: [];
  close: [];
}>();
</script>

<template>
  <aside class="sidebar" :class="{ 'is-open': open }">
    <div class="sidebar-logo">
      <div class="brand-mark">
        <Sparkles :size="20" />
      </div>
      <div class="brand-copy">
        <span class="brand-kicker">青农 AI 助手</span>
        <strong>QAU SmartAI</strong>
        <span>校园知识服务台</span>
      </div>
      <button class="icon-button sidebar-close" type="button" aria-label="关闭导航" @click="$emit('close')">
        <X :size="18" />
      </button>
    </div>

    <button class="sidebar-theme" type="button" @click="$emit('toggle-theme')">
      <span class="theme-icon">
        <Sun v-if="dark" :size="17" />
        <Moon v-else :size="17" />
      </span>
      <span>{{ dark ? '切换浅色模式' : '切换深色模式' }}</span>
    </button>

    <nav class="sidebar-nav" aria-label="主导航">
      <section v-for="group in groups" :key="group.id" class="nav-group">
        <p>{{ group.title }}</p>
        <template v-for="item in group.items" :key="item.id">
          <button
            class="nav-item"
            :class="{ active: item.id === activeId }"
            type="button"
            :aria-current="item.id === activeId ? 'page' : undefined"
            @click="$emit('select', item.id)"
          >
            <span class="nav-icon"><component :is="item.icon" :size="20" /></span>
            <span class="nav-text">
              <strong>{{ item.label }}</strong>
              <small>{{ item.description }}</small>
            </span>
            <ChevronRight v-if="item.id === activeId" class="nav-arrow" :size="17" />
          </button>

          <KnowledgePanel
            v-if="item.id === 'kb' && item.id === activeId"
            class="sidebar-knowledge-panel"
            :knowledge-bases="knowledgeBases"
            :selected-ids="selectedIds"
            @toggle="$emit('toggle-knowledge-base', $event)"
          />
        </template>
      </section>
    </nav>

    <div class="sidebar-footer">
      <span>Campus Knowledge Hub</span>
      <small>知识库、流程与问答一体化</small>
      <div class="footer-status"><i aria-hidden="true"></i>系统运行中</div>
      <button class="sidebar-logout" type="button" @click="$emit('logout')">
        <LogOut :size="16" />
        <span>退出登录</span>
      </button>
    </div>
  </aside>
</template>
