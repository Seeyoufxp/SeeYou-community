<template>
  <div class="sy-container chat-page">
    <div class="chat-card sy-card">
      <!-- 头部 -->
      <div class="chat-head">
        <el-icon class="back" @click="$router.push('/chat')"><ArrowLeft /></el-icon>
        <div class="head-info">
          <div class="room-name">{{ room.name || '聊天室' }}</div>
          <div class="room-desc ellipsis-1">{{ room.description }}</div>
        </div>
        <div class="online">
          <span class="online-badge"><i class="dot" />{{ onlineCount }} 人在线</span>
        </div>
        <el-popover v-if="isManager" placement="bottom" trigger="click" width="200">
          <template #reference>
            <el-icon class="more-btn"><MoreFilled /></el-icon>
          </template>
          <div class="room-ops">
            <el-button size="small" @click="openEdit">修改信息</el-button>
            <el-button size="small" type="danger" plain @click="dissolve">解散房间</el-button>
          </div>
        </el-popover>
      </div>

      <!-- 消息区 -->
      <div ref="msgArea" class="msg-area" @scroll="onScroll">
        <div v-if="hasMore" class="load-more">
          <el-button text size="small" :loading="loadingMore" @click="loadMore">
            加载更早的消息
          </el-button>
        </div>
        <div v-else-if="messages.length" class="load-more no-more">没有更早的消息了</div>

        <div
          v-for="m in messages"
          :key="String(m.id)"
          class="msg-row"
          :class="{ mine: String(m.userId) === String(userStore.userInfo?.userId) }"
        >
          <el-avatar :size="36" :src="m.avatarUrl || defaultAvatar(m.nickname)" />
          <div class="msg-main">
            <div class="msg-nick">{{ m.nickname }}<span class="msg-time">{{ formatMsgTime(m.createTime) }}</span></div>
            <div class="msg-bubble">{{ m.content }}</div>
          </div>
        </div>
        <el-empty v-if="!messages.length && !loadingHistory" description="还没有消息，说点什么吧" :image-size="80" />
      </div>

      <!-- 输入区 -->
      <div class="input-area">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          resize="none"
          maxlength="1024"
          placeholder="输入消息，Enter 发送，Shift+Enter 换行"
          @keydown.enter.exact.prevent="send"
        />
        <el-button type="primary" :disabled="!input.trim()" @click="send">
          发送<el-icon><Promotion /></el-icon>
        </el-button>
      </div>
    </div>

    <el-dialog v-model="editVisible" title="修改聊天室" width="440px">
      <el-form label-position="top">
        <el-form-item label="名称"><el-input v-model="editForm.name" maxlength="100" /></el-form-item>
        <el-form-item label="公告"><el-input v-model="editForm.description" type="textarea" :rows="3" maxlength="255" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus/es/components/message/index'
import 'element-plus/es/components/message/style/css'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import 'element-plus/es/components/message-box/style/css'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import JSONbig from 'json-bigint'
import {
  getRoomDetail,
  updateRoom,
  deleteRoom,
  getOnlineCount,
  sendMessage,
  getMessageHistory
} from '@/api/chat'
import { defaultAvatar } from '@/utils/format'
import { useUserStore } from '@/stores/user'

// 雪花 ID（19 位）JS Number 会丢精度，必须按字符串处理，
// 与 axios 的 transformResponse(JSONbig) 保持一致，否则 sendMessage 立即上屏的 id
// 和 WS 回推的 id 在 seenIds 里比较不等 → 重复显示
const JSONbigString = JSONbig({ storeAsString: true })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const roomId = route.params.id

const room = ref({})
const onlineCount = ref(0)
const messages = ref([])
const input = ref('')
const loadingHistory = ref(false)
const loadingMore = ref(false)
const current = ref(1)
const pageSize = 20
const hasMore = ref(false)
const msgArea = ref(null)

const isManager = computed(
  () =>
    userStore.isAdmin ||
    String(room.value.creatorId) === String(userStore.userInfo?.userId)
)

const formatMsgTime = (t) => dayjs(t).format('HH:mm')

// ---------- 历史消息 ----------
async function loadHistory(isMore = false) {
  if (isMore) loadingMore.value = true
  else loadingHistory.value = true
  try {
    const res = await getMessageHistory({ roomId, current: current.value, size: pageSize })
    // 后端倒序分页：records 是最新在前，渲染前要反转
    const page = (res.data.records || []).slice().reverse()
    messages.value = isMore ? [...page, ...messages.value] : page
    hasMore.value = current.value * pageSize < Number(res.data.total)
    if (!isMore) scrollToBottom()
    return page.length
  } finally {
    loadingMore.value = false
    loadingHistory.value = false
  }
}

async function loadMore() {
  current.value += 1
  const el = msgArea.value
  const prevHeight = el?.scrollHeight || 0
  const n = await loadHistory(true)
  if (n === 0) hasMore.value = false
  // 保持滚动位置
  await nextTick()
  if (el) el.scrollTop = el.scrollHeight - prevHeight
}

