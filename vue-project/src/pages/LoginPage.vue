<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { Loader2, LockKeyhole, LogIn, ShieldCheck, Sparkles, UserRound } from 'lucide-vue-next';
import { useAuth } from '../composables/useAuth';

const { login } = useAuth();

const form = reactive({
  username: '',
  password: '',
});
const loading = ref(false);
const errorMessage = ref('');

const canSubmit = computed(() => form.username.trim().length > 0 && form.password.trim().length > 0 && !loading.value);

const handleSubmit = async () => {
  if (!canSubmit.value) {
    errorMessage.value = '请输入用户名和密码';
    return;
  }

  errorMessage.value = '';
  loading.value = true;

  try {
    await login({
      username: form.username.trim(),
      password: form.password,
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请稍后重试';
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <main class="auth-shell">
    <section class="auth-panel">
      <div class="auth-brand">
        <div class="brand-mark">
          <LockKeyhole :size="20" />
        </div>
        <div>
          <p class="eyebrow">QAU SMARTAI</p>
          <h1>系统登录</h1>
        </div>
      </div>

      <div class="auth-welcome">
        <span><Sparkles :size="15" />校园知识服务台</span>
        <p>请输入后台账号后进入智能问答系统。</p>
      </div>

      <form class="auth-form" @submit.prevent="handleSubmit">
        <label class="auth-field">
          <span><UserRound :size="16" />用户名</span>
          <input v-model="form.username" type="text" autocomplete="username" placeholder="请输入用户名" />
        </label>

        <label class="auth-field">
          <span><LockKeyhole :size="16" />密码</span>
          <input
            v-model="form.password"
            type="password"
            autocomplete="current-password"
            placeholder="请输入密码"
          />
        </label>

        <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>

        <button class="auth-submit" type="submit" :disabled="!canSubmit">
          <Loader2 v-if="loading" :size="18" class="spin" />
          <LogIn v-else :size="18" />
          <span>{{ loading ? '登录中' : '登录' }}</span>
        </button>
      </form>

      <div class="auth-footnote">
        <ShieldCheck :size="15" />
        <span>统一身份认证与权限保护</span>
      </div>
    </section>
  </main>
</template>
