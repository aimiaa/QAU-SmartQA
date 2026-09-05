<script setup lang="ts">
import { AlertCircle, CheckCircle2, Clock3, Database, FileText, RefreshCw } from 'lucide-vue-next';
import type { KnowledgeBase } from '../types';

defineProps<{
  knowledgeBases: KnowledgeBase[];
  selectedIds: number[];
}>();

defineEmits<{
  toggle: [id: number];
}>();

const statusMeta = {
  ready: { text: '已完成', icon: CheckCircle2 },
  syncing: { text: '同步中', icon: RefreshCw },
  review: { text: '待复核', icon: AlertCircle },
};
</script>

<template>
  <aside class="knowledge-panel panel">
    <div class="panel-heading">
      <div>
        <h2>选择知识库</h2>
        <span>多选后联合检索</span>
      </div>
      <Database :size="20" />
    </div>

    <div class="kb-list">
      <button
        v-for="kb in knowledgeBases"
        :key="kb.id"
        class="kb-card"
        :class="{ active: selectedIds.includes(kb.id), syncing: kb.status === 'syncing' }"
        type="button"
        @click="$emit('toggle', kb.id)"
      >
        <div class="kb-card-top">
          <label class="check-row" @click.stop>
            <input
              type="checkbox"
              :checked="selectedIds.includes(kb.id)"
              @change="$emit('toggle', kb.id)"
            />
            <span>{{ kb.name }}</span>
          </label>
          <component :is="statusMeta[kb.status].icon" :size="17" class="status-icon" />
        </div>

        <p>{{ kb.description }}</p>

        <div class="kb-meta">
          <span><FileText :size="14" />{{ kb.documents }} 份</span>
          <span><Clock3 :size="14" />{{ kb.updatedAt }}</span>
        </div>
        <span class="kb-badge">{{ kb.category }} · {{ statusMeta[kb.status].text }}</span>
      </button>
    </div>
  </aside>
</template>
