<template>
  <div class="nf-page">
    <div class="nf-frame">
      <p class="nf-code">404</p>
      <h1 class="nf-title">页面不存在</h1>
      <p class="nf-desc">
        地址可能输错了，也可能这道题或这场比赛已被移除。回到题库继续。
      </p>
      <div class="nf-actions">
        <el-button type="primary" @click="router.replace('/problems')">返回题库</el-button>
        <el-button @click="router.back()">回到上一页</el-button>
      </div>
      <p v-if="attempted" class="nf-attempted">
        <span>访问地址</span>
        <code>{{ attempted }}</code>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 只在确实是"用户敲错地址"时展示，避免把上下文泄露得太啰嗦
const attempted = computed(() => (route.fullPath.length <= 60 ? route.fullPath : ''))
</script>

<style scoped>
.nf-page {
  min-height: calc(100vh - 220px);
  display: grid;
  place-items: center;
  padding: 48px 20px;
}
.nf-frame {
  width: min(480px, 100%);
  padding: 0;
}
.nf-code {
  margin: 0 0 10px;
  font-family: var(--oj-font-mono);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--oj-text-weak);
}
.nf-title {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 750;
  line-height: 1.3;
  letter-spacing: -0.03em;
  color: var(--oj-text);
}
.nf-desc {
  margin: 0 0 22px;
  font-size: 12.5px;
  line-height: 1.9;
  color: var(--oj-text-sub);
}
.nf-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.nf-attempted {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 26px 0 0;
  padding-top: 12px;
  border-top: 1px solid var(--oj-border);
  font-size: 10px;
  color: var(--oj-text-weak);
  word-break: break-all;
}
.nf-attempted code {
  font-family: var(--oj-font-mono);
  font-size: 10px;
  color: var(--oj-text-sub);
}

@media (max-width: 560px) {
  .nf-title {
    font-size: 23px;
  }
}
</style>
