<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DataAnalysis, Key, Lock, Notebook, UserFilled } from '@element-plus/icons-vue'
import { api } from '../services/api'
import { useSessionStore } from '../stores/session'

const router = useRouter()
const session = useSessionStore()
const loading = ref(false)
const form = ref({ username: '', password: '' })
const demoAccounts = [
  { label: '管理员', username: 'admin', role: '系统管理员' },
  { label: '教师', username: 'teacher', role: '教师' },
  { label: '学生', username: 'student', role: '学生' },
  { label: '部门', username: 'dept', role: '部门管理员' }
]

function fillAccount(account) {
  form.value.username = account.username
  form.value.password = '123456'
}

async function login() {
  const username = form.value.username.trim()
  const password = form.value.password.trim()
  form.value.username = username
  form.value.password = password
  if (!username || !password) {
    ElMessage.warning('账号和密码不能为空')
    return
  }
  loading.value = true
  try {
    const user = await api.login(username, password)
    session.setUser(user)
    ElMessage.success('登录成功')
    router.replace('/dashboard')
  } catch (err) {
    ElMessage.error(err.message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="element-login-page">
    <section class="login-shell">
      <aside class="login-intro">
        <div class="login-logo">
          <el-icon :size="34"><Notebook /></el-icon>
          <span>校园知识库管理系统</span>
        </div>
        <h1>校园知识服务平台</h1>
        <p>规章制度、办事流程、教学资料和学生服务知识统一接入。</p>
        <div class="login-stat-grid">
          <article>
            <el-icon><DataAnalysis /></el-icon>
            <strong>知识库</strong>
            <span>知识增强</span>
          </article>
          <article>
            <el-icon><Lock /></el-icon>
            <strong>权限</strong>
            <span>分权访问</span>
          </article>
        </div>
      </aside>

      <el-card class="element-login-card" shadow="never">
        <div class="login-card-head">
          <span>系统登录</span>
          <el-tag effect="plain">内置演示数据</el-tag>
        </div>
        <h2>欢迎回来</h2>
        <p>使用统一身份账号或演示账号进入系统。</p>

        <el-form label-position="top" :model="form" @submit.prevent @keyup.enter="login">
          <el-form-item label="账号">
            <el-input v-model="form.username" autocomplete="username" :prefix-icon="UserFilled" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password autocomplete="current-password" :prefix-icon="Lock" />
          </el-form-item>
          <el-button type="primary" :loading="loading" class="full-control login-submit" @click="login">
            <el-icon><Key /></el-icon>
            登录
          </el-button>
        </el-form>

        <div class="account-strip">
          <button v-for="account in demoAccounts" :key="account.username" type="button" @click="fillAccount(account)">
            <strong>{{ account.label }}</strong>
            <span>{{ account.role }} · 密码 123456</span>
          </button>
        </div>
      </el-card>
    </section>
  </main>
</template>
