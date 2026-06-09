export const roleOptions = [
  { label: '全校公开', value: 'PUBLIC' },
  { label: '仅学生', value: 'STUDENT' },
  { label: '仅教师', value: 'TEACHER' },
  { label: '学生工作处', value: 'DEPARTMENT:学生工作处' },
  { label: '教务处', value: 'DEPARTMENT:教务处' },
  { label: '信息化办公室', value: 'DEPARTMENT:信息化办公室' }
]

export const departmentOptions = ['学生工作处', '教务处', '信息化办公室', '后勤服务中心', '图书馆']

export const userRoleOptions = [
  { label: '学生', value: 'STUDENT' },
  { label: '教师', value: 'TEACHER' },
  { label: '部门管理员', value: 'DEPARTMENT_ADMIN' },
  { label: '系统管理员', value: 'SYSTEM_ADMIN' }
]

export function visibilityLabel(value) {
  return roleOptions.find((item) => item.value === value)?.label || value
}

export function roleLabel(value) {
  return userRoleOptions.find((item) => item.value === value)?.label || value
}

export function statusLabel(status) {
  return {
    PENDING: '待解析',
    PARSING: '解析中',
    CHUNKING: '切片中',
    VECTORIZING: '向量化中',
    INDEXED: '已入库',
    FAILED: '入库失败'
  }[status] || status
}

export function feedbackLabel(value) {
  return {
    HELPFUL: '有帮助',
    NOT_HELPFUL: '无帮助',
    SOURCE_WRONG: '来源不准确',
    INCOMPLETE: '答案不完整'
  }[value] || value
}

export function actionLabel(value) {
  return {
    DOCUMENT_UPLOAD: '文档上传',
    DOCUMENT_TEXT_CREATE: '资料录入',
    DOCUMENT_UPDATE: '文档权限更新',
    DOCUMENT_REBUILD: '重建索引',
    DOCUMENT_DELETE: '文档删除',
    RAG_ASK: '智能问答',
    RAG_STREAM: '流式问答',
    SYSTEM_CONFIG_UPDATE: '系统配置修改'
  }[value] || value
}

export function formatTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}
