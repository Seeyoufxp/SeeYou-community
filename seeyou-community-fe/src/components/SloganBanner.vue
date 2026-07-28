<template>
  <div class="slogan">{{ text }}</div>
</template>

<script setup>
defineProps({
  text: { type: String, default: '海内存知己，天涯若比邻' }
})
</script>

<style scoped>
.slogan {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  margin: 4px 0 20px;
  font-family: 'STKaiti', 'Kaiti SC', KaiTi, serif;
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 6px;
  color: var(--sy-text-main);
}
.slogan::before,
.slogan::after {
  content: '';
  width: 56px;
  height: 1px;
  background: var(--sy-border);
}
</style>
