<script setup lang="ts">
import { useAuth } from './composables/useAuth';
import LoginPage from './pages/LoginPage.vue';
import CampusAssistantPage from './pages/CampusAssistantPage.vue';

const { isAuthenticated } = useAuth();
</script>

<template>
  <LoginPage v-if="!isAuthenticated" />
  <CampusAssistantPage v-else />
</template>
