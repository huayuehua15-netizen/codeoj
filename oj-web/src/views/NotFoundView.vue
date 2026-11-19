<template>
  <div class="nf-page">
    <div class="nf-frame">
      <p class="kicker">404 / NOT FOUND</p>
      <div class="nf-code" aria-hidden="true">404</div>
      <h1 class="nf-title">页面不存在</h1>
      <p class="nf-desc">
        地址可能输错了，也可能这道题或这场比赛已被移除。<br />
        回到题库继续。
      </p>
      <div class="nf-actions">
        <el-button type="primary" @click="router.replace('/problems')">返回题库</el-button>
        <el-button @click="router.back()">回到上一页</el-button>
      </div>
      <p v-if="attempted" class="nf-attempted">
        <span>REQUEST</span>
        <code class="mono">{{ attempted }}</code>
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
  width: min(540px, 100%);
  padding: 32px 34px 28px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 12px;
  box-shadow: var(--oj-shadow-sm);
}
.nf-code {
  margin-top: 6px;
  font-family: var(--oj-font-mono);
  font-size: 88px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: -0.07em;
  color: var(--oj-gray-200);
  user-select: none;
}
.nf-title {
  margin: 14px 0 7px;
  font-size: 24px;
  font-weight: 750;
  line-height: 1.3;
  letter-spacing: -0.03em;
  color: var(--oj-text);
}
.nf-desc {
  margin: 0 0 20px;
  font-size: 12px;
  line-height: 1.8;
  color: var(--oj-text-sub);
}
.nf-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.nf-attempted {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 22px 0 0;
  padding-top: 12px;
  border-top: 1px solid var(--oj-border);
  font-family: var(--oj-font-mono);
  font-size: 8px;
  letter-spacing: 0.08em;
  color: var(--oj-text-weak);
  word-break: break-all;
}
.nf-attempted code {
  padding: 3px 6px;
  border-radius: 4px;
  background: var(--oj-gray-100);
  font-family: var(--oj-font-mono);
  font-size: 10px;
  letter-spacing: 0;
  color: var(--oj-text-sub);
}

@media (max-width: 560px) {
  .nf-frame {
    padding: 24px 20px 22px;
  }
  .nf-code {
    font-size: 64px;
  }
  .nf-title {
    font-size: 21px;
  }
}
</style>
