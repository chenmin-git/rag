<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ChatDotRound,
  CircleCheck,
  CircleClose,
  DocumentChecked,
  MagicStick,
  Promotion,
  RefreshRight,
  Setting,
  Warning
} from '@element-plus/icons-vue'
import { api } from '../services/api'
import { renderMarkdown } from '../utils/markdown'
import { formatTime, visibilityLabel } from '../utils/options'

const askForm = ref({ question: '', topK: 5, threshold: 0.2 })
const feedbackForm = ref({ type: 'HELPFUL', comment: '' })
const result = ref(null)
const history = ref([])
const aiStatus = ref(null)
const asking = ref(false)
const showSettings = ref(false)
const messages = ref([
  {
    role: 'assistant',
    content: '你好，我是校园知识库助手。你可以问我校园卡、考试缓考、宿舍报修、校园网账号等问题，我会优先依据知识库资料回答。',
    sources: []
  }
])

const suggestedQuestions = [
  '校园卡丢了怎么补办？',
  '期末考试缓考需要什么材料？',
  '宿舍报修一般多久处理？',
  '校园网账号忘记密码怎么办？'
]

const activeSources = computed(() => {
  const lastAssistant = [...messages.value].reverse().find((message) => message.role === 'assistant' && message.sources?.length)
  return lastAssistant?.sources || []
})
const aiStatusType = computed(() => (aiStatus.value?.mode === 'SPARK' ? 'success' : 'warning'))
const aiStatusLabel = computed(() => (aiStatus.value?.mode === 'SPARK' ? '大模型已接入' : '本地演示模式'))
const aiStatusMessage = computed(() => (
  aiStatus.value?.mode === 'SPARK'
    ? '已启用大模型增强回答，系统会优先结合知识库来源生成内容。'
    : '当前使用本地演示回答，可在系统配置中启用大模型服务。'
))
const askingText = computed(() => (
  aiStatus.value?.mode === 'SPARK'
    ? '正在检索知识库，并调用大模型服务生成回答...'
    : '正在检索知识库，并生成本地演示回答...'
))

async function loadAiStatus() {
  try {
    aiStatus.value = await api.aiStatus()
  } catch (err) {
    aiStatus.value = {
      mode: 'MOCK',
      message: err.message
    }
  }
}

async function loadHistory() {
  history.value = await api.history()
}

function useQuestion(question) {
  askForm.value.question = question
}

function messageHtml(message) {
  return renderMarkdown(message.content)
}

function newConversation() {
  result.value = null
  askForm.value.question = ''
  messages.value = [
    {
      role: 'assistant',
      content: '新的会话已开始。请直接输入你想查询的校园事项。',
      sources: []
    }
  ]
}

async function ask(question = askForm.value.question) {
  const cleaned = question.trim()
  if (!cleaned) return
  asking.value = true
  result.value = null
  askForm.value.question = ''
  messages.value.push({ role: 'user', content: cleaned })
  try {
    const data = await api.ask({ ...askForm.value, question: cleaned })
    result.value = data
    messages.value.push({
      role: 'assistant',
      content: data.answer,
      latencyMs: data.latencyMs,
      answerMode: data.answerMode,
      sources: data.sources || []
    })
    await loadHistory()
  } catch (err) {
    ElMessage.error(err.message)
    messages.value.push({ role: 'assistant', content: `抱歉，本次回答生成失败：${err.message}`, sources: [] })
  } finally {
    asking.value = false
  }
}

async function sendFeedback(type = feedbackForm.value.type) {
  if (!result.value?.id) return
  await api.feedback(result.value.id, { type, comment: feedbackForm.value.comment })
  feedbackForm.value = { type: 'HELPFUL', comment: '' }
  ElMessage.success('反馈已提交')
}

onMounted(() => {
  loadHistory()
  loadAiStatus()
})
</script>

