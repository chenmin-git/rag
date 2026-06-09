<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  ChatDotRound,
  Coin,
  Collection,
  DataAnalysis,
  Files,
  Refresh,
  TrendCharts,
  Warning
} from '@element-plus/icons-vue'
import EChart from '../components/EChart.vue'
import { api } from '../services/api'
import { useSessionStore } from '../stores/session'
import { feedbackLabel, formatTime } from '../utils/options'

const dashboard = ref(null)
const loading = ref(false)
const session = useSessionStore()

const roleBrief = computed(() => ({
  SYSTEM_ADMIN: {
    eyebrow: '系统总览',
    title: '全校知识库运行态势'
  },
  DEPARTMENT_ADMIN: {
    eyebrow: '部门知识运营',
    title: `${session.user?.department || '本部门'}知识维护看板`
  },
  TEACHER: {
    eyebrow: '教学服务视图',
    title: '教学与办事知识查询概览'
  },
  STUDENT: {
    eyebrow: '学生服务视图',
    title: '校园事项查询入口'
  }
}[session.user?.role] || {
  eyebrow: '工作台',
  title: '校园知识库运行概览'
}))

const metrics = computed(() => [
  { label: metricLabel('documents'), value: dashboard.value?.documentCount ?? 0, icon: Collection, type: 'primary' },
  { label: metricLabel('chunks'), value: dashboard.value?.chunkCount ?? 0, icon: Coin, type: 'success' },
  { label: metricLabel('qa'), value: dashboard.value?.qaCount ?? 0, icon: ChatDotRound, type: 'warning' },
  { label: metricLabel('latency'), value: `${dashboard.value?.averageLatencyMs ?? 0} 毫秒`, icon: DataAnalysis, type: dashboard.value?.failedTaskCount ? 'danger' : 'info' }
])

const departmentEntries = computed(() => Object.entries(dashboard.value?.departmentDistribution || {}))
const feedbackEntries = computed(() => Object.entries(dashboard.value?.feedbackDistribution || {}))
const recentQuestions = computed(() => dashboard.value?.recentQuestions || [])
const recentDocuments = computed(() => dashboard.value?.recentDocuments || [])
const departmentOption = computed(() => ({
  color: ['#1f6f78'],
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { top: 24, left: 12, right: 12, bottom: 0, containLabel: true },
  xAxis: {
    type: 'category',
    data: departmentEntries.value.map(([name]) => name),
    axisLabel: { color: '#64748b', interval: 0, rotate: departmentEntries.value.length > 4 ? 20 : 0 }
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#edf2f7' } },
    axisLabel: { color: '#64748b' }
  },
  series: [{
    name: '文档数',
    type: 'bar',
    barWidth: 28,
    data: departmentEntries.value.map(([, count]) => count),
    itemStyle: { borderRadius: [5, 5, 0, 0] }
  }]
}))
const feedbackOption = computed(() => {
  const data = feedbackEntries.value.length
    ? feedbackEntries.value.map(([type, count]) => ({ name: feedbackLabel(type), value: count }))
    : [{ name: '暂无反馈', value: 1 }]
  return {
    color: ['#1f6f78', '#2f7d57', '#b98024', '#b34032', '#2f5f9c'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, left: 'center', textStyle: { color: '#64748b' } },
    series: [{
      name: '反馈类型',
      type: 'pie',
      radius: ['48%', '70%'],
      center: ['50%', '43%'],
      avoidLabelOverlap: true,
      label: { formatter: '{b}\n{c}', color: '#374553' },
      data
    }]
  }
})
const trendOption = computed(() => {
  const data = recentQuestions.value.slice().reverse()
  const labels = data.map((item) => formatTime(item.createdAt).slice(5) || '记录')
  return {
    color: ['#2f5f9c'],
    tooltip: { trigger: 'axis' },
    grid: { top: 26, left: 12, right: 18, bottom: 0, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisLabel: { color: '#64748b' }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#edf2f7' } },
      axisLabel: { color: '#64748b' }
    },
    series: [{
      name: '响应耗时',
      type: 'line',
      smooth: true,
      symbolSize: 7,
      areaStyle: { color: 'rgba(47,95,156,.12)' },
      data: data.map((item) => item.latencyMs || 0)
    }]
  }
})
const healthOption = computed(() => {
  const total = Math.max(1, dashboard.value?.documentCount || 0)
  const failed = dashboard.value?.failedTaskCount || 0
  const healthy = Math.max(0, total - failed)
  return {
    color: ['#2f7d57', '#b34032'],
    tooltip: { trigger: 'item' },
    series: [{
      name: '处理状态',
      type: 'pie',
      radius: ['55%', '76%'],
      center: ['50%', '50%'],
      label: { formatter: '{b}\n{c}', color: '#374553' },
      data: [
        { name: '已入库', value: healthy },
        { name: '失败任务', value: failed }
      ]
    }]
  }
})
const fileTypeRows = computed(() => {
  const map = new Map()
  recentDocuments.value.forEach((doc) => {
    const type = (doc.fileType || 'txt').toUpperCase()
    map.set(type, (map.get(type) || 0) + 1)
  })
  return Array.from(map.entries()).map(([type, count]) => ({ type, count }))
})

