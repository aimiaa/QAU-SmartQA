import { computed, ref } from 'vue';
import { knowledgeApi } from '../api/knowledge';
import { knowledgeBases as fallbackKnowledgeBases } from '../constants/mockData';
import type { KnowledgeBase, KnowledgeBaseDraft } from '../types';

const STORAGE_KEY = 'qau-smartqa-knowledge-bases';
const SELECTED_STORAGE_KEY = 'qau-smartqa-selected-knowledge-bases';

const readJson = <T>(key: string): T | null => {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : null;
  } catch {
    return null;
  }
};

const writeJson = (key: string, value: unknown) => {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // 存储不可用时忽略，仅影响刷新后的本地持久化。
  }
};

const cachedBases = readJson<KnowledgeBase[]>(STORAGE_KEY);
const cachedSelected = readJson<number[]>(SELECTED_STORAGE_KEY);

const defaultBases = cachedBases?.length ? cachedBases : fallbackKnowledgeBases.map((item) => ({ ...item }));

// 模块级状态：侧边栏、问答页与知识库管理页共享同一份数据。
const knowledgeBases = ref<KnowledgeBase[]>(defaultBases);
const selectedKbIds = ref<number[]>(
  cachedSelected ?? defaultBases.filter((item) => item.status === 'ready').map((item) => item.id),
);
const loading = ref(false);
const saving = ref(false);
const errorMessage = ref('');
const notice = ref('');
const loadedFromServer = ref(false);

const formatToday = () =>
  `今天 ${new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`;

const nextLocalId = () =>
  knowledgeBases.value.reduce((max, item) => Math.max(max, item.id), 0) + 1;

const persist = () => {
  writeJson(STORAGE_KEY, knowledgeBases.value);
  writeJson(SELECTED_STORAGE_KEY, selectedKbIds.value);
};

const pruneSelection = () => {
  const ids = new Set(knowledgeBases.value.map((item) => item.id));
  selectedKbIds.value = selectedKbIds.value.filter((id) => ids.has(id));
};

export const useKnowledgeBases = () => {
  const selectedKnowledgeBases = computed(() =>
    knowledgeBases.value.filter((item) => selectedKbIds.value.includes(item.id)),
  );

  const readyCount = computed(
    () => knowledgeBases.value.filter((item) => item.status === 'ready').length,
  );

  const documentCount = computed(() =>
    knowledgeBases.value.reduce((total, item) => total + (item.documents ?? 0), 0),
  );

  const categories = computed(() => [
    ...new Set(knowledgeBases.value.map((item) => item.category).filter(Boolean)),
  ]);

  const resetFeedback = () => {
    errorMessage.value = '';
    notice.value = '';
  };

  /** 拉取知识库列表；后端不可用时保留本地数据，保证界面可用。 */
  const loadKnowledgeBases = async (force = false) => {
    if (loading.value || (loadedFromServer.value && !force)) return;

    loading.value = true;

    try {
      const list = await knowledgeApi.getKnowledgeBases();

      if (list?.length) {
        knowledgeBases.value = list;
        loadedFromServer.value = true;
        pruneSelection();
        persist();
      }
    } catch {
      // 后端未就绪时使用本地演示数据。
    } finally {
      loading.value = false;
    }
  };

  /** 新建知识库：优先调用后端，失败时在本地创建，便于前端独立联调。 */
  const createKnowledgeBase = async (draft: KnowledgeBaseDraft) => {
    const name = draft.name.trim();

    if (!name) {
      errorMessage.value = '请填写知识库名称';
      return false;
    }

    if (knowledgeBases.value.some((item) => item.name === name)) {
      errorMessage.value = '已存在同名知识库';
      return false;
    }

    resetFeedback();
    saving.value = true;

    const payload: KnowledgeBaseDraft = {
      name,
      category: draft.category.trim() || '未分类',
      description: draft.description.trim() || '暂无描述，可在创建后上传文档并补充说明。',
    };

    let created: KnowledgeBase;

    try {
      created = await knowledgeApi.createKnowledgeBase(payload);
    } catch {
      created = {
        id: nextLocalId(),
        documents: 0,
        status: 'building',
        updatedAt: '待上传',
        ...payload,
      };
    }

    knowledgeBases.value = [created, ...knowledgeBases.value];

    if (created.status === 'ready') {
      selectedKbIds.value = [...selectedKbIds.value, created.id];
    }

    persist();
    saving.value = false;
    notice.value = `知识库「${created.name}」已创建，请继续上传文档`;
    return true;
  };

  /** 文档上传成功后乐观更新文档数；最终状态以后端刷新结果为准。 */
  const markDocumentsUploaded = (knowledgeBaseId: number, count: number) => {
    const index = knowledgeBases.value.findIndex((item) => item.id === knowledgeBaseId);
    if (index < 0) return;

    const current = knowledgeBases.value[index];
    knowledgeBases.value.splice(index, 1, {
      ...current,
      documents: current.documents + count,
      status: 'syncing',
      updatedAt: formatToday(),
    });

    persist();
  };

  /** 删除知识库：后端失败时也移除本地记录，避免界面与操作不一致。 */
  const removeKnowledgeBase = async (id: number) => {
    const target = knowledgeBases.value.find((item) => item.id === id);
    if (!target) return false;

    resetFeedback();
    saving.value = true;

    try {
      await knowledgeApi.deleteKnowledgeBase(id);
    } catch {
      // 后端未就绪时仅在本地删除。
    }

    knowledgeBases.value = knowledgeBases.value.filter((item) => item.id !== id);
    pruneSelection();
    persist();
    saving.value = false;
    notice.value = `知识库「${target.name}」已删除`;
    return true;
  };

  const toggleKnowledgeBase = (id: number) => {
    selectedKbIds.value = selectedKbIds.value.includes(id)
      ? selectedKbIds.value.filter((item) => item !== id)
      : [...selectedKbIds.value, id];
    persist();
  };

  const selectAllKnowledgeBases = () => {
    selectedKbIds.value = knowledgeBases.value.map((item) => item.id);
    persist();
  };

  const clearSelectedKnowledgeBases = () => {
    selectedKbIds.value = [];
    persist();
  };

  return {
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
    selectedKnowledgeBases,
    toggleKnowledgeBase,
  };
};
