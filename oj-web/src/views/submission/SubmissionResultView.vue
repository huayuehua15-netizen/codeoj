<template>
  <div class="page-container">
    <div class="card">
      <template v-if="detail">
        <h2>
          提交 #{{ detail.id }}
          <el-tag :type="statusType(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
        </h2>
        <p class="meta">
          {{ detail.problemTitle || `题目 ${detail.problemId}` }} ｜ {{ detail.language }}
          ｜ 耗时 {{ detail.timeUsedMs ?? '-' }}ms ｜ 内存 {{ detail.memoryUsedKb ?? '-' }}KB
        </p>
        <el-alert v-if="detail.errorInfo" :title="detail.errorInfo" type="error" :closable="false" class="error" />

        <!-- 用例级结果表格（论文 5.7：用例序号、状态、耗时、内存） -->
        <h3>用例结果</h3>
        <el-table :data="detail.cases || []" v-loading="loading">
          <el-table-column prop="caseIndex" label="#" width="70" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ row.statusDesc }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="timeUsedMs" label="耗时(ms)" />
          <el-table-column prop="memoryUsedKb" label="内存(KB)" />
        </el-table>

        <template v-if="canViewCode">
          <h3>提交代码</h3>
          <pre class="code-view">{{ detail.code }}</pre>
        </template>
      </template>
      <el-empty v-else-if="!loading" description="提交不存在或无权查看" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getSubmission, type SubmissionDetail } from '@/api/submission'
import { useUserStore } from '@/stores/user'
import { statusLabel, statusType } from '@/utils/status'

const route = useRoute()
const userStore = useUserStore()

const detail = ref<SubmissionDetail>()
const loading = ref(false)

// 他人代码仅本人与管理员可查（论文 4.7）
const canViewCode = computed(
  () => detail.value && (detail.value.userId === userStore.userId || userStore.isAdmin),
)

onMounted(async () => {
  loading.value = true
  try {
    detail.value = await getSubmission(route.params.id as string)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.meta {
  color: #909399;
  font-size: 13px;
}
.error {
  margin: 12px 0;
}
.code-view {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 6px;
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
