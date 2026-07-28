<template>
  <div class="sy-container search-page">
    <div class="search-bar sy-card">
      <el-input
        v-model="keyword"
        size="large"
        placeholder="输入关键词，搜索帖子 / 博客 / 问答"
        clearable
        @keyup.enter="doSearch"
      >
        <template #append>
          <el-button type="primary" @click="doSearch">
            <el-icon><Search /></el-icon>&nbsp;搜索
          </el-button>
        </template>
      </el-input>
      <div class="type-tabs">
        <span
          v-for="t in typeTabs"
          :key="String(t.value)"
          class="type-tab"
          :class="{ active: type === t.value }"
          @click="changeType(t.value)"
        >
          {{ t.label }}
        </span>
        <span class="total-text">共 {{ total }} 条结果</span>
      </div>
    </div>

    <div v-loading="loading" class="results">
      <div v-for="item in list" :key="item.id" class="result-item sy-card" @click="goDetail(item)">
        <div class="result-head">
          <el-tag size="small" :type="tagType(item.type)">{{ TYPE_NAME[item.type] }}</el-tag>
          <span class="result-title ellipsis-1" v-html="item.title" />
        </div>
        <div v-if="item.summary" class="result-summary ellipsis-2" v-html="item.summary" />
        <div v-else-if="item.contentSnippet" class="result-summary ellipsis-2" v-html="item.contentSnippet" />
        <div class="result-meta">
          <el-avatar :size="20" :src="item.avatarUrl || defaultAvatar(item.nickname)" />
          <span>{{ item.nickname }}</span>
          <span>{{ fromNow(item.createTime) }}</span>
          <span><el-icon><Pointer /></el-icon>{{ item.likeCount }}</span>
          <span><el-icon><ChatDotRound /></el-icon>{{ item.commentCount }}</span>
        </div>
      </div>
      <el-empty v-if="!loading && !list.length" description="没有找到相关内容，换个关键词试试" />

      <el-pagination
        v-if="total > size"
        v-model:current-page="current"
        :total="Number(total)"
        :page-size="size"
        layout="total, prev, pager, next, jumper"
        class="pager"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { search } from '@/api/search'
import { fromNow, defaultAvatar, TYPE_NAME, TYPE_ROUTE } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const keyword = ref(String(route.query.keyword || ''))
const type = ref(route.query.type ? Number(route.query.type) : null)
const list = ref([])
const total = ref(0)
const current = ref(1)
const size = 10
const loading = ref(false)

const typeTabs = [
  { label: '全部', value: null },
  { label: '帖子', value: 1 },
  { label: '博客', value: 2 },
  { label: '问答', value: 3 }
]

const tagType = (t) => ({ 1: 'danger', 2: 'primary', 3: 'success' }[t])

async function load() {
  loading.value = true
  try {
    const params = { current: current.value, size }
    if (keyword.value) params.keyword = keyword.value
    if (type.value) params.type = type.value
    const res = await search(params)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function doSearch() {
  current.value = 1
  router.replace({ query: { ...(keyword.value && { keyword: keyword.value }), ...(type.value && { type: type.value }) } })
  load()
}

const changeType = (v) => {
  type.value = v
  doSearch()
}

const goDetail = (item) => router.push(`${TYPE_ROUTE[item.type]}/${item.id}`)

onMounted(load)
</script>

<style scoped>
.search-page {
  padding-top: 20px;
}
.search-bar {
  padding: 20px;
}
.type-tabs {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-top: 14px;
}
.type-tab {
  font-size: 14px;
  color: var(--sy-text-regular);
  cursor: pointer;
}
.type-tab.active {
  color: var(--sy-primary);
  font-weight: 600;
}
.total-text {
  margin-left: auto;
  font-size: 13px;
  color: var(--sy-text-secondary);
}
.results {
  margin-top: 16px;
  min-height: 300px;
}
.result-item {
  padding: 18px 20px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.result-item:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
}
.result-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.result-title {
  font-size: 16px;
  font-weight: 600;
}
.result-summary {
  font-size: 13px;
  color: var(--sy-text-regular);
  margin-top: 8px;
  line-height: 1.7;
}
.result-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 10px;
  font-size: 12px;
  color: var(--sy-text-secondary);
}
.result-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.pager {
  justify-content: center;
  margin: 20px 0;
}
</style>
