<template>
  <div class="sy-container ai-page">
    <div class="ai-card sy-card">
      <div class="ai-head">
        <div class="ai-title">
          <el-icon color="#e60012" :size="22"><MagicStick /></el-icon>
          AI 开发助手
        </div>
        <div class="ai-sub">基于社区知识库（博客 / 问答）的 RAG 智能问答</div>
        <el-button
          v-if="userStore.isAdmin"
          size="small"
          text
          :loading="rebuilding"
          @click="rebuild"
        >
          <el-icon><Refresh /></el-icon>&nbsp;重建知识库
        </el-button>
      </div>

      <!-- 对话区 -->
      <div class="chat-area">
        <div v-if="!sessions.length" class="empty">
          <el-icon :size="48" color="#f9b3b7"><MagicStick /></el-icon>
          <p>你好，我是之友 AI 开发助手</p>
          <p class="empty-sub">直接输入你的问题开始对话</p>
        </div>

        <template v-for="(s, i) in sessions" :key="i">
          <div class="row mine">
            <div class="bubble user">{{ s.question }}</div>
          </div>
          <div class="row">
            <div class="bubble ai">
              <div v-if="s.loading" class="loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                正在检索社区知识库并生成回答，可能需要几十秒…
              </div>
              <template v-else>
                <div class="md-body" v-html="md.render(s.answer)" />
                <div v-if="s.references?.length" class="refs">
                  <div class="refs-title">参考内容：</div>
                  <div
                    v-for="r in s.references"
                    :key="r.id"
                    class="ref-item"
                    @click="goRef(r)"
                  >
                    <el-icon><Document /></el-icon>
                    <span class="ellipsis-1">{{ r.title }}</span>
                    <el-tag size="small" :type="r.type === 2 ? 'primary' : 'success'">
                      {{ r.type === 2 ? '博客' : '问答' }}
                    </el-tag>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </template>
      </div>

      <!-- 输入区 -->
      <div class="input-area">
        <el-input
          v-model="question"
          type="textarea"
          :rows="2"
          resize="none"
          maxlength="500"
          show-word-limit
          placeholder="输入你的问题，Enter 发送（Shift+Enter 换行）"
          @keydown.enter.exact.prevent="ask(question)"
        />
        <el-button type="primary" :disabled="!question.trim() || asking" @click="ask(question)">
          发送<el-icon><Promotion /></el-icon>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus/es/components/message/index'
import 'element-plus/es/components/message/style/css'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import 'element-plus/es/components/message-box/style/css'
import { nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MarkdownIt from 'markdown-it'
import { askAssistant, rebuildKnowledge } from '@/api/ai'
import { useUserStore } from '@/stores/user'

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const question = ref('')
const sessions = ref([])
const asking = ref(false)
const rebuilding = ref(false)

async function ask(q) {
  const text = (q || '').trim()
  if (!text || asking.value) return
  if (!userStore.isLogin) {
    ElMessage.warning('AI 助手需要登录后使用')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  question.value = ''
  asking.value = true
  // 必须用 reactive() 包装，ref([]) 的 push 不会让原对象变成响应式，
  // 直接 s.answer/s.loading = ... 改的是原对象，Vue 不会触发更新 → 一直 loading。
  const s = reactive({ question: text, loading: true, answer: '', references: [] })
  sessions.value.push(s)
  scrollBottom()
  try {
    const res = await askAssistant(text)
    s.answer = res.data.answer
    s.references = res.data.references || []
  } catch {
    s.answer = '抱歉，回答生成失败，请稍后重试。'
    s.references = []
  } finally {
    s.loading = false
    asking.value = false
    scrollBottom()
  }
}

function scrollBottom() {
  nextTick(() => {
    const el = document.querySelector('.chat-area')
    if (el) el.scrollTop = el.scrollHeight
  })
}

const goRef = (r) => router.push(`${r.type === 2 ? '/blog' : '/qa'}/${r.id}`)

async function rebuild() {
  await ElMessageBox.confirm('将重新同步全量博客/问答到向量知识库，确认执行？', '重建知识库', {
    type: 'warning'
  })
  rebuilding.value = true
  try {
    const res = await rebuildKnowledge()
    ElMessage.success(`重建完成，共同步 ${res.data} 篇文档`)
  } finally {
    rebuilding.value = false
  }
}

onMounted(() => {
  const q = route.query.q
  if (q) ask(String(q))
})
</script>

<style scoped>
.ai-page {
  padding-top: 20px;
}
.ai-card {
  max-width: 860px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px - 40px - 80px);
  min-height: 480px;
  overflow: hidden;
}
.ai-head {
  padding: 16px 24px;
  border-bottom: 1px solid var(--sy-border);
  display: flex;
  align-items: center;
  gap: 16px;
}
.ai-title {
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}
.ai-sub {
  flex: 1;
  font-size: 12px;
  color: var(--sy-text-secondary);
}

.chat-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  background: #f7f8fa;
}
.empty {
  text-align: center;
  padding-top: 60px;
  color: var(--sy-text-regular);
}
.empty-sub {
  font-size: 13px;
  color: var(--sy-text-secondary);
  margin-top: 6px;
}

.row {
  display: flex;
  margin-bottom: 16px;
}
.row.mine {
  justify-content: flex-end;
}
.bubble {
  max-width: 78%;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.7;
}
.bubble.user {
  background: var(--sy-primary);
  color: #fff;
  border-radius: 10px 2px 10px 10px;
  white-space: pre-wrap;
  word-break: break-word;
}
.bubble.ai {
  background: #fff;
  border-radius: 2px 10px 10px 10px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}
.loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--sy-text-secondary);
  font-size: 13px;
}
.refs {
  margin-top: 12px;
  border-top: 1px dashed var(--sy-border);
  padding-top: 10px;
}
.refs-title {
  font-size: 12px;
  color: var(--sy-text-secondary);
  margin-bottom: 6px;
}
.ref-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  padding: 5px 8px;
  border-radius: 4px;
  cursor: pointer;
}
.ref-item:hover {
  background: #f2f3f5;
  color: var(--sy-primary);
}

.input-area {
  display: flex;
  gap: 10px;
  padding: 12px 24px;
  border-top: 1px solid var(--sy-border);
  align-items: flex-end;
}
</style>
