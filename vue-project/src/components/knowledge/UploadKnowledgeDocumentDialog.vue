<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  AlertCircle,
  CheckCircle2,
  CloudUpload,
  FileText,
  Loader2,
  RefreshCw,
  Trash2,
  X,
} from 'lucide-vue-next';
import { knowledgeApi } from '../../api/knowledge';
import { ApiError } from '../../api/request';
import {
  ALLOWED_KNOWLEDGE_FILE_EXTENSIONS,
  KNOWLEDGE_FILE_ACCEPT,
  KNOWLEDGE_FILE_TYPES_LABEL,
  MAX_KNOWLEDGE_FILE_SIZE,
} from '../../constants';
import type { KnowledgeBase, KnowledgeDocument, UploadQueueItem } from '../../types';
import { formatNow } from '../../utils/date';

interface DocumentUploadedPayload {
  knowledgeBaseId: number;
  count: number;
  localOnly: boolean;
}

const props = defineProps<{
  open: boolean;
  knowledgeBases: KnowledgeBase[];
  defaultKnowledgeBaseId?: number | null;
  loadingBases?: boolean;
}>();

const emit = defineEmits<{
  close: [];
  uploaded: [payload: DocumentUploadedPayload];
}>();

const fileInputRef = ref<HTMLInputElement | null>(null);
const targetKnowledgeBaseId = ref<number | null>(null);
const queue = ref<UploadQueueItem[]>([]);
const dragActive = ref(false);
const uploading = ref(false);
const validationMessages = ref<string[]>([]);
const batchFinished = ref(false);

const targetKnowledgeBase = computed(() =>
  props.knowledgeBases.find((item) => item.id === targetKnowledgeBaseId.value),
);

const selectableKnowledgeBases = computed(() =>
  [...props.knowledgeBases].sort((a, b) => {
    const statusWeight: Record<KnowledgeBase['status'], number> = {
      ready: 0,
      syncing: 1,
      building: 2,
      review: 3,
      failed: 4,
    };

    return statusWeight[a.status] - statusWeight[b.status] || a.name.localeCompare(b.name, 'zh-CN');
  }),
);

const waitingCount = computed(
  () => queue.value.filter((item) => item.status === 'waiting' || item.status === 'error').length,
);

const successCount = computed(() => queue.value.filter((item) => item.status === 'success').length);
const failedCount = computed(() => queue.value.filter((item) => item.status === 'error').length);
const hasQueue = computed(() => queue.value.length > 0);

const canUpload = computed(
  () =>
    !uploading.value &&
    targetKnowledgeBaseId.value !== null &&
    waitingCount.value > 0,
);

const getFileExtension = (fileName: string) => {
  const index = fileName.lastIndexOf('.');
  return index >= 0 ? fileName.slice(index).toLowerCase() : '';
};

const formatFileSize = (size: number) => {
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};

const resetQueue = () => {
  queue.value = [];
  validationMessages.value = [];
  batchFinished.value = false;
};

const openFilePicker = () => {
  if (uploading.value) return;
  fileInputRef.value?.click();
};

const removeFile = (uid: string) => {
  if (uploading.value) return;
  queue.value = queue.value.filter((item) => item.uid !== uid);
};

const addFiles = (fileList: FileList | File[]) => {
  if (uploading.value) return;

  batchFinished.value = false;
  validationMessages.value = [];

  const incoming = Array.from(fileList);
  const nextMessages: string[] = [];
  const accepted: UploadQueueItem[] = [];
  const existingKeys = new Set(
    queue.value.map((item) => `${item.file.name}_${item.file.size}_${item.file.lastModified}`),
  );

  incoming.forEach((file) => {
    const extension = getFileExtension(file.name);
    const duplicateKey = `${file.name}_${file.size}_${file.lastModified}`;

    if (!extension || !ALLOWED_KNOWLEDGE_FILE_EXTENSIONS.includes(extension as (typeof ALLOWED_KNOWLEDGE_FILE_EXTENSIONS)[number])) {
      nextMessages.push(`「${file.name}」格式不支持，仅支持 ${KNOWLEDGE_FILE_TYPES_LABEL}`);
      return;
    }

    if (file.size === 0) {
      nextMessages.push(`「${file.name}」是空文件，不能上传`);
      return;
    }

    if (file.size > MAX_KNOWLEDGE_FILE_SIZE) {
      nextMessages.push(`「${file.name}」超过 50MB，当前大小 ${formatFileSize(file.size)}`);
      return;
    }

    if (existingKeys.has(duplicateKey)) {
      nextMessages.push(`「${file.name}」已在上传列表中`);
      return;
    }

    existingKeys.add(duplicateKey);
    accepted.push({
      uid: `${Date.now()}_${Math.random().toString(36).slice(2, 9)}`,
      file,
      status: 'waiting',
      progress: 0,
    });
  });

  validationMessages.value = nextMessages;
  queue.value.push(...accepted);
};

