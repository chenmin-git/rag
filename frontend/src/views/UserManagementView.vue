<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, UserFilled, User } from '@element-plus/icons-vue'
import { api } from '../services/api'
import { departmentOptions, roleLabel, userRoleOptions } from '../utils/options'

const users = ref([])
const loading = ref(false)
const form = ref({ username: '', password: '123456', displayName: '', role: 'STUDENT', department: '学生工作处' })
const editVisible = ref(false)
const saving = ref(false)
const editForm = ref({ id: null, username: '', displayName: '', role: 'STUDENT', department: '学生工作处', enabled: true })

async function load() {
  loading.value = true
  try {
    users.value = await api.users()
  } finally {
    loading.value = false
  }
}

async function createUser() {
  if (!form.value.username.trim() || !form.value.displayName.trim()) {
    ElMessage.warning('账号和姓名不能为空')
    return
  }
  await api.createUser(form.value)
  form.value = { username: '', password: '123456', displayName: '', role: 'STUDENT', department: '学生工作处' }
  ElMessage.success('用户已创建')
  await load()
}

async function toggleUser(user) {
  await api.updateUser(user.id, { enabled: !user.enabled })
  ElMessage.success(user.enabled ? '用户已停用' : '用户已启用')
  await load()
}

function openEdit(user) {
  editForm.value = {
    id: user.id,
    username: user.username,
    displayName: user.displayName,
    role: user.role,
    department: user.department,
    enabled: user.enabled
  }
  editVisible.value = true
}

async function saveEdit() {
  if (!editForm.value.displayName.trim()) {
    ElMessage.warning('姓名不能为空')
    return
  }
  saving.value = true
  try {
    await api.updateUser(editForm.value.id, {
      displayName: editForm.value.displayName.trim(),
      role: editForm.value.role,
      department: editForm.value.department,
      enabled: editForm.value.enabled
    })
    ElMessage.success('用户信息已更新')
    editVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="page-stack">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><UserFilled /></el-icon> 新增用户</span>
          <el-tag effect="plain">默认密码 123456</el-tag>
        </div>
      </template>

      <el-form :model="form" label-position="top">
        <el-row :gutter="12">
          <el-col :lg="4" :md="8" :sm="24"><el-form-item label="账号"><el-input v-model="form.username" /></el-form-item></el-col>
          <el-col :lg="4" :md="8" :sm="24"><el-form-item label="姓名"><el-input v-model="form.displayName" /></el-form-item></el-col>
          <el-col :lg="4" :md="8" :sm="24"><el-form-item label="密码"><el-input v-model="form.password" /></el-form-item></el-col>
          <el-col :lg="5" :md="8" :sm="24">
            <el-form-item label="角色">
              <el-select v-model="form.role" class="full-control">
                <el-option v-for="role in userRoleOptions" :key="role.value" :label="role.label" :value="role.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="5" :md="8" :sm="24">
            <el-form-item label="部门">
              <el-select v-model="form.department" class="full-control">
                <el-option v-for="dept in departmentOptions" :key="dept" :label="dept" :value="dept" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="2" :md="8" :sm="24" class="form-actions-col">
            <el-button type="primary" :icon="UserFilled" @click="createUser">创建</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><User /></el-icon> 用户列表</span>
          <el-alert class="inline-alert" type="info" :closable="false" show-icon title="内置演示账号可在登录页一键填入，密码均为 123456。" />
        </div>
      </template>

      <el-table :data="users" border stripe v-loading="loading">
        <el-table-column prop="username" label="账号" width="140" />
        <el-table-column prop="displayName" label="姓名" width="130" />
        <el-table-column label="角色" width="150">
          <template #default="{ row }">{{ roleLabel(row.role) }}</template>
        </el-table-column>
        <el-table-column prop="department" label="部门" min-width="150" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.enabled ? 'danger' : 'success'" @click="toggleUser(row)">
              {{ row.enabled ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editVisible" title="编辑用户信息" width="520px">
      <el-form :model="editForm" label-position="top">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="账号">
              <el-input v-model="editForm.username" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名">
              <el-input v-model="editForm.displayName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色">
              <el-select v-model="editForm.role" class="full-control">
                <el-option v-for="role in userRoleOptions" :key="role.value" :label="role.label" :value="role.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门">
              <el-select v-model="editForm.department" class="full-control">
                <el-option v-for="dept in departmentOptions" :key="dept" :label="dept" :value="dept" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="账号状态">
              <el-switch v-model="editForm.enabled" active-text="启用" inactive-text="停用" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存修改</el-button>
      </template>
    </el-dialog>
  </section>
</template>
