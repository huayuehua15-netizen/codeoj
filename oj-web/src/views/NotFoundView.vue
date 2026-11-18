<template>
  <div class="nf-page">
    <div class="nf-card">
      <div class="nf-code" aria-hidden="true">
        <span class="br">&lt;</span><span class="num">404</span><span class="br">/&gt;</span>
      </div>
      <h1 class="nf-title">这个页面不存在</h1>
      <p class="nf-desc">
        地址可能输错了，也可能这道题 / 这场比赛已经被移除。<br />
        回到题库继续刷题吧。
      </p>
      <div class="nf-actions">
        <el-button type="primary" @click="router.replace('/problems')">返回题库</el-button>
        <el-button text @click="router.back()">回到上一页</el-button>
      </div>
      <p v-if="attempted" class="nf-attempted">
        你访问的是 <code class="mono">{{ attempted }}</code>
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
  padding: 40px 20px;
}
.nf-card {
  max-width: 480px;
  text-align: center;
}
.nf-code {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
  font-family: var(--oj-font-mono);
  font-size: 56px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -1px;
  color: var(--oj-primary);
}
.nf-code .br {
  color: var(--oj-indigo-400);
  font-weight: 700;
  opacity: 0.65;
}
.nf-code .num {
  background: var(--oj-gradient-brand);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  padding: 0 2px;
}
.nf-title {
  margin: 22px 0 10px;
  font-size: 20px;
  font-weight: 700;
  color: var(--oj-gray-900);
}
.nf-desc {
  margin: 0 0 26px;
  font-size: 13.5px;
  line-height: 1.9;
  color: var(--oj-text-sub);
}
.nf-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
}
.nf-attempted {
  margin: 22px 0 0;
  font-size: 12px;
  color: var(--oj-text-weak);
  word-break: break-all;
}
.nf-attempted code {
  padding: 2px 6px;
  border-radius: var(--oj-radius-sm);
  background: var(--oj-gray-50);
  border: 1px solid var(--oj-border);
  color: var(--oj-text-sub);
}
</style>