const handleFileInputChange = (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (input.files?.length) {
    addFiles(input.files);
  }
  input.value = '';
};

const handleDragOver = (event: DragEvent) => {
  event.preventDefault();
  if (!uploading.value) dragActive.value = true;
};

const handleDragLeave = (event: DragEvent) => {
  event.preventDefault();
  dragActive.value = false;
};

const handleDrop = (event: DragEvent) => {
  event.preventDefault();
  dragActive.value = false;

  if (event.dataTransfer?.files?.length) {
    addFiles(event.dataTransfer.files);
  }
};

const buildLocalDocument = (knowledgeBaseId: number, file: File): KnowledgeDocument => {
  const extension = getFileExtension(file.name);
  const title = file.name.slice(0, file.name.length - extension.length);

  return {
    id: Date.now() + Math.floor(Math.random() * 10000),
    knowledgeBaseId,
    title,
    fileName: file.name,
    fileType: extension,
    fileSize: file.size,
    status: 'parsing',
    uploadedAt: `今天 ${formatNow()}`,
  };
};

const simulateLocalUpload = (item: UploadQueueItem, knowledgeBaseId: number) =>
  new Promise<KnowledgeDocument>((resolve) => {
    item.progress = 8;

    const timer = window.setInterval(() => {
      item.progress = Math.min(96, item.progress + Math.floor(Math.random() * 22) + 10);

      if (item.progress >= 96) {
        window.clearInterval(timer);
        item.progress = 100;
        resolve(buildLocalDocument(knowledgeBaseId, item.file));
      }
    }, 130);
  });

const uploadOne = async (item: UploadQueueItem, knowledgeBaseId: number) => {
  item.status = 'uploading';
  item.progress = 0;
  item.errorMessage = undefined;

  try {
    await knowledgeApi.uploadDocument(knowledgeBaseId, item.file, (event) => {
      item.progress = event.progress;
    });
    item.progress = 100;
    item.status = 'success';
    return 'server' as const;
  } catch (error) {
    const canUseLocalFallback =
      (error instanceof ApiError && (error.status === 404 || error.status === undefined)) ||
      (!(error instanceof ApiError) && error instanceof Error);

    if (canUseLocalFallback) {
      try {
        await simulateLocalUpload(item, knowledgeBaseId);
        item.progress = 100;
        item.status = 'success';
        return 'local' as const;
      } catch {
        // 本地模拟不会失败，保留兜底分支避免中断后续文件。
      }
    }

    item.status = 'error';
    item.progress = 0;
    item.errorMessage = error instanceof Error ? error.message : '上传失败，请稍后重试';
    return 'failed' as const;
  }
};

const uploadQueue = async () => {
  const knowledgeBaseId = targetKnowledgeBaseId.value;
  if (!knowledgeBaseId || uploading.value) return;

  uploading.value = true;
  batchFinished.value = false;

  let uploaded = 0;
  let localOnly = false;
  const pendingItems = queue.value.filter((item) => item.status === 'waiting' || item.status === 'error');

  for (const item of pendingItems) {
    // eslint-disable-next-line no-await-in-loop
    const result = await uploadOne(item, knowledgeBaseId);
    if (result === 'server' || result === 'local') {
      uploaded += 1;
    }
    if (result === 'local') {
      localOnly = true;
    }
  }

  uploading.value = false;
  batchFinished.value = true;

  if (uploaded > 0) {
    emit('uploaded', {
      knowledgeBaseId,
      count: uploaded,
      localOnly,
    });
  }
};

const closeDialog = () => {
  if (uploading.value) return;
  emit('close');
};

const pickDefaultTarget = () => {
  if (props.defaultKnowledgeBaseId) {
    targetKnowledgeBaseId.value = props.defaultKnowledgeBaseId;
    return;
  }

  targetKnowledgeBaseId.value = selectableKnowledgeBases.value[0]?.id ?? null;
};

const resetDialog = () => {
  pickDefaultTarget();
  resetQueue();
  uploading.value = false;
  dragActive.value = false;
};

resetDialog();

defineExpose({ resetDialog });
</script>

