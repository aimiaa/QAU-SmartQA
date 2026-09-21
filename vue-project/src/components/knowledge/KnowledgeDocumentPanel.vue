<script setup lang="ts">
import { ref, watch } from 'vue';
import { AlertCircle, CheckCircle2, FileText, Loader2, RefreshCw, Trash2, X } from 'lucide-vue-next';
import { knowledgeApi } from '../../api/knowledge';
import type { KnowledgeBase, KnowledgeDocument, KnowledgeDocumentStatus } from '../../types';

const props = defineProps<{
  open: boolean;
  knowledgeBase: KnowledgeBase;
}>();

const emit = defineEmits<{
  close: [];
  deleted: [];
}>();

const documents = ref<KnowledgeDocument[]>([]);
const loading = ref(false);
const deletingId = ref<number | null>(null);
const pendingDeleteId = ref<number | null>(null);
const errorMessage = ref('');

const statusMeta: Record<KnowledgeDocumentStatus, { label: string; icon: typeof CheckCircle2 }> = {
  uploaded: { label: '已上传', icon: FileText },
  parsing: { label: '解析中', icon: Loader2 },
  ready: { label: '可检索', icon: CheckCircle2 },
  failed: { label: '解析失败', icon: AlertCircle },
};

const formatSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
};

const statusOf = (status: KnowledgeDocumentStatus) => statusMeta[status] ?? statusMeta.uploaded;

const loadDocuments = async () => {
  loading.value = true;
  errorMessage.value = '';
  try {
    documents.value = await knowledgeApi.getDocuments(props.knowledgeBase.id);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '文档列表加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
};

const confirmDelete = (id: number) => {
  pendingDeleteId.value = id;
};

const cancelDelete = () => {
  pendingDeleteId.value = null;
};

const deleteDocument = async (document: KnowledgeDocument) => {
  if (deletingId.value !== null) return;

  deletingId.value = document.id;
  errorMessage.value = '';
  try {
    await knowledgeApi.deleteDocument(props.knowledgeBase.id, document.id);
    documents.value = documents.value.filter((item) => item.id !== document.id);
    pendingDeleteId.value = null;
    emit('deleted');
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '文档删除失败，请稍后重试';
  } finally {
    deletingId.value = null;
  }
};

watch(
  () => [props.open, props.knowledgeBase.id] as const,
  ([open]) => {
    if (open) void loadDocuments();
  },
  { immediate: true },
);
</script>

<template>
  <div class="document-panel-mask" @click.self="emit('close')">
    <section class="document-panel panel" role="dialog" aria-modal="true" aria-labelledby="document-panel-title">
      <header class="document-panel-head">
        <div>
          <span class="panel-kicker">Documents</span>
          <h2 id="document-panel-title">{{ knowledgeBase.name }}</h2>
          <p>查看当前知识库中的文档，并管理解析结果。</p>
        </div>
        <button class="icon-button" type="button" aria-label="关闭文档列表" @click="emit('close')">
          <X :size="18" />
        </button>
      </header>

      <div class="document-panel-toolbar">
        <span>{{ documents.length }} 份文档</span>
        <button class="ghost-button" type="button" :disabled="loading" @click="loadDocuments">
          <RefreshCw :size="16" :class="{ spin: loading }" />
          <span>刷新</span>
        </button>
      </div>

      <p v-if="errorMessage" class="kb-alert error" role="alert">{{ errorMessage }}</p>

      <div v-if="loading && !documents.length" class="document-panel-empty">
        <Loader2 :size="24" class="spin" />
        <span>正在加载文档...</span>
      </div>

      <div v-else-if="!documents.length" class="document-panel-empty">
        <FileText :size="26" />
        <strong>这个知识库还没有文档</strong>
        <span>关闭窗口后，可以从知识库卡片上传 PDF、Word、Markdown 或文本文件。</span>
      </div>

      <div v-else class="document-list">
        <article v-for="document in documents" :key="document.id" class="document-row">
          <div class="document-file-icon"><FileText :size="19" /></div>
          <div class="document-info">
            <strong :title="document.fileName">{{ document.title || document.fileName }}</strong>
            <span>{{ document.fileName }} · {{ formatSize(document.fileSize) }} · {{ document.uploadedAt || '时间未知' }}</span>
          </div>
          <span class="document-status" :class="`document-status-${document.status}`">
            <component :is="statusOf(document.status).icon" :size="14" :class="{ spin: document.status === 'parsing' }" />
            {{ statusOf(document.status).label }}
          </span>
          <div v-if="pendingDeleteId === document.id" class="document-confirm">
            <span>确认删除？</span>
            <button class="danger-button" type="button" :disabled="deletingId !== null" @click="deleteDocument(document)">删除</button>
            <button class="ghost-button" type="button" :disabled="deletingId !== null" @click="cancelDelete">取消</button>
          </div>
          <button
            v-else
            class="icon-button danger-icon-button"
            type="button"
            :aria-label="`删除 ${document.fileName}`"
            :disabled="deletingId !== null"
            @click="confirmDelete(document.id)"
          >
            <Trash2 :size="16" />
          </button>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.document-panel-mask {
  position: fixed;
  inset: 0;
  z-index: 110;
  display: grid;
  place-items: center;
  padding: 22px;
  background: rgba(7, 18, 15, 0.48);
  backdrop-filter: blur(8px);
}

.document-panel {
  display: grid;
  width: min(900px, 100%);
  max-height: min(84vh, 760px);
  grid-template-rows: auto auto minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: var(--shadow);
}

.document-panel-head,
.document-panel-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--line);
}

