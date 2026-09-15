import { computed, ref } from 'vue';
import { knowledgeApi } from '../api/knowledge';
import type { KnowledgeBase, KnowledgeBaseDraft } from '../types';

// 旧版本把整张知识库表缓存在本地，启动时清掉，避免与后端真实数据混淆。
const LEGACY_LIST_STORAGE_KEY = 'qau-smartqa-knowledge-bases';
// 选中的知识库范围后端接口尚未实现，暂时继续存本地。
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

const getErrorMessage = (error: unknown) =>
  error instanceof Error && error.message ? error.message : '请求失败，请稍后重试';

try {
  localStorage.removeItem(LEGACY_LIST_STORAGE_KEY);
} catch {
  // 存储不可用时忽略。
}

const cachedSelected = readJson<number[]>(SELECTED_STORAGE_KEY);

// 模块级状态：侧边栏、问答页与知识库管理页共享同一份数据。
const knowledgeBases = ref<KnowledgeBase[]>([]);
const selectedKbIds = ref<number[]>(Array.isArray(cachedSelected) ? cachedSelected : []);
const loading = ref(false);
const saving = ref(false);
const errorMessage = ref('');
const notice = ref('');
const loadedFromServer = ref(false);

const padTime = (value: number) => String(value).padStart(2, '0');

/** 上传后的乐观更新文案，格式与后端 updatedAt 保持一致：MM-dd HH:mm。 */
const formatNow = () => {
  const now = new Date();
  return `${padTime(now.getMonth() + 1)}-${padTime(now.getDate())} ${padTime(now.getHours())}:${padTime(now.getMinutes())}`;
};

const persistSelection = () => writeJson(SELECTED_STORAGE_KEY, selectedKbIds.value);

const pruneSelection = () => {
  const ids = new Set(knowledgeBases.value.map((item) => item.id));
  selectedKbIds.value = selectedKbIds.value.filter((id) => ids.has(id));
};

/** 首次拿到数据时默认启用全部可用知识库，保证问答页开箱就有检索范围。 */
const selectReadyBases = () => {
  selectedKbIds.value = knowledgeBases.value
    .filter((item) => item.status === 'ready')
    .map((item) => item.id);
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

  /** 拉取知识库列表：后端是唯一数据源，失败时给出提示而不是回落假数据。 */
  const loadKnowledgeBases = async (force = false) => {
    if (loading.value || (loadedFromServer.value && !force)) return;

    loading.value = true;

    try {
      const list = await knowledgeApi.getKnowledgeBases();
      knowledgeBases.value = list ?? [];
      loadedFromServer.value = true;
      pruneSelection();

      if (!selectedKbIds.value.length) {
        selectReadyBases();
      }

      persistSelection();
      resetFeedback();
    } catch (error) {
      knowledgeBases.value = [];
      errorMessage.value = `知识库列表加载失败：${getErrorMessage(error)}`;
    } finally {
      loading.value = false;
    }
  };

  /** 新建知识库：只有后端创建成功才写入本地状态，失败时如实提示。 */
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

    try {
      const created = await knowledgeApi.createKnowledgeBase(payload);
      knowledgeBases.value = [created, ...knowledgeBases.value];

      if (created.status === 'ready') {
        selectedKbIds.value = [...selectedKbIds.value, created.id];
        persistSelection();
      }

      notice.value = `知识库「${created.name}」已创建，请继续上传文档`;
      return true;
    } catch (error) {
      errorMessage.value = `知识库创建失败：${getErrorMessage(error)}`;
      return false;
    } finally {
      saving.value = false;
    }
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
      updatedAt: formatNow(),
    });
  };

  /** 删除知识库：后端删除失败时保留记录，避免界面与数据库不一致。 */
  const removeKnowledgeBase = async (id: number) => {
    const target = knowledgeBases.value.find((item) => item.id === id);
    if (!target) return false;

    resetFeedback();
    saving.value = true;

    try {
      await knowledgeApi.deleteKnowledgeBase(id);
      knowledgeBases.value = knowledgeBases.value.filter((item) => item.id !== id);
      pruneSelection();
      persistSelection();
      notice.value = `知识库「${target.name}」已删除`;
      return true;
    } catch (error) {
      errorMessage.value = `知识库删除失败：${getErrorMessage(error)}`;
      return false;
    } finally {
      saving.value = false;
    }
  };

  const toggleKnowledgeBase = (id: number) => {
    selectedKbIds.value = selectedKbIds.value.includes(id)
      ? selectedKbIds.value.filter((item) => item !== id)
      : [...selectedKbIds.value, id];
    persistSelection();
  };

  const selectAllKnowledgeBases = () => {
    selectedKbIds.value = knowledgeBases.value.map((item) => item.id);
    persistSelection();
  };

  const clearSelectedKnowledgeBases = () => {
    selectedKbIds.value = [];
    persistSelection();
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