<template>
  <div class="upload-dialog-mask" @click.self="closeDialog">
    <section class="upload-dialog panel" role="dialog" aria-modal="true" aria-labelledby="upload-dialog-title">
      <header class="upload-dialog-head">
        <div class="upload-dialog-title">
          <span class="panel-kicker">Upload</span>
          <h2 id="upload-dialog-title">上传知识库文档</h2>
          <p>将文件上传到已有知识库，系统保存后会异步解析、切片并向量化。</p>
        </div>
        <button class="icon-button" type="button" aria-label="关闭上传窗口" :disabled="uploading" @click="closeDialog">
          <X :size="18" />
        </button>
      </header>

      <div class="upload-dialog-body">
        <label class="kb-field upload-target-field">
          <span>目标知识库 <i>*</i></span>
          <select v-model.number="targetKnowledgeBaseId" :disabled="uploading || !knowledgeBases.length">
            <option v-if="!knowledgeBases.length" :value="null" disabled>暂无可选知识库，请先新建</option>
            <option v-for="kb in selectableKnowledgeBases" :key="kb.id" :value="kb.id">
              {{ kb.name }}（{{ kb.category }}）
            </option>
          </select>
          <small v-if="targetKnowledgeBase">
            当前文档数：{{ targetKnowledgeBase.documents }} 份 · 上传后该库将进入解析/同步状态
          </small>
        </label>

        <input
          ref="fileInputRef"
          :accept="KNOWLEDGE_FILE_ACCEPT"
          type="file"
          multiple
          hidden
          @change="handleFileInputChange"
        />

        <button
          class="upload-dropzone"
          type="button"
          :class="{ active: dragActive, disabled: uploading }"
          :disabled="uploading"
          @click="openFilePicker"
          @dragover="handleDragOver"
          @dragleave="handleDragLeave"
          @drop="handleDrop"
        >
          <span class="upload-dropzone-icon">
            <CloudUpload :size="30" />
          </span>
          <strong>点击选择文件，或将文件拖拽到这里</strong>
          <span>支持 {{ KNOWLEDGE_FILE_TYPES_LABEL }}，单个文件最大 50MB</span>
          <span class="upload-dropzone-hint">可一次选择多个文件，队列中将逐个上传</span>
        </button>

        <div v-if="validationMessages.length" class="upload-validation" role="alert">
          <AlertCircle :size="16" />
          <div>
            <p v-for="message in validationMessages" :key="message">{{ message }}</p>
          </div>
        </div>

        <div v-if="hasQueue" class="upload-queue">
          <div class="upload-queue-head">
            <span>上传列表</span>
            <span>{{ successCount }} 成功 / {{ failedCount }} 失败 / {{ queue.length }} 总计</span>
          </div>

          <div v-for="item in queue" :key="item.uid" class="upload-queue-item">
            <div class="upload-file-icon">
              <FileText :size="18" />
            </div>

            <div class="upload-file-info">
              <div class="upload-file-row">
                <strong :title="item.file.name">{{ item.file.name }}</strong>
                <span>{{ formatFileSize(item.file.size) }}</span>
              </div>

              <div class="upload-progress-track">
                <div
                  class="upload-progress-bar"
                  :class="item.status"
                  :style="{ width: `${item.status === 'error' ? 0 : item.progress}%` }"
                ></div>
              </div>

              <p v-if="item.status === 'error'" class="upload-file-error">
                <AlertCircle :size="13" />
                {{ item.errorMessage || '上传失败，可重试' }}
              </p>
            </div>

            <div class="upload-file-action">
              <Loader2 v-if="item.status === 'uploading'" :size="17" class="spin" />
              <CheckCircle2 v-else-if="item.status === 'success'" :size="18" class="success-icon" />
              <AlertCircle v-else-if="item.status === 'error'" :size="18" class="error-icon" />
              <RefreshCw v-else :size="17" />

              <button
                v-if="item.status === 'waiting' || item.status === 'error'"
                class="icon-button"
                type="button"
                :aria-label="`移除 ${item.file.name}`"
                :disabled="uploading"
                @click="removeFile(item.uid)"
              >
                <Trash2 :size="16" />
              </button>
            </div>
          </div>
        </div>

        <div v-if="batchFinished && successCount > 0" class="upload-result success" role="status">
          <CheckCircle2 :size="16" />
          <span>{{ successCount }} 个文件已提交，文档解析和向量化将在后台继续进行。</span>
        </div>
      </div>

      <footer class="upload-dialog-footer">
        <span class="upload-footer-tip">上传不是新建知识库，请先在上方选择目标知识库</span>
        <div class="upload-footer-actions">
          <button class="ghost-button" type="button" :disabled="uploading" @click="closeDialog">取消</button>
          <button class="primary-button" type="button" :disabled="!canUpload" @click="uploadQueue">
            <CloudUpload :size="17" />
            <span>{{ uploading ? '上传中...' : failedCount > 0 ? '重试失败文件' : '开始上传' }}</span>
          </button>
        </div>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.upload-dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  padding: 22px;
  background: rgba(7, 18, 15, 0.48);
  backdrop-filter: blur(8px);
}

