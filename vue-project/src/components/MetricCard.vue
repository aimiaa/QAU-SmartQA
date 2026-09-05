<script setup lang="ts">
defineProps<{
  label: string;
  value: string;
  detail: string;
  tone: 'primary' | 'green' | 'orange';
}>();
</script>

<template>
  <article class="metric-card" :class="`tone-${tone}`">
    <span>{{ label }}</span>
    <strong>{{ value }}</strong>
    <small>{{ detail }}</small>
  </article>
</template>
