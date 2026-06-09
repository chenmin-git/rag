<script setup>
import { computed, onMounted, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { api } from '../services/api'
import { feedbackLabel, formatTime } from '../utils/options'

const feedback = ref([])
const loading = ref(false)
const keyword = ref('')

const filteredFeedback = computed(() => feedback.value.filter((item) => {
  const text = keyword.value.trim()
  if (!text) return true
  return [item.question, item.answer, item.comment, item.userName, feedbackLabel(item.type)]
    .some((value) => value?.includes(text))
}))

function feedbackType(type) {
  return type === 'HELPFUL' ? 'success' : type === 'SOURCE_WRONG' ? 'danger' : 'warning'
}

async function load() {
  loading.value = true
  try {
    feedback.value = await api.feedbackList()
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
          <span><el-icon><Search /></el-icon> 反馈检索</span>
          <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        </div>
      </template>
      <el-input v-model="keyword" clearable placeholder="按问题、用户、反馈内容检索" />
    </el-card>

    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>问答反馈</span>
          <el-tag effect="plain">共 {{ filteredFeedback.length }} 条</el-tag>
        </div>
      </template>
      <el-table :data="filteredFeedback" border stripe v-loading="loading">
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="130">
          <template #default="{ row }">
            <el-tag :type="feedbackType(row.type)">{{ feedbackLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userName" label="用户" width="130" />
        <el-table-column prop="question" label="问题" min-width="260" show-overflow-tooltip />
        <el-table-column prop="comment" label="备注" min-width="240" show-overflow-tooltip />
        <el-table-column label="问答记录" width="110">
          <template #default="{ row }">#{{ row.qaRecordId }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </section>
</template>
