<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, EditPen, Refresh, Tickets } from '@element-plus/icons-vue'
import { api } from '../services/api'
import { departmentOptions, roleOptions, statusLabel, visibilityLabel, formatTime } from '../utils/options'

const route = useRoute()
const router = useRouter()
const document = ref(null)
const chunks = ref([])
const metadata = ref({ department: '', visibility: 'PUBLIC' })
const loading = ref(false)
const saving = ref(false)

const statusType = computed(() => {
  if (document.value?.status === 'INDEXED') return 'success'
  if (document.value?.status === 'FAILED') return 'danger'
  return 'warning'
})

async function load() {
  loading.value = true
  try {
    const id = route.params.id
    const [detail, chunkList] = await Promise.all([api.document(id), api.chunks(id)])
    document.value = detail
    chunks.value = chunkList
    metadata.value = { department: detail.department, visibility: detail.visibility }
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    loading.value = false
  }
}

async function updateMetadata() {
  if (!document.value) return
  saving.value = true
  try {
    const updated = await api.updateDocument(document.value.id, metadata.value)
    document.value = updated
    chunks.value = await api.chunks(updated.id)
    ElMessage.success('文档元数据已更新，向量元信息已同步')
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    saving.value = false
  }
}

async function rebuild() {
  if (!document.value) return
  saving.value = true
  try {
    const updated = await api.rebuildDocument(document.value.id)
    document.value = updated
    chunks.value = await api.chunks(updated.id)
    ElMessage.success('文档索引已重建')
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    saving.value = false
  }
}

async function removeDocument() {
  if (!document.value) return
  try {
    await ElMessageBox.confirm(`确定删除《${document.value.originalName}》吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.deleteDocument(document.value.id)
    ElMessage.success('文档已删除')
    router.replace('/knowledge')
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(err.message || '操作已取消')
  }
}

onMounted(load)
</script>

<template>
  <section class="page-stack">
    <div class="page-toolbar">
      <el-button @click="router.push('/knowledge')">返回列表</el-button>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      <el-button :icon="Refresh" :loading="saving" :disabled="!document" @click="rebuild">重建索引</el-button>
      <el-button type="danger" plain :icon="Delete" :disabled="!document" @click="removeDocument">删除文档</el-button>
    </div>

    <el-row v-if="document" :gutter="16">
      <el-col :lg="15" :md="24">
        <el-card shadow="never" class="page-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Tickets /></el-icon> {{ document.originalName }}</span>
              <el-tag :type="statusType">{{ statusLabel(document.status) }}</el-tag>
            </div>
          </template>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="文档编号">#{{ document.id }}</el-descriptions-item>
            <el-descriptions-item label="文件类型">{{ document.fileType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属部门">{{ document.department }}</el-descriptions-item>
            <el-descriptions-item label="可见范围">{{ visibilityLabel(document.visibility) }}</el-descriptions-item>
            <el-descriptions-item label="上传人">{{ document.uploadUserName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="切片数量">{{ document.chunkCount }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatTime(document.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatTime(document.updatedAt) }}</el-descriptions-item>
          </el-descriptions>

          <el-alert v-if="document.failureReason" type="error" :title="document.failureReason" show-icon class="block-alert" />
          <el-divider content-position="left">内容摘要</el-divider>
          <p class="summary-text">{{ document.summary || '暂无摘要。' }}</p>
        </el-card>
      </el-col>

      <el-col :lg="9" :md="24">
        <el-card shadow="never" class="page-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><EditPen /></el-icon> 权限与元数据</span>
            </div>
          </template>
          <el-form label-position="top" :model="metadata">
            <el-form-item label="所属部门">
              <el-select v-model="metadata.department" class="full-control">
                <el-option v-for="dept in departmentOptions" :key="dept" :label="dept" :value="dept" />
              </el-select>
            </el-form-item>
            <el-form-item label="可见范围">
              <el-select v-model="metadata.visibility" class="full-control">
                <el-option v-for="option in roleOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="updateMetadata">保存修改</el-button>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>知识切片</span>
          <el-tag effect="plain">共 {{ chunks.length }} 条</el-tag>
        </div>
      </template>
      <el-table :data="chunks" border stripe v-loading="loading">
        <el-table-column prop="chunkNo" label="序号" width="90">
          <template #default="{ row }">#{{ row.chunkNo }}</template>
        </el-table-column>
        <el-table-column prop="pageNo" label="页码" width="100">
          <template #default="{ row }">第 {{ row.pageNo }} 页</template>
        </el-table-column>
        <el-table-column prop="vectorState" label="向量状态" width="120">
          <template #default="{ row }"><el-tag type="success">{{ statusLabel(row.vectorState) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="content" label="切片内容" min-width="520" />
      </el-table>
    </el-card>
  </section>
</template>
