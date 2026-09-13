<script setup lang="ts">
import { AlertCircle, CheckCircle2, Clock3, Database, FileText, RefreshCw } from 'lucide-vue-next';
import type { KnowledgeBase } from '../../types';

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
  <section class="knowledge-panel panel">
    <div class="panel-heading">
      <div>
        <span class="panel-kicker">Knowledge</span>
        <h2>RAG 知识库</h2>
        <span>在知识库管理中配置检索范围</span>
      </div>
      <div class="panel-icon"><Database :size="20" /></div>
    </div>

    <div class="kb-list">
      <button
        v-for="kb in knowledgeBases"
        :key="kb.id"
        class="kb-card"
        :class="{ active: selectedIds.includes(kb.id), syncing: kb.status === 'syncing' }"
        type="button"
        :aria-pressed="selectedIds.includes(kb.id)"
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
          <span class="status-badge" :class="`status-${kb.status}`">
            <component :is="statusMeta[kb.status].icon" :size="15" class="status-icon" />
            {{ statusMeta[kb.status].text }}
          </span>
        </div>

        <p>{{ kb.description }}</p>

        <div class="kb-meta">
          <span><FileText :size="14" />{{ kb.documents }} 份</span>
          <span><Clock3 :size="14" />{{ kb.updatedAt }}</span>
        </div>
        <span class="kb-badge">{{ kb.category }}</span>
      </button>
    </div>
  </section>
</template>
