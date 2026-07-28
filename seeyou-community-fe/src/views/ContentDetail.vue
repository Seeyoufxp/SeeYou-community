<template>
  <div class="sy-container detail-page">
    <div class="detail-main">
      <div v-loading="loading" class="detail-card sy-card">
        <template v-if="detail.id">
          <h1 class="title">{{ detail.title }}</h1>
          <div class="meta">
            <el-avatar :size="36" :src="detail.avatarUrl || defaultAvatar(detail.nickname)" />
            <div class="meta-info">
              <div class="nickname">{{ detail.nickname }}</div>
              <div class="time">{{ formatDateTime(detail.createTime) }}</div>
            </div>
            <el-tag size="small" :type="tagType">{{ typeName }}</el-tag>
            <div class="meta-actions" v-if="isAuthor">
              <el-button size="small" @click="$router.push(`/${res}/${detail.id}/edit`)">编辑</el-button>
              <el-button size="small" type="danger" plain @click="remove">删除</el-button>
            </div>
          </div>

          <div class="content md-body" v-html="renderedContent" />

          <div class="like-bar">
            <el-button
              :type="detail.liked ? 'primary' : 'default'"
              round
              :loading="likeLoading"
              @click="toggleLike"
            >
              <el-icon><Pointer /></el-icon>&nbsp;
              {{ detail.liked ? '已点赞' : '点赞' }} {{ detail.likeCount }}
            </el-button>
          </div>
        </template>
        <el-empty v-else-if="!loading" description="内容不存在或已删除">
          <el-button type="primary" @click="$router.push(`/${res}`)">返回列表</el-button>
        </el-empty>
      </div>

      <CommentSection v-if="detail.id" :post-id="detail.id" :is-qa="res === 'qa'" />
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus/es/components/message/index'
import 'element-plus/es/components/message/style/css'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import 'element-plus/es/components/message-box/style/css'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MarkdownIt from 'markdown-it'
import {
  getContentDetail,
  deleteContent,
  likeContent,
  unlikeContent
} from '@/api/content'
import CommentSection from '@/components/CommentSection.vue'
import { formatDateTime, defaultAvatar } from '@/utils/format'
import { useUserStore } from '@/stores/user'

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const res = computed(() => route.params.res)
const id = computed(() => route.params.id)
const typeName = computed(() => ({ post: '帖子', blog: '博客', qa: '问答' }[res.value]))
const tagType = computed(() => ({ post: 'danger', blog: 'primary', qa: 'success' }[res.value]))

const detail = ref({})
const loading = ref(false)
const likeLoading = ref(false)

const isAuthor = computed(
  () =>
    userStore.isLogin &&
    (String(detail.value.userId) === String(userStore.userInfo?.userId) || userStore.isAdmin)
)

// 博客/帖子正文按 Markdown 渲染，问答按纯文本
const renderedContent = computed(() => {
  const c = detail.value.content || ''
  if (res.value === 'blog') return md.render(c)
  return md.render(c.replace(/</g, '&lt;'))
})

async function load() {
  loading.value = true
  try {
    const res2 = await getContentDetail(res.value, id.value)
    detail.value = res2.data || {}
  } catch {
    detail.value = {}
  } finally {
    loading.value = false
  }
}

async function toggleLike() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  likeLoading.value = true
  try {
    const r = detail.value.liked
      ? await unlikeContent(res.value, id.value)
      : await likeContent(res.value, id.value)
    detail.value.liked = r.data.liked
    detail.value.likeCount = r.data.likeCount
  } finally {
    likeLoading.value = false
  }
}

async function remove() {
  await ElMessageBox.confirm(`确定删除这篇${typeName.value}吗？`, '提示', { type: 'warning' })
  await deleteContent(res.value, id.value)
  ElMessage.success('已删除')
  router.push(`/${res.value}`)
}

watch(() => route.fullPath, load, { immediate: true })
</script>

<style scoped>
.detail-page {
  padding-top: 20px;
}
.detail-main {
  max-width: 900px;
  margin: 0 auto;
}
.detail-card {
  padding: 28px 36px;
  min-height: 240px;
}
.title {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.4;
}
.meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--sy-border);
}
.meta-info {
  flex: 1;
}
.nickname {
  font-size: 14px;
  font-weight: 600;
}
.time {
  font-size: 12px;
  color: var(--sy-text-secondary);
  margin-top: 2px;
}
.meta-actions {
  display: flex;
  gap: 8px;
}
.content {
  margin-top: 20px;
  font-size: 15px;
  min-height: 120px;
}
.like-bar {
  display: flex;
  justify-content: center;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid var(--sy-border);
}
</style>
