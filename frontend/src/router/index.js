import { createRouter, createWebHistory } from 'vue-router'
import { useSessionStore } from '../stores/session'
import LoginView from '../views/LoginView.vue'
import AppLayout from '../layouts/AppLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import ChatView from '../views/ChatView.vue'
import KnowledgeListView from '../views/KnowledgeListView.vue'
import KnowledgeCreateView from '../views/KnowledgeCreateView.vue'
import KnowledgeDetailView from '../views/KnowledgeDetailView.vue'
import SystemSettingsView from '../views/SystemSettingsView.vue'
import UserManagementView from '../views/UserManagementView.vue'
import AuditLogView from '../views/AuditLogView.vue'
import FeedbackView from '../views/FeedbackView.vue'

const ALL_ROLES = ['SYSTEM_ADMIN', 'DEPARTMENT_ADMIN', 'TEACHER', 'STUDENT']
const KNOWLEDGE_READ_ROLES = ['SYSTEM_ADMIN', 'DEPARTMENT_ADMIN', 'TEACHER']
const KNOWLEDGE_WRITE_ROLES = ['SYSTEM_ADMIN', 'DEPARTMENT_ADMIN']
const SYSTEM_ADMIN_ROLES = ['SYSTEM_ADMIN']
const FEEDBACK_ROLES = ['SYSTEM_ADMIN', 'DEPARTMENT_ADMIN']

const routes = [
  { path: '/login', name: 'login', component: LoginView, meta: { title: '登录' } },
  {
    path: '/',
    component: AppLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: DashboardView, meta: { title: '工作台', roles: ALL_ROLES } },
      { path: 'chat', name: 'chat', component: ChatView, meta: { title: '智能问答', roles: ALL_ROLES } },
      { path: 'knowledge', name: 'knowledge', component: KnowledgeListView, meta: { title: '文档管理', roles: KNOWLEDGE_READ_ROLES } },
      { path: 'knowledge/create', name: 'knowledge-create', component: KnowledgeCreateView, meta: { title: '资料录入', roles: KNOWLEDGE_WRITE_ROLES } },
      { path: 'knowledge/:id', name: 'knowledge-detail', component: KnowledgeDetailView, meta: { title: '文档详情', roles: KNOWLEDGE_READ_ROLES } },
      { path: 'settings', name: 'settings', component: SystemSettingsView, meta: { title: '系统配置', roles: SYSTEM_ADMIN_ROLES } },
      { path: 'users', name: 'users', component: UserManagementView, meta: { title: '用户管理', roles: SYSTEM_ADMIN_ROLES } },
      { path: 'logs', name: 'logs', component: AuditLogView, meta: { title: '操作日志', roles: SYSTEM_ADMIN_ROLES } },
      { path: 'feedback', name: 'feedback', component: FeedbackView, meta: { title: '问答反馈', roles: FEEDBACK_ROLES } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const session = useSessionStore()
  if (to.name !== 'login' && !session.user) {
    return { name: 'login' }
  }
  if (to.name === 'login' && session.user) {
    return { name: 'dashboard' }
  }
  if (to.meta.roles?.length && !to.meta.roles.includes(session.user?.role)) {
    if (to.name === 'dashboard') {
      return false
    }
    return { name: 'dashboard' }
  }
  document.title = `${to.meta.title || '系统'} - 校园知识库`
})

export default router
