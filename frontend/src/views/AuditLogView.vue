<script setup>
import { computed, onMounted, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { api } from '../services/api'
import { actionLabel, formatTime } from '../utils/options'

const logs = ref([])
const loading = ref(false)
const keyword = ref('')

const filteredLogs = computed(() => logs.value.filter((log) => {
  const text = keyword.value.trim()
  if (!text) return true
  return [log.userName, actionLabel(log.action), log.action, log.target, log.detail]
    .some((value) => value?.includes(text))
}))

async function load() {
  loading.value = true
  try {
    logs.value = await api.logs()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="page-stack">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><Search /></el-icon> 日志检索</span>
          <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        </div>
      </template>
      <el-input v-model="keyword" clearable placeholder="按用户、操作、对象或详情检索" />
    </el-card>

    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>操作日志</span>
          <el-tag effect="plain">共 {{ filteredLogs.length }} 条</el-tag>
        </div>
      </template>
      <el-table :data="filteredLogs" border stripe v-loading="loading">
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">{{ actionLabel(row.action) }}</template>
        </el-table-column>
        <el-table-column prop="userName" label="用户" width="130" />
        <el-table-column prop="target" label="对象" min-width="180" show-overflow-tooltip />
        <el-table-column prop="detail" label="详情" min-width="260" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP" width="150" />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </section>
</template>
