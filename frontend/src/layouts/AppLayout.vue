<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ChatDotRound,
  DataAnalysis,
  DocumentAdd,
  Files,
  Notebook,
  Operation,
  Setting,
  SwitchButton,
  User,
  Warning
} from '@element-plus/icons-vue'
import { useSessionStore } from '../stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const ALL_ROLES = ['SYSTEM_ADMIN', 'DEPARTMENT_ADMIN', 'TEACHER', 'STUDENT']
const KNOWLEDGE_READ_ROLES = ['SYSTEM_ADMIN', 'DEPARTMENT_ADMIN', 'TEACHER']
const KNOWLEDGE_WRITE_ROLES = ['SYSTEM_ADMIN', 'DEPARTMENT_ADMIN']
const SYSTEM_ADMIN_ROLES = ['SYSTEM_ADMIN']
const FEEDBACK_ROLES = ['SYSTEM_ADMIN', 'DEPARTMENT_ADMIN']

const groups = [
  {
    title: '业务功能',
    items: [
      { label: '工作台', path: '/dashboard', icon: DataAnalysis, roles: ALL_ROLES },
      { label: '智能问答', path: '/chat', icon: ChatDotRound, roles: ALL_ROLES },
      { label: '文档管理', path: '/knowledge', icon: Files, roles: KNOWLEDGE_READ_ROLES },
      { label: '资料录入', path: '/knowledge/create', icon: DocumentAdd, roles: KNOWLEDGE_WRITE_ROLES }
    ]
  },
  {
    title: '系统维护',
    items: [
      { label: '系统配置', path: '/settings', icon: Setting, roles: SYSTEM_ADMIN_ROLES },
      { label: '用户管理', path: '/users', icon: User, roles: SYSTEM_ADMIN_ROLES },
      { label: '操作日志', path: '/logs', icon: Operation, roles: SYSTEM_ADMIN_ROLES },
      { label: '问答反馈', path: '/feedback', icon: Warning, roles: FEEDBACK_ROLES }
    ]
  }
]

const visibleGroups = computed(() => groups
  .map((group) => ({
    ...group,
    items: group.items.filter((item) => item.roles.includes(session.user?.role))
  }))
  .filter((group) => group.items.length))

const currentTitle = computed(() => route.meta.title || '工作台')
const activePath = computed(() => {
  if (route.path.startsWith('/knowledge/')) {
    return route.path === '/knowledge/create' ? '/knowledge/create' : '/knowledge'
  }
  return route.path
})
const userDisplayName = computed(() => {
  const displayName = session.user?.displayName?.trim()
  if (!displayName) {
    return session.roleLabel || '未命名用户'
  }
  return displayName
})
const userInitial = computed(() => (
  userDisplayName.value?.slice(0, 1)?.toUpperCase()
  || session.user?.username?.trim()?.slice(0, 1)
  || '用'
))
const userSubtitle = computed(() => {
  return session.user?.department?.trim() || '未设置部门'
})
const roleTagType = computed(() => ({
  SYSTEM_ADMIN: 'danger',
  DEPARTMENT_ADMIN: 'warning',
  TEACHER: 'success',
  STUDENT: 'info'
}[session.user?.role] || 'info'))

function navigate(path) {
  router.push(path)
}

function logout() {
  session.logout()
  router.replace('/login')
}
</script>

<template>
  <el-container class="el-admin-shell">
    <el-aside width="236px" class="el-admin-aside">
      <div class="el-admin-brand">
        <el-icon :size="26"><Notebook /></el-icon>
        <div>
          <strong>校园知识库</strong>
          <span>知识库管理系统</span>
        </div>
      </div>

      <el-menu
        :default-active="activePath"
        background-color="#1f2937"
        text-color="#cbd5e1"
        active-text-color="#ffffff"
        class="el-admin-menu"
        @select="navigate"
      >
        <template v-for="group in visibleGroups" :key="group.title">
          <div class="el-menu-caption">{{ group.title }}</div>
          <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="el-admin-header">
        <div>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
          <h1>{{ currentTitle }}</h1>
        </div>

        <el-dropdown trigger="click">
          <div class="el-user-entry">
            <el-avatar :size="36" class="user-avatar">{{ userInitial }}</el-avatar>
            <div class="el-user-info">
              <div class="el-user-name-row">
                <strong>{{ userDisplayName }}</strong>
                <el-tag :type="roleTagType" size="small" effect="plain">{{ session.roleLabel }}</el-tag>
              </div>
              <span class="el-user-subtitle">{{ userSubtitle }}</span>
            </div>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>{{ userSubtitle }}</el-dropdown-item>
              <el-dropdown-item divided @click="logout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="el-admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
