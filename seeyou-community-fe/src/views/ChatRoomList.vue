<template>
  <div class="sy-container chat-list-page">
    <div class="page-head">
      <h2>聊天室</h2>
      <el-button
        v-if="userStore.isAdmin"
        type="primary"
        @click="createVisible = true"
      >
        <el-icon><Plus /></el-icon>&nbsp;创建聊天室
      </el-button>
    </div>

    <div v-loading="loading" class="room-grid">
      <div v-for="room in rooms" :key="room.id" class="room-card sy-card" @click="enter(room)">
        <div class="room-head">
          <div class="room-name ellipsis-1">{{ room.name }}</div>
          <span class="online-badge"><i class="dot" />{{ room.onlineCount || 0 }} 在线</span>
        </div>
        <div class="room-desc ellipsis-2">{{ room.description || '暂无公告' }}</div>
        <div class="room-foot">
          <span class="creator">
            <el-avatar :size="20" :src="room.creator?.avatarUrl || defaultAvatar(room.creator?.nickname)" />
            {{ room.creator?.nickname || '管理员' }}
          </span>
          <span class="time">{{ formatDateTime(room.createTime) }}</span>
        </div>
      </div>
      <el-empty v-if="!loading && !rooms.length" description="暂无聊天室" />
    </div>

    <el-pagination
      v-if="total > size"
      v-model:current-page="current"
      :total="Number(total)"
      :page-size="size"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="load"
    />

    <el-dialog v-model="createVisible" title="创建聊天室" width="440px">
      <el-form label-position="top">
        <el-form-item label="名称" required>
          <el-input v-model="createForm.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="公告">
          <el-input v-model="createForm.description" type="textarea" :rows="3" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="doCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus/es/components/message/index'
import 'element-plus/es/components/message/style/css'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getRoomList, createRoom } from '@/api/chat'
import { formatDateTime, defaultAvatar } from '@/utils/format'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const rooms = ref([])
const loading = ref(false)
const current = ref(1)
const size = 12
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const res = await getRoomList({ current: current.value, size })
    rooms.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const enter = (room) => {
  if (!userStore.isLogin) {
    router.push({ path: '/login', query: { redirect: `/chat/${room.id}` } })
    return
  }
  router.push(`/chat/${room.id}`)
}

const createVisible = ref(false)
const creating = ref(false)
const createForm = reactive({ name: '', description: '' })
async function doCreate() {
  if (!createForm.name.trim()) {
    ElMessage.warning('请输入名称')
    return
  }
  creating.value = true
  try {
    const res = await createRoom({ ...createForm })
    ElMessage.success('创建成功')
    createVisible.value = false
    router.push(`/chat/${res.data}`)
  } finally {
    creating.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.chat-list-page {
  padding-top: 20px;
}
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.room-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  min-height: 240px;
}
.room-card {
  padding: 18px 20px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.room-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}
.room-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.room-name {
  font-size: 16px;
  font-weight: 700;
}
.online-badge {
  flex-shrink: 0;
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
.room-desc {
  font-size: 13px;
  color: var(--sy-text-secondary);
  margin-top: 10px;
  min-height: 40px;
  line-height: 1.6;
}
.room-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}
.creator {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--sy-text-regular);
}
.time {
  font-size: 12px;
  color: var(--sy-text-secondary);
}
.pager {
  justify-content: center;
  margin-top: 24px;
}
</style>
