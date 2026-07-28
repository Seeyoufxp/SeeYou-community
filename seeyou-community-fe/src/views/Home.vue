<template>
  <div class="home">
    <!-- 欢迎区 + AI 助手 -->
    <section class="hero">
      <div class="sy-container">
        <h1 class="hero-title">
          {{ welcome || 'Hi，欢迎来到之友开发者社区' }}
          <el-icon v-if="welcome" class="hero-badge"><Sunny /></el-icon>
        </h1>

        <div class="hero-box sy-card">
          <div class="hero-tabs">
            <span
              v-for="t in tabs"
              :key="t.key"
              class="hero-tab"
              :class="{ active: activeTab === t.key }"
              @click="activeTab = t.key"
            >
              <el-icon v-if="t.key === 'ai'"><MagicStick /></el-icon>{{ t.label }}
            </span>
          </div>

          <!-- AI 助手 -->
          <div v-if="activeTab === 'ai'" class="hero-input">
            <el-input
              v-model="aiQuestion"
              type="textarea"
              :rows="3"
              resize="none"
              placeholder="#提问咨询：向 AI 开发助手提问，基于社区知识库为你解答"
              @keyup.ctrl.enter="askAi"
            />
            <div class="hero-actions">
              <span class="hero-tip">Ctrl + Enter 快速提问</span>
              <el-button type="primary" round :loading="aiLoading" @click="askAi">
                向 AI 提问
              </el-button>
            </div>
          </div>

          <!-- 全站搜索 -->
          <div v-else class="hero-input">
            <el-input
              v-model="searchKeyword"
              size="large"
              :placeholder="`输入关键词，搜索${tabsLabel[activeTab] || '全站内容'}`"
              clearable
              @keyup.enter="doSearch"
            >
              <template #append>
                <el-button type="primary" @click="doSearch">
                  <el-icon><Search /></el-icon>&nbsp;搜索
                </el-button>
              </template>
            </el-input>
          </div>
        </div>

        <!-- 快捷入口 -->
        <div class="quick-grid">
          <div v-for="q in quickEntries" :key="q.title" class="quick-card sy-card" @click="quickGo(q)">
            <div class="quick-title">
              <el-icon :color="q.color"><component :is="q.icon" /></el-icon>
              {{ q.title }}
            </div>
            <div class="quick-desc">{{ q.desc }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 内容区块 -->
    <section class="sy-container sections">
      <!-- 最新发布 -->
      <div class="section-card sy-card section-latest">
        <div class="section-head">
          <div class="section-tabs">
            <span
              v-for="t in latestTabs"
              :key="t.key"
              class="section-tab"
              :class="{ active: latestType === t.key }"
              @click="switchLatest(t.key)"
            >
              {{ t.label }}
            </span>
          </div>
          <span class="more sy-link" @click="$router.push(latestTabs.find(t => t.key === latestType).path)">
            更多<el-icon><ArrowRight /></el-icon>
          </span>
        </div>
        <div v-loading="latestLoading" class="latest-list">
          <div
            v-for="item in latestList"
            :key="item.id"
            class="latest-item"
            @click="goDetail(item)"
          >
            <span class="dot" />
            <span class="latest-title ellipsis-1">{{ item.title }}</span>
            <span class="latest-time">{{ fromNow(item.createTime) }}</span>
          </div>
          <el-empty v-if="!latestLoading && !latestList.length" description="暂无内容" :image-size="60" />
        </div>
        <div class="latest-footer">
          <el-button size="small" @click="$router.push(latestTabs.find(t => t.key === latestType).path)">更多</el-button>
          <el-button size="small" type="primary" @click="goPublish">发布{{ latestTabs.find(t => t.key === latestType).short }}</el-button>
        </div>
      </div>

      <!-- 热门问答 -->
      <div class="section-card sy-card" @click="$router.push('/qa')">
        <div class="side-title">热门问答</div>
        <div class="side-desc">汇聚知识碎片，分享你的见解，一起解决技术难题</div>
        <div class="side-list">
          <div v-for="q in hotQa" :key="q.id" class="side-item ellipsis-1" @click.stop="goDetail(q)">
            {{ q.title }}
          </div>
        </div>
      </div>

      <!-- 精选博客 -->
      <div class="section-card sy-card" @click="$router.push('/blog')">
        <div class="side-title">精选博客</div>
        <div class="side-desc">体系化技术分享，快速掌握开发技能、跟进最新趋势</div>
        <div class="side-list">
          <div v-for="b in hotBlog" :key="b.id" class="side-item ellipsis-1" @click.stop="goDetail(b)">
            {{ b.title }}
          </div>
        </div>
      </div>

      <!-- 聊天室 -->
      <div class="section-card sy-card" @click="$router.push('/chat')">
        <div class="side-title">聊天室</div>
        <div class="side-desc">构建充满活力的开发者交流平台，让热爱从此启航</div>
        <div class="side-list">
          <div v-for="r in rooms" :key="r.id" class="side-item ellipsis-1" @click.stop="$router.push(`/chat/${r.id}`)">
            {{ r.name }}<span class="online">{{ r.onlineCount || 0 }} 人在线</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus/es/components/message/index'
import 'element-plus/es/components/message/style/css'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getWelcome } from '@/api/ai'
import { getContentList } from '@/api/content'
import { search as searchAll } from '@/api/search'
import { getRoomList } from '@/api/chat'
import { fromNow, TYPE_ROUTE } from '@/utils/format'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const welcome = ref('')
const tabs = [
  { key: 'ai', label: 'AI 开发助手' },
  { key: 'all', label: '全站搜索' },
  { key: 'blog', label: '搜博文' },
  { key: 'qa', label: '搜问答' },
  { key: 'post', label: '搜帖子' }
]
const activeTab = ref('ai')
const aiQuestion = ref('')
const searchKeyword = ref('')