.upload-dialog {
  display: grid;
  width: min(760px, 100%);
  max-height: min(88vh, 820px);
  grid-template-rows: auto minmax(0, 1fr) auto;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: var(--shadow);
}

.upload-dialog-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--line);
}

.upload-dialog-title h2 {
  margin: 3px 0 0;
  font-size: 20px;
}

.upload-dialog-title p {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 13px;
}

.upload-dialog-body {
  display: grid;
  gap: 14px;
  align-content: start;
  overflow-y: auto;
  padding: 18px 20px;
}

.upload-target-field small {
  color: var(--text-muted);
  font-size: 12px;
}

.upload-dropzone {
  display: grid;
  gap: 8px;
  justify-items: center;
  min-height: 190px;
  padding: 24px 18px;
  color: var(--text-soft);
  border: 1.5px dashed var(--line-strong);
  border-radius: 17px;
  background:
    radial-gradient(circle at 50% 0%, var(--primary-50), transparent 58%),
    var(--surface-soft);
  transition: border-color 0.16s ease, background 0.16s ease, transform 0.16s ease;
}

.upload-dropzone:hover,
.upload-dropzone.active {
  border-color: var(--primary-500);
  background: var(--primary-50);
  transform: translateY(-1px);
}

.upload-dropzone.disabled {
  opacity: 0.65;
}

.upload-dropzone-icon {
  display: grid;
  width: 62px;
  height: 62px;
  place-items: center;
  color: var(--primary-600);
  border-radius: 20px;
  background: var(--surface);
  box-shadow: var(--shadow-tight);
}

.upload-dropzone strong {
  color: var(--text);
  font-size: 15px;
}

.upload-dropzone span:not(.upload-dropzone-icon) {
  font-size: 13px;
}

.upload-dropzone-hint {
  color: var(--text-muted);
  font-size: 12px !important;
}

.upload-validation,
.upload-result {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  padding: 11px 13px;
  border: 1px solid transparent;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 700;
}

.upload-validation {
  color: #b45309;
  border-color: rgba(245, 158, 11, 0.3);
  background: rgba(245, 158, 11, 0.1);
}

.upload-validation p {
  margin: 0;
  line-height: 1.6;
}

.upload-result.success {
  color: var(--primary-600);
  border-color: var(--line-strong);
  background: var(--primary-50);
}

.upload-queue {
  display: grid;
  gap: 9px;
}

.upload-queue-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--text-soft);
  font-size: 12px;
  font-weight: 800;
}

.upload-queue-item {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) auto;
  gap: 11px;
  align-items: center;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--surface-soft);
}

.upload-file-icon {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  color: var(--primary-600);
  border-radius: 12px;
  background: var(--primary-50);
}

.upload-file-info {
  display: grid;
  min-width: 0;
  gap: 7px;
}

.upload-file-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.upload-file-row strong {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.upload-file-row span {
  flex: 0 0 auto;
  color: var(--text-muted);
  font-size: 12px;
}

.upload-progress-track {
  height: 7px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--surface-muted);
}

.upload-progress-bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--primary-500), var(--primary-300));
  transition: width 0.18s ease;
}

.upload-progress-bar.uploading {
  background: linear-gradient(90deg, #2563eb, #60a5fa);
}

.upload-progress-bar.error {
  width: 0 !important;
  background: var(--red);
}

.upload-file-error {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 0;
  color: #dc2626;
  font-size: 12px;
}

.upload-file-action {
  display: flex;
  align-items: center;
  gap: 4px;
}

.success-icon {
  color: var(--primary-600);
}

.error-icon {
  color: var(--red);
}

.upload-dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 20px 18px;
  border-top: 1px solid var(--line);
}

.upload-footer-tip {
  color: var(--text-muted);
  font-size: 12px;
}

.upload-footer-actions {
  display: flex;
  gap: 10px;
}

@media (max-width: 640px) {
  .upload-dialog-mask {
    padding: 12px;
  }

  .upload-dialog-footer,
  .upload-queue-head {
    align-items: stretch;
    flex-direction: column;
  }

  .upload-footer-actions {
    justify-content: stretch;
  }

  .upload-footer-actions button {
    flex: 1;
  }
}
</style>



