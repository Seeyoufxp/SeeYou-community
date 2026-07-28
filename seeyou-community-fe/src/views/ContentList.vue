<template>
  <div class="sy-container content-list">
    <div class="list-main">
      <div class="toolbar sy-card">
        <div class="tabs">
          <span
            v-for="s in sortOptions"
            :key="s.value"
            class="tab"
            :class="{ active: query.sortBy === s.value }"
            @click="changeSort(s.value)"
          >
            {{ s.label }}
          </span>
        </div>
        <div class="toolbar-right">
          <el-input
            v-model="query.keyword"
            placeholder="搜索标题/摘要"
            clearable
            style="width: 200px"
            @keyup.enter="search"
            @clear="search"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="goPublish">
            <el-icon><EditPen /></el-icon>&nbsp;{{ publishText }}
          </el-button>
        </div>
      </div>

      <div v-loading="loading" class="items">
        <div v-for="item in list" :key="item.id" class="item sy-card" @click="goDetail(item.id)">
          <el-avatar :size="44" :src="item.avatarUrl || defaultAvatar(item.nickname)" />
          <div class="item-body">
            <div class="item-title ellipsis-1">{{ item.title }}</div>
            <div v-if="item.summary" class="item-summary ellipsis-2">{{ item.summary }}</div>
            <div class="item-meta">
              <span class="author">{{ item.nickname }}</span>
              <span>{{ fromNow(item.createTime) }}</span>
            </div>
          </div>
          <div class="item-stats">
            <span class="stat" :class="{ liked: item.liked }">
              <el-icon><Pointer /></el-icon>{{ item.likeCount }}
            </span>
            <span class="stat">
              <el-icon><ChatDotRound /></el-icon>{{ item.commentCount }}
            </span>
          </div>
        </div>
        <el-empty v-if="!loading && !list.length" :description="`还没有${typeName}，来发第一篇吧`" />
      </div>

      <el-pagination
        v-if="total > query.size"
        v-model:current-page="query.current"
        :total="Number(total)"
        :page-size="query.size"
        layout="total, prev, pager, next, jumper"
        class="pager"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getContentList } from '@/api/content'
import { fromNow, defaultAvatar } from '@/utils/format'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// route.meta.res: post | blog | qa（同组件复用，meta 变化时重置）
const res = computed(() => route.meta.res)
const typeName = computed(() => ({ post: '帖子', blog: '博客', qa: '问答' }[res.value]))
const publishText = computed(() => ({ post: '发帖子', blog: '写博客', qa: '提问题' }[res.value]))

const sortOptions = [
  { label: '最新', value: 'create_time' },
  { label: '最热', value: 'like_count' },
  { label: '评论最多', value: 'comment_count' }
]

const query = reactive({ current: 1, size: 10, keyword: '', sortBy: 'create_time', order: 'desc' })
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (!params.keyword) delete params.keyword
    const res2 = await getContentList(res.value, params)
    list.value = res2.data.records
    total.value = res2.data.total
  } finally {
    loading.value = false
  }
}

const changeSort = (v) => {
  query.sortBy = v
  query.current = 1
  load()
}
const search = () => {
  query.current = 1
  load()
}
const goDetail = (id) => router.push(`/${res.value}/${id}`)
const goPublish = () => {
  const path = `/${res.value}/publish`
  if (!userStore.isLogin) {
    router.push({ path: '/login', query: { redirect: path } })
    return
  }
  router.push(path)
}

watch(() => route.meta.res, () => {
  query.current = 1
  query.keyword = ''
  query.sortBy = 'create_time'
  load()
}, { immediate: true })
</script>

<style scoped>
.content-list {
  padding-top: 20px;
}
.list-main {
  max-width: 900px;
  margin: 0 auto;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
}
.tabs {
  display: flex;
  gap: 20px;
}
.tab {
  font-size: 14px;
  color: var(--sy-text-regular);
  cursor: pointer;
}
.tab.active {
  color: var(--sy-primary);
  font-weight: 600;
}
.toolbar-right {
  display: flex;
  gap: 12px;
}

.items {
  margin-top: 16px;
  min-height: 300px;
}
.item {
  display: flex;
  gap: 14px;
  padding: 18px 20px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.item:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
}
.item-body {
  flex: 1;
  min-width: 0;
}
.item-title {
  font-size: 16px;
  font-weight: 600;
}
.item-summary {
  font-size: 13px;
  color: var(--sy-text-secondary);
  margin-top: 6px;
  line-height: 1.6;
}
.item-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--sy-text-secondary);
  margin-top: 8px;
}
.author {
  color: var(--sy-text-regular);
}
.item-stats {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  flex-shrink: 0;
}
.stat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--sy-text-secondary);
}
.stat.liked {
  color: var(--sy-primary);
}
.pager {
  justify-content: center;
  margin: 20px 0;
}
</style>
