<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import {
  AlertCircle,
  ArrowLeft,
  CheckCheck,
  CheckCircle2,
  Clock3,
  CloudUpload,
  Database,
  FileText,
  HardDrive,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  X,
} from 'lucide-vue-next';
import MetricCard from '../metrics/MetricCard.vue';
import UploadKnowledgeDocumentDialog from './UploadKnowledgeDocumentDialog.vue';
import { useKnowledgeBases } from '../../composables/useKnowledgeBases';
import type { KnowledgeBaseDraft, KnowledgeBaseStatus } from '../../types';

interface DocumentUploadedPayload {
  knowledgeBaseId: number;
  count: number;
  localOnly: boolean;
}

const emit = defineEmits<{
  back: [];
}>();

const {
  categories,
  clearSelectedKnowledgeBases,
  createKnowledgeBase,
  documentCount,
  errorMessage,
  knowledgeBases,
  loadKnowledgeBases,
  loading,
  markDocumentsUploaded,
  notice,
  readyCount,
  removeKnowledgeBase,
  resetFeedback,
  saving,
  selectAllKnowledgeBases,
  selectedKbIds,
  toggleKnowledgeBase,
} = useKnowledgeBases();

const statusMeta: Record<KnowledgeBaseStatus, { text: string; icon: typeof CheckCircle2 }> = {
  ready: { text: '已完成', icon: CheckCircle2 },
  syncing: { text: '解析中', icon: RefreshCw },
  building: { text: '待上传', icon: HardDrive },
  review: { text: '待复核', icon: AlertCircle },
  failed: { text: '处理失败', icon: AlertCircle },
};

const statusFilters: Array<{ value: 'all' | KnowledgeBaseStatus; label: string }> = [
  { value: 'all', label: '全部' },
  { value: 'ready', label: '已完成' },
  { value: 'syncing', label: '解析中' },
  { value: 'building', label: '待上传' },
  { value: 'review', label: '待复核' },
];

const keyword = ref('');
const statusFilter = ref<'all' | KnowledgeBaseStatus>('all');
const formOpen = ref(false);
const uploadDialogOpen = ref(false);
const uploadTargetKnowledgeBaseId = ref<number | null>(null);
const pendingDeleteId = ref<number | null>(null);

const draft = reactive<KnowledgeBaseDraft>({
  name: '',
  category: '',
  description: '',
});

const filteredBases = computed(() => {
  const text = keyword.value.trim().toLowerCase();

  return knowledgeBases.value.filter((item) => {
    const matchStatus = statusFilter.value === 'all' || item.status === statusFilter.value;
    const matchText =
      !text ||
      item.name.toLowerCase().includes(text) ||
      item.category.toLowerCase().includes(text) ||
      item.description.toLowerCase().includes(text);

    return matchStatus && matchText;
  });
});

const resetDraft = () => {
  draft.name = '';
  draft.category = '';
  draft.description = '';
};

const openForm = () => {
  resetFeedback();
  resetDraft();
  formOpen.value = true;
};

const closeForm = () => {
  formOpen.value = false;
  resetDraft();
};

const openUploadDialog = (knowledgeBaseId?: number) => {
  resetFeedback();
  uploadTargetKnowledgeBaseId.value = knowledgeBaseId ?? null;
  uploadDialogOpen.value = true;
};

const closeUploadDialog = () => {
  uploadDialogOpen.value = false;
  uploadTargetKnowledgeBaseId.value = null;
};

const submitDraft = async () => {
  const created = await createKnowledgeBase({ ...draft });

  if (created) {
    const createdName = draft.name.trim();
    const createdBase = knowledgeBases.value.find((item) => item.name === createdName);
    closeForm();
    openUploadDialog(createdBase?.id);
  }
};

