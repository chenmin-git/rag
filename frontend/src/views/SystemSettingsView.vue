<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, Tools } from '@element-plus/icons-vue'
import { api } from '../services/api'

const settings = ref([])
const draft = ref({})
const loading = ref(false)
const sparkSettings = computed(() => settings.value.filter((item) => item.configKey.startsWith('spark.')))
const ragSettings = computed(() => settings.value.filter((item) => item.configKey.startsWith('rag.') || item.configKey.startsWith('embedding.') || item.configKey.startsWith('milvus.')))
const sparkReady = computed(() => draft.value['spark.enabled'] === 'true' && Boolean(draft.value['spark.apiPassword']?.trim()))
const configLabels = {
  'spark.enabled': '是否启用',
  'spark.provider': '服务厂商',
  'spark.protocol': '接入方式',
  'spark.endpoint': '接口地址',
  'spark.model': '模型名称',
  'spark.apiPassword': '接口密钥',
  'spark.temperature': '生成温度',
  'spark.maxTokens': '最大输出长度',
  'rag.topK': '默认召回条数',
  'rag.similarityThreshold': '相似度阈值',
  'rag.maxContextChars': '上下文长度',
  'milvus.collection': '向量集合名称',
  'embedding.dimension': '向量维度'
}

async function load() {
  settings.value = await api.settings()
  draft.value = Object.fromEntries(settings.value.map((item) => [item.configKey, item.configValue]))
}

function configLabel(key) {
  return configLabels[key] || key
}

async function save() {
  loading.value = true
  try {
    settings.value = await api.updateSettings(draft.value)
    draft.value = Object.fromEntries(settings.value.map((item) => [item.configKey, item.configValue]))
    ElMessage.success('配置已保存')
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <el-row :gutter="16">
    <el-col :lg="12" :md="24">
      <el-card shadow="never" class="page-card">
        <template #header>
          <div class="card-header">
            <span><el-icon><Setting /></el-icon> 大模型服务</span>
            <el-tag :type="sparkReady ? 'success' : 'warning'" effect="plain">
              {{ sparkReady ? '已配置' : '未启用' }}
            </el-tag>
          </div>
        </template>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          :title="sparkReady ? '当前配置会调用大模型兼容接口。' : '填写接口密钥并开启服务后，智能问答会调用大模型服务。'"
          class="block-alert"
        />
        <el-form label-position="top">
          <el-form-item v-for="item in sparkSettings" :key="item.id" :label="configLabel(item.configKey)">
            <el-switch
              v-if="item.configKey === 'spark.enabled'"
              v-model="draft[item.configKey]"
              active-value="true"
              inactive-value="false"
              active-text="开启"
              inactive-text="关闭"
            />
            <el-input
              v-else
              v-model="draft[item.configKey]"
              :type="item.configKey.includes('apiPassword') ? 'password' : 'text'"
              show-password
            />
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>

    <el-col :lg="12" :md="24">
      <el-card shadow="never" class="page-card">
        <template #header>
          <div class="card-header">
            <span><el-icon><Tools /></el-icon> 检索参数</span>
            <el-button type="primary" :loading="loading" @click="save">保存配置</el-button>
          </div>
        </template>
        <el-form label-position="top">
          <el-form-item v-for="item in ragSettings" :key="item.id" :label="configLabel(item.configKey)">
            <el-input v-model="draft[item.configKey]" />
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
  </el-row>
</template>