function scrollToBottom() {
  nextTick(() => {
    const el = msgArea.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

const onScroll = () => {
  const el = msgArea.value
  if (el && el.scrollTop < 40 && hasMore.value && !loadingMore.value) loadMore()
}

// ---------- WebSocket ----------
let ws = null
let heartbeatTimer = null
let reconnectTimer = null
let closedBySelf = false
const seenIds = new Set()

function wsUrl() {
  // 开发环境 VITE_API_BASE 为空，经 vite 代理走网关；生产环境直连线上网关。token 走 query
  const apiBase = import.meta.env.VITE_API_BASE
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  const host = apiBase ? apiBase.replace(/^https?:\/\//, '') : location.host
  return `${proto}://${host}/api/chat/ws?roomId=${roomId}&token=${encodeURIComponent(userStore.token)}`
}

function connect() {
  closedBySelf = false
  ws = new WebSocket(wsUrl())

  ws.onopen = () => {
    heartbeatTimer = setInterval(() => {
      if (ws?.readyState === WebSocket.OPEN) ws.send('ping')
    }, 25000)
  }

  ws.onmessage = (e) => {
    if (e.data === 'pong') return
    try {
      // 用 JSONbig 解析，把雪花 id 保留为字符串，与 HTTP 走 axios 那条路径的 key 一致
      const msg = JSONbigString.parse(e.data)
      const key = String(msg.id)
      if (seenIds.has(key)) return // 去重：发送者自己也会收到 MQ 回推
      seenIds.add(key)
      messages.value.push(msg)
      scrollToBottom()
    } catch {
      // 非 JSON 帧忽略
    }
  }

  ws.onclose = () => {
    clearInterval(heartbeatTimer)
    if (!closedBySelf) {
      // 断线重连
      reconnectTimer = setTimeout(connect, 3000)
    }
  }

  ws.onerror = () => ws?.close()
}

// ---------- 发消息（走 HTTP） ----------
async function send() {
  const text = input.value.trim()
  if (!text) return
  try {
    const res = await sendMessage({ roomId, content: text })
    input.value = ''
    // 立即上屏，WS 回推时按 id 去重
    const msg = res.data
    const key = String(msg.id)
    if (!seenIds.has(key)) {
      seenIds.add(key)
      messages.value.push(msg)
      scrollToBottom()
    }
  } catch {
    // 拦截器已提示
  }
}

// ---------- 房间管理 ----------
const editVisible = ref(false)
const editForm = reactive({ name: '', description: '' })
function openEdit() {
  editForm.name = room.value.name
  editForm.description = room.value.description
  editVisible.value = true
}
async function saveEdit() {
  await updateRoom(roomId, { ...editForm })
  ElMessage.success('已保存')
  editVisible.value = false
  loadRoom()
}
async function dissolve() {
  await ElMessageBox.confirm('确定解散该聊天室吗？', '提示', { type: 'warning' })
  await deleteRoom(roomId)
  ElMessage.success('已解散')
  router.push('/chat')
}

async function loadRoom() {
  const res = await getRoomDetail(roomId)
  room.value = res.data || {}
  if (room.value.status === 0) {
    ElMessage.warning('该聊天室已解散')
    router.push('/chat')
  }
}

let onlineTimer = null
async function refreshOnline() {
  try {
    const res = await getOnlineCount(roomId)
    onlineCount.value = Number(res.data)
  } catch {
    // 忽略
  }
}

onMounted(async () => {
  await loadRoom()
  await loadHistory()
  connect()
  refreshOnline()
  onlineTimer = setInterval(refreshOnline, 15000)
})

onBeforeUnmount(() => {
  closedBySelf = true
  clearTimeout(reconnectTimer)
  clearInterval(heartbeatTimer)
  clearInterval(onlineTimer)
  ws?.close()
})
</script>

<style scoped>
.chat-page {
  padding-top: 20px;
}
.chat-card {
  max-width: 860px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px - 40px - 80px);
  min-height: 480px;
  overflow: hidden;
}
.chat-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--sy-border);
}
.back {
  cursor: pointer;
  font-size: 18px;
}
.back:hover {
  color: var(--sy-primary);
}
.head-info {
  flex: 1;
  min-width: 0;
}
.room-name {
  font-size: 16px;
  font-weight: 700;
}
.room-desc {
  font-size: 12px;
  color: var(--sy-text-secondary);
  margin-top: 2px;
}
.online-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #00b42a;
  background: #e8ffea;
  border-radius: 10px;
  padding: 2px 10px;
}
.online-badge .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #00b42a;
}
.more-btn {
  cursor: pointer;
  font-size: 18px;
}
.room-ops {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.msg-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  background: #f7f8fa;
}
.load-more {
  text-align: center;
  margin-bottom: 8px;
}
.no-more {
  font-size: 12px;
  color: var(--sy-text-secondary);
}

.msg-row {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.msg-row.mine {
  flex-direction: row-reverse;
}
.msg-main {
  max-width: 70%;
}
.msg-nick {
  font-size: 12px;
  color: var(--sy-text-secondary);
  margin-bottom: 4px;
}
.msg-row.mine .msg-nick {
  text-align: right;
}
.msg-time {
  margin-left: 8px;
}
.msg-bubble {
  display: inline-block;
  background: #fff;
  border-radius: 2px 10px 10px 10px;
  padding: 8px 12px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}
.msg-row.mine .msg-bubble {
  background: #fde5e6;
  border-radius: 10px 2px 10px 10px;
}

.input-area {
  display: flex;
  gap: 10px;
  padding: 12px 20px;
  border-top: 1px solid var(--sy-border);
  align-items: flex-end;
}
</style>