const handleDocumentsUploaded = (payload: DocumentUploadedPayload) => {
  markDocumentsUploaded(payload.knowledgeBaseId, payload.count);
  notice.value = payload.localOnly
    ? `已在前端联调模式加入 ${payload.count} 个文件，接入后端后将真实上传并解析`
    : `${payload.count} 个文件已上传，知识库正在后台解析和向量化`;
  errorMessage.value = '';
};

const confirmDelete = (id: number) => {
  resetFeedback();
  pendingDeleteId.value = id;
};

const cancelDelete = () => {
  pendingDeleteId.value = null;
};

const handleDelete = async (id: number) => {
  pendingDeleteId.value = null;
  await removeKnowledgeBase(id);
};

onMounted(() => {
  void loadKnowledgeBases();
});
</script>

<template>
  <section class="kb-page" aria-label="RAG 知识库管理">
    <header class="kb-page-head panel">
      <div class="kb-page-head-left">
        <button class="icon-button" type="button" aria-label="返回智能问答" @click="emit('back')">
          <ArrowLeft :size="18" />
        </button>
        <div class="kb-page-title">
          <span class="panel-kicker">Knowledge Base</span>
          <h2>RAG 知识库管理</h2>
          <p>新建知识库，或向已有知识库上传 PDF、Word、Markdown 与文本资料。</p>
        </div>
      </div>
      <div class="kb-page-actions">
        <button class="ghost-button" type="button" :disabled="loading" @click="loadKnowledgeBases(true)">
          <RefreshCw :size="16" :class="{ spin: loading }" />
          <span>刷新</span>
        </button>
        <button class="ghost-button" type="button" @click="openForm">
          <Plus :size="17" />
          <span>新建知识库</span>
        </button>
        <button class="primary-button" type="button" :disabled="!knowledgeBases.length" @click="openUploadDialog()">
          <CloudUpload :size="17" />
          <span>上传文档</span>
        </button>
      </div>
    </header>

    <div class="metric-grid kb-page-metrics">
      <MetricCard label="知识库总数" :value="knowledgeBases.length.toString()" detail="全部已接入" tone="primary" />
      <MetricCard label="可用知识库" :value="readyCount.toString()" detail="可参与回答" tone="green" />
      <MetricCard label="文档总量" :value="documentCount.toString()" detail="制度、指南、通知" tone="orange" />
    </div>

    <div class="kb-toolbar panel">
      <label class="search-box" aria-label="搜索知识库">
        <Search :size="18" />
        <input v-model="keyword" type="search" placeholder="按名称、分类或描述搜索" />
      </label>

      <div class="kb-filter-row" role="group" aria-label="按状态筛选">
        <button
          v-for="filter in statusFilters"
          :key="filter.value"
          class="chip"
          :class="{ active: statusFilter === filter.value }"
          type="button"
          @click="statusFilter = filter.value"
        >
          {{ filter.label }}
        </button>
      </div>

      <div class="kb-select-actions">
        <span>检索已选 {{ selectedKbIds.length }} / {{ knowledgeBases.length }}</span>
        <button class="ghost-button" type="button" @click="selectAllKnowledgeBases">
          <CheckCheck :size="16" />
          <span>全选</span>
        </button>
        <button class="ghost-button" type="button" @click="clearSelectedKnowledgeBases">
          <X :size="16" />
          <span>清空</span>
        </button>
      </div>
    </div>

    <p v-if="errorMessage" class="kb-alert error" role="alert">{{ errorMessage }}</p>
    <p v-else-if="notice" class="kb-alert success" role="status">{{ notice }}</p>

    <div v-if="formOpen" class="kb-form panel">
      <div class="panel-heading">
        <div>
          <span class="panel-kicker">Create</span>
          <h2>新建 RAG 知识库</h2>
          <span>只创建知识库信息；创建成功后会引导你继续上传文档。</span>
        </div>
        <button class="icon-button" type="button" aria-label="关闭表单" @click="closeForm">
          <X :size="18" />
        </button>
      </div>

      <form class="kb-form-body" @submit.prevent="submitDraft">
        <label class="kb-field">
          <span>知识库名称 <i>*</i></span>
          <input v-model="draft.name" type="text" maxlength="40" placeholder="例如：本科教学管理制度" />
        </label>

        <label class="kb-field">
          <span>分类</span>
          <input
            v-model="draft.category"
            type="text"
            maxlength="20"
            list="kb-category-options"
            placeholder="例如：教务"
          />
          <datalist id="kb-category-options">
            <option v-for="item in categories" :key="item" :value="item" />
          </datalist>
        </label>

        <label class="kb-field">
          <span>系统状态</span>
          <input value="待上传文档" disabled />
        </label>

        <label class="kb-field kb-field-wide">
          <span>描述</span>
          <textarea
            v-model="draft.description"
            rows="3"
            maxlength="200"
            placeholder="说明该知识库覆盖的内容范围，便于问答时自动路由。"
          />
        </label>

        <div class="kb-form-footer">
          <button class="ghost-button" type="button" @click="closeForm">取消</button>
          <button class="primary-button" type="submit" :disabled="saving || !draft.name.trim()">
            <Plus :size="17" />
            <span>{{ saving ? '创建中...' : '创建并上传文档' }}</span>
          </button>
        </div>
      </form>
    </div>

    <div v-if="!filteredBases.length" class="kb-empty panel">
      <Database :size="26" />
      <strong>暂无匹配的知识库</strong>
      <span>调整搜索条件，或点击“新建知识库”创建后再上传 PDF、DOCX、DOC、TXT、MD 文件。</span>
    </div>

    <div v-else class="kb-grid">
      <article
        v-for="kb in filteredBases"
        :key="kb.id"
        class="kb-manage-card panel"
        :class="{ active: selectedKbIds.includes(kb.id) }"
      >
        <div class="kb-card-top">
          <label class="check-row">
            <input
              type="checkbox"
              :checked="selectedKbIds.includes(kb.id)"
              @change="toggleKnowledgeBase(kb.id)"
            />
            <span>{{ kb.name }}</span>
          </label>
          <span class="status-badge" :class="`status-${kb.status}`">
            <component :is="statusMeta[kb.status].icon" :size="15" class="status-icon" :class="{ spin: kb.status === 'syncing' }" />
            {{ statusMeta[kb.status].text }}
          </span>
        </div>

        <p>{{ kb.description }}</p>

        <div class="kb-meta">
          <span><FileText :size="14" />{{ kb.documents }} 份</span>
          <span><Clock3 :size="14" />{{ kb.updatedAt }}</span>
        </div>

        <div class="kb-card-footer">
          <span class="kb-badge">{{ kb.category }}</span>

          <div v-if="pendingDeleteId === kb.id" class="kb-confirm">
            <span>确认删除？</span>
            <button class="danger-button" type="button" :disabled="saving" @click="handleDelete(kb.id)">
              删除
            </button>
            <button class="ghost-button" type="button" @click="cancelDelete">取消</button>
          </div>

          <div v-else class="kb-card-actions">
            <button class="ghost-button" type="button" @click="openUploadDialog(kb.id)">
              <CloudUpload :size="16" />
              <span>上传文档</span>
            </button>
            <button
              class="ghost-button danger"
              type="button"
              :aria-label="`删除 ${kb.name}`"
              @click="confirmDelete(kb.id)"
            >
              <Trash2 :size="16" />
            </button>
          </div>
        </div>
      </article>
    </div>

    <UploadKnowledgeDocumentDialog
      v-if="uploadDialogOpen"
      :open="uploadDialogOpen"
      :knowledge-bases="knowledgeBases"
      :default-knowledge-base-id="uploadTargetKnowledgeBaseId"
      :loading-bases="loading"
      @close="closeUploadDialog"
      @uploaded="handleDocumentsUploaded"
    />
  </section>
</template>