const tabsLabel = { all: '全站内容', blog: '博文', qa: '问答', post: '帖子' }
const doSearch = () => {
  const query = {}
  if (searchKeyword.value) query.keyword = searchKeyword.value
  if (activeTab.value !== 'all') query.type = { blog: 2, qa: 3, post: 1 }[activeTab.value]
  router.push({ path: '/search', query })
}
const askAi = () => {
  if (!aiQuestion.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  router.push({ path: '/assistant', query: { q: aiQuestion.value.trim() } })
}

// 快捷入口
const quickEntries = [
  { title: '发帖子', desc: '分享动态与技术日常', icon: 'ChatDotRound', color: '#e60012', path: '/post/publish' },
  { title: '写博客', desc: '沉淀体系化技术文章', icon: 'EditPen', color: '#168cff', path: '/blog/publish' },
  { title: '提问题', desc: '向社区开发者求助', icon: 'QuestionFilled', color: '#00b42a', path: '/qa/publish' },
  { title: '聊天室', desc: '实时交流在线畅聊', icon: 'ChatLineRound', color: '#ff7d00', path: '/chat' },
  { title: 'AI 助手', desc: '社区知识库智能问答', icon: 'MagicStick', color: '#722ed1', path: '/assistant' }
]
const quickGo = (q) => {
  if (q.path.endsWith('/publish') && !userStore.isLogin) {
    router.push({ path: '/login', query: { redirect: q.path } })
    return
  }
  router.push(q.path)
}

// 最新发布
const latestTabs = [
  { key: 'all', label: '最新发布', path: '/search', short: '' },
  { key: 'post', label: '帖子', path: '/post', short: '帖子' },
  { key: 'blog', label: '博客', path: '/blog', short: '博客' },
  { key: 'qa', label: '问答', path: '/qa', short: '问题' }
]
const latestType = ref('all')
const latestList = ref([])
const latestLoading = ref(false)

async function loadLatest() {
  latestLoading.value = true
  try {
    if (latestType.value === 'all') {
      // 空关键词 = 全站按时间倒序
      const res = await searchAll({ current: 1, size: 6 })
      latestList.value = res.data.records
    } else {
      const res = await getContentList(latestType.value, { current: 1, size: 6 })
      latestList.value = res.data.records
    }
  } finally {
    latestLoading.value = false
  }
}
const switchLatest = (key) => {
  latestType.value = key
  loadLatest()
}
const goPublish = () => {
  const res = latestType.value === 'all' ? 'post' : latestType.value
  if (!userStore.isLogin) {
    router.push({ path: '/login', query: { redirect: `/${res}/publish` } })
    return
  }
  router.push(`/${res}/publish`)
}

// 热门问答 / 精选博客 / 聊天室
const hotQa = ref([])
const hotBlog = ref([])
const rooms = ref([])

const goDetail = (item) => {
  router.push(`${TYPE_ROUTE[item.type]}/${item.id}`)
}

onMounted(async () => {
  loadLatest()
  getWelcome().then((res) => (welcome.value = res.data.message)).catch(() => {})
  getContentList('qa', { current: 1, size: 5, sortBy: 'comment_count' })
    .then((res) => (hotQa.value = res.data.records))
    .catch(() => {})
  getContentList('blog', { current: 1, size: 5, sortBy: 'like_count' })
    .then((res) => (hotBlog.value = res.data.records))
    .catch(() => {})
  getRoomList({ current: 1, size: 5 })
    .then((res) => (rooms.value = res.data.records))
    .catch(() => {})
})
</script>

<style scoped>
.hero {
  background: linear-gradient(135deg, #f6f7ff 0%, #fff 45%, #eef4ff 100%);
  padding: 40px 0 24px;
}
.hero-title {
  text-align: center;
  font-size: 28px;
  font-weight: 700;
}
.hero-badge {
  color: #ffab00;
  vertical-align: middle;
}

.hero-box {
  max-width: 820px;
  margin: 24px auto 0;
  padding: 16px 20px;
}
.hero-tabs {
  display: flex;
  gap: 20px;
  border-bottom: 1px solid var(--sy-border);
  padding-bottom: 10px;
}
.hero-tab {
  font-size: 14px;
  color: var(--sy-text-regular);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.hero-tab.active {
  color: var(--sy-primary);
  font-weight: 600;
}
.hero-input {
  padding-top: 14px;
}
.hero-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}
.hero-tip {
  font-size: 12px;
  color: var(--sy-text-secondary);
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-top: 24px;
}
.quick-card {
  padding: 16px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.quick-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}
.quick-title {
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}
.quick-desc {
  margin-top: 6px;
  font-size: 12px;
  color: var(--sy-text-secondary);
}

.sections {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: 16px;
  margin-top: 20px;
}
.section-card {
  padding: 16px 20px;
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.section-tabs {
  display: flex;
  gap: 16px;
}
.section-tab {
  font-size: 15px;
  color: var(--sy-text-regular);
  cursor: pointer;
  padding-bottom: 6px;
  border-bottom: 2px solid transparent;
}
.section-tab.active {
  color: var(--sy-text-main);
  font-weight: 700;
  border-bottom-color: var(--sy-primary);
}
.more {
  font-size: 13px;
  color: var(--sy-text-secondary);
  display: inline-flex;
  align-items: center;
}

.latest-list {
  margin-top: 10px;
  min-height: 180px;
}
.latest-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  cursor: pointer;
}
.latest-item:hover .latest-title {
  color: var(--sy-primary);
}
.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--sy-primary);
  flex-shrink: 0;
}
.latest-title {
  flex: 1;
  font-size: 14px;
}
.latest-time {
  font-size: 12px;
  color: var(--sy-text-secondary);
  flex-shrink: 0;
}
.latest-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 8px;
}

.section-card:not(.section-latest) {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.section-card:not(.section-latest):hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}
.side-title {
  font-size: 16px;
  font-weight: 700;
}
.side-desc {
  font-size: 12px;
  color: var(--sy-text-secondary);
  margin-top: 8px;
  line-height: 1.6;
}
.side-list {
  margin-top: 10px;
}
.side-item {
  font-size: 13px;
  padding: 6px 0;
  color: var(--sy-text-regular);
}
.side-item:hover {
  color: var(--sy-primary);
}
.online {
  color: var(--sy-text-secondary);
  font-size: 12px;
  margin-left: 6px;
}
</style>
