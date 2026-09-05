<script setup lang="ts">
import { ChevronRight, Moon, Sparkles, Sun, X } from 'lucide-vue-next';
import type { NavGroup } from '../types';

defineProps<{
  groups: NavGroup[];
  activeId: string;
  open: boolean;
  dark: boolean;
}>();

defineEmits<{
  select: [id: string];
  'toggle-theme': [];
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
        <strong>QAU AI</strong>
        <span>智能问答系统</span>
      </div>
      <button class="icon-button sidebar-close" type="button" aria-label="关闭导航" @click="$emit('close')">
        <X :size="18" />
      </button>
    </div>

    <button class="sidebar-theme" type="button" @click="$emit('toggle-theme')">
      <Sun v-if="dark" :size="17" />
      <Moon v-else :size="17" />
      <span>{{ dark ? '切换浅色模式' : '切换深色模式' }}</span>
    </button>

    <nav class="sidebar-nav" aria-label="主导航">
      <section v-for="group in groups" :key="group.id" class="nav-group">
        <p>{{ group.title }}</p>
        <button
          v-for="item in group.items"
          :key="item.id"
          class="nav-item"
          :class="{ active: item.id === activeId }"
          type="button"
          @click="$emit('select', item.id)"
        >
          <span class="nav-icon"><component :is="item.icon" :size="20" /></span>
          <span class="nav-text">
            <strong>{{ item.label }}</strong>
            <small>{{ item.description }}</small>
          </span>
          <ChevronRight v-if="item.id === activeId" class="nav-arrow" :size="17" />
        </button>
      </section>
    </nav>

    <div class="sidebar-footer">
      <span>AI Campus v1.0</span>
      <small>Powered by Vue</small>
    </div>
  </aside>
</template>
