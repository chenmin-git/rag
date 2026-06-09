<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DocumentAdd, UploadFilled } from '@element-plus/icons-vue'
import { api } from '../services/api'
import { departmentOptions, roleOptions } from '../utils/options'

const router = useRouter()
const loading = ref(false)
const uploadRef = ref(null)
const uploadForm = ref({ department: '学生工作处', visibility: 'PUBLIC', file: null })
const textForm = ref({
  title: '学院办事补充说明',
  department: '学生工作处',
  visibility: 'PUBLIC',
  content: '请在这里录入新的校园政策、办事流程或常见问题。系统会自动切片并写入向量库。'
})

function onUploadChange(file) {
  uploadForm.value.file = file.raw || file
}

function removeFile() {
  uploadForm.value.file = null
}

async function uploadDocument() {
  if (!uploadForm.value.file) {
    ElMessage.warning('请选择需要上传的文件')
    return
  }
  loading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadForm.value.file)
    formData.append('department', uploadForm.value.department)
    formData.append('visibility', uploadForm.value.visibility)
    await api.uploadDocument(formData)
    ElMessage.success('文件已上传并写入知识库')
    router.push('/knowledge')
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    loading.value = false
  }
}

async function createTextDocument() {
  if (!textForm.value.title.trim() || !textForm.value.content.trim()) {
    ElMessage.warning('标题和正文不能为空')
    return
  }
  loading.value = true
  try {
    await api.createTextDocument(textForm.value)
    ElMessage.success('资料已保存并向量化')
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-row :gutter="16">
    <el-col :lg="10" :md="24">
      <el-card shadow="never" class="page-card">
        <template #header>
          <div class="card-header">
            <span><el-icon><UploadFilled /></el-icon> 文件上传</span>
            <el-tag type="info" effect="plain">支持常见文档与文本格式</el-tag>
          </div>
        </template>

        <el-form label-position="top" :model="uploadForm">
          <el-form-item label="所属部门">
            <el-select v-model="uploadForm.department" class="full-control">
              <el-option v-for="dept in departmentOptions" :key="dept" :label="dept" :value="dept" />
            </el-select>
          </el-form-item>
          <el-form-item label="可见范围">
            <el-select v-model="uploadForm.visibility" class="full-control">
              <el-option v-for="option in roleOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="资料文件">
            <el-upload
              ref="uploadRef"
              drag
              :limit="1"
              :auto-upload="false"
              :on-change="onUploadChange"
              :on-remove="removeFile"
              class="full-control"
            >
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择</em></div>
              <template #tip>
                <div class="el-upload__tip">上传后系统会解析、切片并写入向量库。</div>
              </template>
            </el-upload>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="uploadDocument">上传入库</el-button>
            <el-button @click="router.push('/knowledge')">返回列表</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>

    <el-col :lg="14" :md="24">
      <el-card shadow="never" class="page-card">
        <template #header>
          <div class="card-header">
            <span><el-icon><DocumentAdd /></el-icon> 手动录入</span>
            <el-tag type="success" effect="plain">自动切片向量化</el-tag>
          </div>
        </template>

        <el-form label-position="top" :model="textForm">
          <el-form-item label="标题">
            <el-input v-model="textForm.title" placeholder="请输入资料标题" />
          </el-form-item>
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="部门">
                <el-select v-model="textForm.department" class="full-control">
                  <el-option v-for="dept in departmentOptions" :key="dept" :label="dept" :value="dept" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="可见范围">
                <el-select v-model="textForm.visibility" class="full-control">
                  <el-option v-for="option in roleOptions" :key="option.value" :label="option.label" :value="option.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="正文内容">
            <el-input v-model="textForm.content" type="textarea" :rows="13" resize="vertical" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="createTextDocument">保存并向量化</el-button>
            <el-button @click="textForm.content = ''">清空正文</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
  </el-row>
</template>
