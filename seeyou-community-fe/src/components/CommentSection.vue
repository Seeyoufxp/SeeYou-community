<template>
  <div class="comment-section sy-card">
    <div class="section-title">{{ isQa ? `${comments.length} 个回答` : `${totalCount} 条评论` }}</div>

    <!-- 发表框 -->
    <div class="editor" v-if="userStore.isLogin">
      <el-input
        v-model="content"
        type="textarea"
        :rows="3"
        maxlength="1024"
        show-word-limit
        :placeholder="isQa ? '写下你的回答…' : '写下你的评论…'"
      />
      <div class="editor-actions">
        <el-button type="primary" :loading="submitting" :disabled="!content.trim()" @click="submit(0)">
          {{ isQa ? '发布回答' : '发布评论' }}
        </el-button>
      </div>
    </div>
    <div v-else class="login-tip">
      <el-button text type="primary" @click="$router.push({ path: '/login', query: { redirect: $route.fullPath } })">
        登录
      </el-button>
      后参与{{ isQa ? '回答' : '评论' }}
    </div>

    <!-- 评论列表 -->
    <div v-loading="loading" class="comments">
      <div v-for="c in comments" :key="c.id" class="comment">
        <el-avatar :size="36" :src="c.avatarUrl || defaultAvatar(c.nickname)" />
        <div class="comment-body">
          <div class="comment-head">
            <span class="nickname">{{ c.nickname }}</span>
            <span class="time">{{ fromNow(c.createTime) }}</span>
          </div>
          <div class="comment-content">{{ c.content }}</div>
          <div class="comment-ops">
            <span class="op" :class="{ liked: c.liked }" @click="toggleLike(c)">
              <el-icon><Pointer /></el-icon>{{ c.likeCount || '赞' }}
            </span>
            <span v-if="!isQa" class="op" @click="replyTo(c)">
              <el-icon><ChatDotRound /></el-icon>回复
            </span>
            <span v-if="canDelete(c)" class="op danger" @click="remove(c)">
              <el-icon><Delete /></el-icon>删除
            </span>
          </div>

          <!-- 回复输入框 -->
          <div v-if="replyTarget?.id === c.id" class="reply-editor">
            <el-input
              v-model="replyContent"
              type="textarea"
              :rows="2"
              maxlength="1024"
              :placeholder="`回复 ${c.nickname}：`"
            />
            <div class="editor-actions">
              <el-button size="small" @click="replyTarget = null">取消</el-button>
              <el-button size="small" type="primary" :loading="submitting" :disabled="!replyContent.trim()" @click="submit(c.id)">
                回复
              </el-button>
            </div>
          </div>

          <!-- 楼中楼（仅帖子/博客一层） -->
          <div v-if="c.children?.length" class="children">
            <div v-for="child in c.children" :key="child.id" class="child">
              <el-avatar :size="26" :src="child.avatarUrl || defaultAvatar(child.nickname)" />
              <div class="comment-body">
                <div class="comment-head">
                  <span class="nickname">{{ child.nickname }}</span>
                  <span class="time">{{ fromNow(child.createTime) }}</span>
                </div>
                <div class="comment-content">{{ child.content }}</div>
                <div class="comment-ops">
                  <span class="op" :class="{ liked: child.liked }" @click="toggleLike(child)">
                    <el-icon><Pointer /></el-icon>{{ child.likeCount || '赞' }}
                  </span>
                  <span v-if="canDelete(child)" class="op danger" @click="remove(child)">
                    <el-icon><Delete /></el-icon>删除
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && !comments.length" :description="isQa ? '还没有回答，来抢沙发' : '还没有评论，来抢沙发'" :image-size="80" />
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus/es/components/message/index'
import 'element-plus/es/components/message/style/css'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import 'element-plus/es/components/message-box/style/css'
import { computed, ref, watch } from 'vue'
import {
  createComment,
  getCommentList,
  deleteComment,
  likeComment,
  unlikeComment
} from '@/api/comment'
import { fromNow, defaultAvatar } from '@/utils/format'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  postId: { type: [String, Number], required: true },
  isQa: { type: Boolean, default: false }
})
const emit = defineEmits(['count-change'])

const userStore = useUserStore()
const comments = ref([])
const loading = ref(false)
const content = ref('')
const submitting = ref(false)
const replyTarget = ref(null)
const replyContent = ref('')

const totalCount = computed(() =>
  comments.value.reduce((sum, c) => sum + 1 + (c.children?.length || 0), 0)
)

const canDelete = (c) =>
  userStore.isLogin && (String(c.userId) === String(userStore.userInfo?.userId) || userStore.isAdmin)

async function load() {
  loading.value = true
  try {
    const res = await getCommentList(props.postId)
    comments.value = res.data || []
    emit('count-change', totalCount.value)
  } finally {
    loading.value = false
  }
}

async function submit(parentId) {
  if (!userStore.isLogin) return
  const text = parentId ? replyContent.value : content.value
  if (!text.trim()) return
  submitting.value = true
  try {
    await createComment({
      postId: String(props.postId),
      parentId: parentId || 0,
      content: text.trim()
    })
    ElMessage.success('发布成功')
    content.value = ''
    replyContent.value = ''
    replyTarget.value = null
    load()
  } finally {
    submitting.value = false
  }
}

const replyTo = (c) => {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return
  }
  replyTarget.value = c
  replyContent.value = ''
}

async function toggleLike(c) {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return
  }
  const res = c.liked ? await unlikeComment(c.id) : await likeComment(c.id)
  c.liked = res.data.liked
  c.likeCount = res.data.likeCount
}

async function remove(c) {
  await ElMessageBox.confirm('确定删除这条内容吗？', '提示', { type: 'warning' })
  await deleteComment(c.id)
  ElMessage.success('已删除')
  load()
}

watch(() => props.postId, load, { immediate: true })
</script>

<style scoped>
.comment-section {
  padding: 20px 28px;
  margin-top: 16px;
}
.section-title {
  font-size: 17px;
  font-weight: 700;
  margin-bottom: 16px;
}
.editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}
.login-tip {
  padding: 16px;
  text-align: center;
  color: var(--sy-text-secondary);
  background: #f7f8fa;
  border-radius: 6px;
  font-size: 14px;
}
.comments {
  margin-top: 20px;
}
.comment {
  display: flex;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--sy-border);
}
.comment:last-child {
  border-bottom: none;
}
.comment-body {
  flex: 1;
  min-width: 0;
}
.comment-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.nickname {
  font-size: 14px;
  font-weight: 600;
}
.time {
  font-size: 12px;
  color: var(--sy-text-secondary);
}
.comment-content {
  margin-top: 6px;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.comment-ops {
  display: flex;
  gap: 18px;
  margin-top: 8px;
}
.op {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--sy-text-secondary);
  cursor: pointer;
}
.op:hover {
  color: var(--sy-primary);
}
.op.liked {
  color: var(--sy-primary);
}
.op.danger:hover {
  color: #f53f3f;
}
.reply-editor {
  margin-top: 10px;
  background: #f7f8fa;
  padding: 12px;
  border-radius: 6px;
}
.children {
  margin-top: 12px;
  background: #f7f8fa;
  border-radius: 6px;
  padding: 4px 14px;
}
.child {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--sy-border);
}
.child:last-child {
  border-bottom: none;
}
</style>
