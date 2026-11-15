<template>
  <div class="page-container">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="problemId" placeholder="题目 ID" style="width: 120px" clearable @keyup.enter="load" />
        <el-select v-model="status" placeholder="判题状态" style="width: 140px" clearable @change="load">
          <el-option v-for="(v, k) in STATUS_MAP" :key="k" :label="v.label" :value="Number(k)" />
        </el-select>
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 360px"
        />
        <el-button type="primary" @click="load">筛选</el-button>
      </div>
      <el-table :data="records" v-loading="loading">
        <el-table-column prop="id" label="提交编号" width="100" />
        <el-table-column prop="problemTitle" label="题目" min-width="240">
          <template #default="{ row }">
            <router-link :to="`/problems/${row.problemId}`">{{ row.problemTitle || row.problemId }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="language" label="语言" width="90" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="timeUsedMs" label="耗时(ms)" width="100" />
        <el-table-column prop="memoryUsedKb" label="内存(KB)" width="100" />
        <el-table-column prop="createdAt" label="提交时间" width="170" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/submissions/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { pageSubmissions, type SubmissionDetail } from '@/api/submission'
import { STATUS_MAP, statusLabel, statusType } from '@/utils/status'

const records = ref<SubmissionDetail[]>([])
const problemId = ref('')
const status = ref<number | undefined>()
const timeRange = ref<[string, string] | null>(null)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await pageSubmissions({
      problemId: problemId.value ? Number(problemId.value) : undefined,
      status: status.value,
      begin: timeRange.value?.[0],
      end: timeRange.value?.[1],
      page: page.value,
      size: size.value,
    })
    records.value = data.records || []
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
