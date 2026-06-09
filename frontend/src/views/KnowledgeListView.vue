<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, DocumentAdd, Refresh, Search, View } from '@element-plus/icons-vue'
import { api } from '../services/api'
import { departmentOptions, statusLabel, visibilityLabel, formatTime } from '../utils/options'

const router = useRouter()
const documents = ref([])
const filter = ref({ keyword: '', department: '全部', status: '全部' })
const loading = ref(false)

const filteredDocuments = computed(() => documents.value.filter((document) => {
  const keyword = filter.value.keyword.trim()
  const matchKeyword = !keyword || document.originalName.includes(keyword) || document.summary?.includes(keyword)
  const matchDepartment = filter.value.department === '全部' || document.department === filter.value.department
  const matchStatus = filter.value.status === '全部' || document.status === filter.value.status
  return matchKeyword && matchDepartment && matchStatus
}))

async function load() {
  loading.value = true
  try {
    documents.value = await api.documents()
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filter.value = { keyword: '', department: '全部', status: '全部' }
}

function statusType(status) {
  if (status === 'INDEXED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

async function rebuild(document) {
  try {
    await api.rebuildDocument(document.id)
    ElMessage.success(`《${document.originalName}》索引已重建`)
    await load()
  } catch (err) {
    ElMessage.error(err.message)
  }
}

async function removeDocument(document) {
  try {
    await ElMessageBox.confirm(`确定删除《${document.originalName}》及其全部向量切片吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.deleteDocument(document.id)
    ElMessage.success('文档已删除')
    await load()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(err.message || '操作已取消')
  }
}

onMounted(load)
</script>

<template>
  <section class="page-stack">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><Search /></el-icon> 文档检索</span>
          <el-button type="primary" :icon="DocumentAdd" @click="router.push('/knowledge/create')">新增资料</el-button>
        </div>
      </template>

      <el-form :model="filter" label-position="top">
        <el-row :gutter="12">
          <el-col :lg="8" :md="12" :sm="24">
            <el-form-item label="关键词">
              <el-input v-model="filter.keyword" clearable placeholder="文档名称、摘要" />
            </el-form-item>
          </el-col>
          <el-col :lg="6" :md="12" :sm="24">
            <el-form-item label="所属部门">
              <el-select v-model="filter.department" class="full-control">
                <el-option label="全部" value="全部" />
                <el-option v-for="dept in departmentOptions" :key="dept" :label="dept" :value="dept" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="6" :md="12" :sm="24">
            <el-form-item label="处理状态">
              <el-select v-model="filter.status" class="full-control">
                <el-option label="全部" value="全部" />
                <el-option label="待解析" value="PENDING" />
                <el-option label="解析中" value="PARSING" />
                <el-option label="切片中" value="CHUNKING" />
                <el-option label="向量化中" value="VECTORIZING" />
                <el-option label="已入库" value="INDEXED" />
                <el-option label="入库失败" value="FAILED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="4" :md="12" :sm="24" class="form-actions-col">
            <el-button @click="resetFilter">重置</el-button>
            <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>文档列表</span>
          <el-tag effect="plain">共 {{ filteredDocuments.length }} 条</el-tag>
        </div>
      </template>

      <el-table :data="filteredDocuments" border stripe v-loading="loading">
        <el-table-column prop="originalName" label="文档名称" min-width="230" show-overflow-tooltip />
        <el-table-column prop="department" label="部门" width="130" />
        <el-table-column label="可见范围" width="140">
          <template #default="{ row }">{{ visibilityLabel(row.visibility) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="切片数" width="90" />
        <el-table-column prop="uploadUserName" label="上传人" width="120" />
        <el-table-column label="更新时间" width="160">
          <template #default="{ row }">{{ formatTime(row.updatedAt || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="router.push(`/knowledge/${row.id}`)">详情</el-button>
            <el-button link type="warning" :icon="Refresh" @click="rebuild(row)">重建</el-button>
            <el-button link type="danger" :icon="Delete" @click="removeDocument(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </section>
</template>