.document-panel-head h2 {
  margin: 3px 0 0;
  font-size: 20px;
}

.document-panel-head p {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 13px;
}

.document-panel-toolbar {
  align-items: center;
  padding-block: 12px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.document-panel-toolbar .ghost-button {
  height: 32px;
  padding: 0 11px;
  font-size: 12px;
}

.document-panel > .kb-alert {
  margin: 14px 20px 0;
}

.document-list {
  display: grid;
  align-content: start;
  gap: 8px;
  overflow-y: auto;
  padding: 14px 20px 20px;
}

.document-row {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) auto auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--surface-soft);
}

.document-file-icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  color: var(--primary-600);
  border-radius: 11px;
  background: var(--primary-50);
}

.document-info {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.document-info strong,
.document-info span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-info strong {
  color: var(--text);
  font-size: 13px;
}

.document-info span {
  color: var(--text-muted);
  font-size: 12px;
}

.document-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 8px;
  border-radius: 999px;
  color: var(--text-muted);
  background: var(--surface-muted);
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.document-status-ready {
  color: var(--primary-600);
  background: var(--primary-50);
}

.document-status-failed {
  color: #dc2626;
  background: rgba(220, 38, 38, 0.09);
}

.document-confirm {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--text-soft);
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.document-confirm .ghost-button,
.document-confirm .danger-button {
  height: 32px;
  padding: 0 10px;
  font-size: 12px;
}

.danger-icon-button {
  color: #dc2626;
}

.danger-icon-button:hover:not(:disabled) {
  border-color: rgba(220, 38, 38, 0.32);
  background: rgba(220, 38, 38, 0.08);
}

.document-panel-empty {
  display: grid;
  min-height: 220px;
  place-items: center;
  align-content: center;
  gap: 8px;
  padding: 30px;
  color: var(--text-muted);
  text-align: center;
}

.document-panel-empty svg {
  color: var(--primary-600);
}

.document-panel-empty strong {
  color: var(--text);
  font-size: 15px;
}

.document-panel-empty span {
  max-width: 460px;
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 680px) {
  .document-panel-mask {
    padding: 12px;
  }

  .document-row {
    grid-template-columns: 40px minmax(0, 1fr) auto;
  }

  .document-status {
    grid-column: 2;
    justify-self: start;
  }

  .document-confirm {
    grid-column: 2 / -1;
    justify-self: end;
  }
}
</style>