function metricLabel(key) {
  const role = session.user?.role
  const labels = {
    STUDENT: {
      documents: '可查资料',
      chunks: '知识片段',
      qa: '问答记录',
      latency: '平均响应'
    },
    TEACHER: {
      documents: '教学资料',
      chunks: '知识片段',
      qa: '近期问答',
      latency: '响应耗时'
    },
    DEPARTMENT_ADMIN: {
      documents: '部门文档',
      chunks: '切片总量',
      qa: '问答次数',
      latency: '处理响应'
    }
  }
  return labels[role]?.[key] || {
    documents: '文档总数',
    chunks: '知识切片',
    qa: '问答次数',
    latency: '平均响应'
  }[key]
}

async function load() {
  loading.value = true
  try {
    dashboard.value = await api.dashboard()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="page-stack">
    <div class="dashboard-hero">
      <div>
        <span>{{ roleBrief.eyebrow }}</span>
        <h2>{{ roleBrief.title }}</h2>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新数据</el-button>
    </div>

    <el-row :gutter="16">
      <el-col v-for="metric in metrics" :key="metric.label" :lg="6" :md="12" :sm="24">
        <el-card shadow="never" class="metric-card">
          <el-icon :class="`metric-icon ${metric.type}`"><component :is="metric.icon" /></el-icon>
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :lg="14" :md="24">
        <el-card shadow="never" class="page-card">
          <template #header><div class="card-header"><span><el-icon><Files /></el-icon> 部门知识分布</span></div></template>
          <EChart :option="departmentOption" height="286px" />
        </el-card>
      </el-col>

      <el-col :lg="10" :md="24">
        <el-card shadow="never" class="page-card">
          <template #header><div class="card-header"><span><el-icon><Warning /></el-icon> 反馈类型占比</span></div></template>
          <EChart :option="feedbackOption" height="286px" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :lg="14" :md="24">
        <el-card shadow="never" class="page-card">
          <template #header><div class="card-header"><span><el-icon><TrendCharts /></el-icon> 最近问答响应趋势</span></div></template>
          <EChart :option="trendOption" height="260px" />
        </el-card>
      </el-col>
      <el-col :lg="10" :md="24">
        <el-card shadow="never" class="page-card">
          <template #header><div class="card-header"><span><el-icon><DataAnalysis /></el-icon> 文档处理健康度</span></div></template>
          <EChart :option="healthOption" height="260px" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :lg="15" :md="24">
        <el-card shadow="never" class="page-card">
          <template #header><div class="card-header"><span>最近文档</span></div></template>
          <el-table :data="dashboard?.recentDocuments || []" border stripe>
            <el-table-column prop="originalName" label="文档名称" min-width="240" show-overflow-tooltip />
            <el-table-column prop="department" label="部门" width="130" />
            <el-table-column prop="chunkCount" label="切片" width="90" />
            <el-table-column label="上传时间" width="160">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :lg="9" :md="24">
        <el-card shadow="never" class="page-card">
          <template #header><div class="card-header"><span>文件类型统计</span></div></template>
          <el-table :data="fileTypeRows" border>
            <el-table-column prop="type" label="类型" />
            <el-table-column prop="count" label="数量" width="90" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>
