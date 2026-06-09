import { defineStore } from 'pinia'

const STORAGE_KEY = 'campus-rag-user'

export const useSessionStore = defineStore('session', {
  state: () => ({
    user: JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
  }),
  getters: {
    roleLabel: (state) => ({
      SYSTEM_ADMIN: '系统管理员',
      DEPARTMENT_ADMIN: '部门管理员',
      TEACHER: '教师',
      STUDENT: '学生'
    }[state.user?.role] || '访客')
  },
  actions: {
    setUser(user) {
      this.user = user
      localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
    },
    logout() {
      this.user = null
      localStorage.removeItem(STORAGE_KEY)
    }
  }
})