<template>
  <section class="ai-chat-shell">
    <aside class="ai-chat-sidebar">
      <div class="ai-sidebar-head">
        <strong>校园问答</strong>
        <el-button size="small" type="primary" plain :icon="ChatDotRound" @click="newConversation">新会话</el-button>
      </div>

      <div class="ai-history-list">
        <button
          v-for="item in history"
          :key="item.id"
          class="ai-history-item"
          @click="useQuestion(item.question)"
        >
          <strong>{{ item.question }}</strong>
          <span>{{ formatTime(item.createdAt) }} · {{ item.latencyMs }} 毫秒</span>
        </button>
      </div>
    </aside>

    <main class="ai-chat-main">
      <header class="ai-chat-hero">
        <div>
          <span class="ai-kicker">校园知识库助手</span>
          <h2>今天想查什么校园事项？</h2>
          <div class="ai-status-line">
            <el-tag :type="aiStatusType" effect="plain">{{ aiStatusLabel }}</el-tag>
            <span>{{ aiStatusMessage }}</span>
          </div>
        </div>
        <div class="ai-chat-actions">
          <el-button size="small" :icon="RefreshRight" @click="loadAiStatus">刷新状态</el-button>
          <el-button :icon="Setting" circle @click="showSettings = true" />
        </div>
      </header>

      <div class="ai-suggestion-row">
        <button v-for="question in suggestedQuestions" :key="question" @click="useQuestion(question)">
          {{ question }}
        </button>
      </div>

      <section class="ai-message-stream">
        <article v-for="(message, index) in messages" :key="index" :class="['ai-message', message.role]">
          <div class="ai-avatar">
            <el-icon v-if="message.role === 'assistant'"><MagicStick /></el-icon>
            <span v-else>我</span>
          </div>
          <div class="ai-message-body">
            <div class="ai-message-name">{{ message.role === 'assistant' ? '校园知识库助手' : '我' }}</div>
            <div v-if="message.role === 'assistant'" class="ai-markdown" v-html="messageHtml(message)" />
            <p v-else class="ai-plain-message">{{ message.content }}</p>
            <div v-if="message.latencyMs" class="ai-message-meta">
              <el-tag size="small" effect="plain">{{ message.latencyMs }} 毫秒</el-tag>
              <el-tag size="small" :type="message.answerMode === 'SPARK' ? 'success' : 'warning'" effect="plain">
                {{ message.answerMode === 'SPARK' ? '大模型服务' : '本地演示' }}
              </el-tag>
              <el-tag size="small" type="success" effect="plain">{{ message.sources?.length || 0 }} 条来源</el-tag>
            </div>
            <el-collapse v-if="message.sources?.length" class="ai-source-collapse">
              <el-collapse-item :title="`查看引用来源（${message.sources.length}）`" name="sources">
                <article v-for="source in message.sources" :key="source.id" class="ai-source-item">
                  <div>
                    <strong>{{ source.fileName }}</strong>
                    <el-tag size="small" effect="plain">{{ source.department }}</el-tag>
                  </div>
                  <span>第 {{ source.pageNo }} 页 / 段落 {{ source.chunkNo }} / 相似度 {{ source.score.toFixed(3) }} / {{ visibilityLabel(source.visibility) }}</span>
                  <p>{{ source.content }}</p>
                </article>
              </el-collapse-item>
            </el-collapse>
            <div v-if="message.role === 'assistant' && result?.answer === message.content" class="ai-feedback-row">
              <el-button size="small" :icon="CircleCheck" @click="sendFeedback('HELPFUL')">有帮助</el-button>
              <el-button size="small" :icon="Warning" @click="sendFeedback('INCOMPLETE')">不完整</el-button>
              <el-button size="small" :icon="CircleClose" @click="sendFeedback('NOT_HELPFUL')">无帮助</el-button>
            </div>
          </div>
        </article>

        <article v-if="asking" class="ai-message assistant">
          <div class="ai-avatar"><el-icon class="is-loading"><RefreshRight /></el-icon></div>
          <div class="ai-message-body">
            <div class="ai-message-name">校园知识库助手</div>
            <p>{{ askingText }}</p>
          </div>
        </article>
      </section>

      <footer class="ai-composer-wrap">
        <div class="ai-active-sources" v-if="activeSources.length">
          <el-icon><DocumentChecked /></el-icon>
          已引用 {{ activeSources.length }} 条知识片段
        </div>
        <div class="ai-composer">
          <el-input
            v-model="askForm.question"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 5 }"
            resize="none"
            placeholder="向校园知识库提问…"
            @keydown.ctrl.enter.prevent="ask()"
          />
          <el-button type="primary" :icon="Promotion" :loading="asking" :disabled="!askForm.question.trim()" circle @click="ask()" />
        </div>
      </footer>
    </main>

    <el-drawer v-model="showSettings" title="检索与反馈设置" size="360px">
      <el-form label-position="top">
        <el-form-item label="召回条数">
          <el-input-number v-model="askForm.topK" :min="1" :max="12" class="full-control" />
        </el-form-item>
        <el-form-item label="相似度阈值">
          <el-input-number v-model="askForm.threshold" :min="0" :max="1" :step="0.05" class="full-control" />
        </el-form-item>
        <el-divider />
        <el-form-item label="反馈类型">
          <el-select v-model="feedbackForm.type" class="full-control">
            <el-option label="有帮助" value="HELPFUL" />
            <el-option label="无帮助" value="NOT_HELPFUL" />
            <el-option label="来源不准确" value="SOURCE_WRONG" />
            <el-option label="答案不完整" value="INCOMPLETE" />
          </el-select>
        </el-form-item>
        <el-form-item label="反馈备注">
          <el-input v-model="feedbackForm.comment" type="textarea" :rows="4" placeholder="提交反馈时附带这段备注" />
        </el-form-item>
      </el-form>
    </el-drawer>
  </section>
</template>
