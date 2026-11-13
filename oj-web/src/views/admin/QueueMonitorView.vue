<template>
  <div class="page-container">
    <div class="card">
      <h2>判题队列监控</h2>
      <el-descriptions :column="3" border style="margin-top: 12px">
        <el-descriptions-item label="队列积压">{{ status?.backlog ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="今日判题量">{{ status?.judgedToday ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="判题实例数">{{ status?.judgeInstances ?? '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-button style="margin-top: 16px" :loading="loading" @click="load">刷新</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getQueueStatus } from '@/api/admin'

const status = ref<Awaited<ReturnType<typeof getQueueStatus>>>()
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    status.value = await getQueueStatus()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
