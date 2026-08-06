<template>
  <div class="layout">
    <header class="header">
      <div class="header-inner sy-container">
        <div class="logo" @click="$router.push('/')">
          <span class="logo-badge">SeeYou</span>
          <span class="logo-text">之友<em>开发者社区</em></span>
        </div>

        <nav class="nav">
          <router-link class="nav-item" to="/" exact-active-class="active">首页</router-link>
          <el-dropdown trigger="hover" @command="go">
            <span class="nav-item el-dropdown-link" :class="{ active: isCircle }">
              社区圈子<el-icon class="nav-arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/post">帖子广场</el-dropdown-item>
                <el-dropdown-item command="/chat">聊天室</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown trigger="hover" @command="go">
            <span class="nav-item el-dropdown-link" :class="{ active: isKnowledge }">
              知识中心<el-icon class="nav-arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/blog">技术博客</el-dropdown-item>
                <el-dropdown-item command="/qa">问答广场</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <router-link class="nav-item" to="/assistant" active-class="active">AI 助手</router-link>
        </nav>

        <div class="header-right">
          <el-input
            v-model="keyword"
            class="search-input"
            placeholder="搜索帖子 / 博客 / 问答"
            clearable
            @keyup.enter="doSearch"
          >
            <template #prefix>
              <el-icon style="cursor: pointer" @click="doSearch"><Search /></el-icon>
            </template>
          </el-input>

          <template v-if="userStore.isLogin">
            <el-dropdown trigger="click" @command="onUserCommand">
              <div class="user-entry">
                <el-avatar :size="32" :src="userStore.userInfo?.avatarUrl || avatar" />
                <span class="nickname ellipsis-1">{{ userStore.userInfo?.nickname }}</span>
                <el-icon><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="/profile">
                    <el-icon><User /></el-icon>个人中心
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" round @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="main">
      <router-view v-slot="{ Component }">
        <component :is="Component" :key="$route.fullPath" />
      </router-view>
    </main>

    <footer class="footer">
      <div class="sy-container">
        <p>之友 SeeYou 开发者社区 · 分享、交流、成长</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { defaultAvatar } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const keyword = ref(route.query.keyword || '')
const avatar = computed(() => defaultAvatar(userStore.userInfo?.nickname))

const isCircle = computed(() => route.path.startsWith('/post') || route.path.startsWith('/chat'))
const isKnowledge = computed(() => route.path.startsWith('/blog') || route.path.startsWith('/qa'))

const go = (path) => router.push(path)

const doSearch = () => {
  router.push({ path: '/search', query: keyword.value ? { keyword: keyword.value } : {} })
}

const onUserCommand = async (cmd) => {
  if (cmd === 'logout') {
    await userStore.logout()
    router.push('/')
  } else {
    router.push(cmd)
  }
}
</script>

<style scoped>
.layout {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.header {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--sy-border-light);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-inner {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  flex-shrink: 0;
}
.logo-badge {
  height: 34px;
  padding: 0 10px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--sy-primary-grad);
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.5px;
  box-shadow: 0 4px 12px rgba(230, 0, 18, 0.3);
}
.logo-text {
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.logo-text em {
  font-style: normal;
  font-size: 13px;
  font-weight: 500;
  color: var(--sy-text-secondary);
}

.nav {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}
.nav-item {
  padding: 0 14px;
  height: 60px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 15px;
  color: var(--sy-text-main);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: color 0.2s;
}
.nav-item:hover {
  color: var(--sy-primary);
}
.nav-item.active {
  color: var(--sy-primary);
  border-bottom-color: var(--sy-primary);
  font-weight: 600;
}
.nav-arrow {
  transition: transform 0.2s;
}
.el-dropdown-link:hover .nav-arrow {
  transform: rotate(180deg);
}
.el-dropdown-link {
  outline: none;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.search-input {
  width: 220px;
}
.search-input :deep(.el-input__wrapper) {
  border-radius: 16px;
  background: #f2f3f5;
  box-shadow: none;
  transition: background 0.2s, box-shadow 0.2s;
}
.search-input :deep(.el-input__wrapper:hover),
.search-input :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px var(--sy-primary-light) inset;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}
.nickname {
  max-width: 90px;
  font-size: 14px;
}

.main {
  flex: 1;
}

.footer {
  background: #fff;
  border-top: 1px solid var(--sy-border);
  padding: 20px 0;
  text-align: center;
  color: var(--sy-text-secondary);
  font-size: 13px;
  margin-top: 40px;
}
</style>
