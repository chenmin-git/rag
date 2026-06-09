const API_BASE = import.meta.env.VITE_API_BASE || '/api'

function userHeaders() {
  const raw = localStorage.getItem('campus-rag-user')
  if (!raw) return {}
  try {
    const user = JSON.parse(raw)
    return user?.id ? { 'X-User-Id': String(user.id) } : {}
  } catch {
    return {}
  }
}

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      ...userHeaders(),
      ...(options.headers || {})
    }
  })
  const contentType = response.headers.get('content-type') || ''
  const payload = contentType.includes('application/json') ? await response.json() : await response.text()
  if (!response.ok || payload?.success === false) {
    throw new Error(payload?.message || response.statusText)
  }
  return payload?.data ?? payload
}

export const api = {
  login: (username, password) => request('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password })
  }),
  me: () => request('/auth/me'),
  dashboard: () => request('/dashboard'),
  documents: () => request('/documents'),
  document: (id) => request(`/documents/${id}`),
  uploadDocument: (formData) => request('/documents', {
    method: 'POST',
    body: formData
  }),
  createTextDocument: (payload) => request('/documents/text', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  updateDocument: (id, payload) => request(`/documents/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  }),
  rebuildDocument: (id) => request(`/documents/${id}/rebuild`, { method: 'POST' }),
  deleteDocument: (id) => request(`/documents/${id}`, { method: 'DELETE' }),
  chunks: (id) => request(`/documents/${id}/chunks`),
  aiStatus: () => request('/chat/status'),
  ask: (payload) => request('/chat/ask', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  history: () => request('/chat/history'),
  feedback: (id, payload) => request(`/chat/${id}/feedback`, {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  settings: () => request('/admin/settings'),
  updateSettings: (payload) => request('/admin/settings', {
    method: 'PUT',
    body: JSON.stringify(payload)
  }),
  users: () => request('/admin/users'),
  createUser: (payload) => request('/admin/users', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  updateUser: (id, payload) => request(`/admin/users/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  }),
  logs: () => request('/admin/logs'),
  feedbackList: () => request('/admin/feedback')
}
