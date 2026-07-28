import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    children: [
      { path: '', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
      {
        path: 'post',
        component: () => import('@/views/ContentList.vue'),
        meta: { title: '帖子广场', res: 'post' }
      },
      {
        path: 'blog',
        component: () => import('@/views/ContentList.vue'),
        meta: { title: '技术博客', res: 'blog' }
      },
      {
        path: 'qa',
        component: () => import('@/views/ContentList.vue'),
        meta: { title: '问答广场', res: 'qa' }
      },
      {
        path: ':res(post|blog|qa)/publish',
        component: () => import('@/views/ContentPublish.vue'),
        meta: { title: '发布', requireAuth: true }
      },
      {
        path: ':res(post|blog|qa)/:id(\\d+)/edit',
        component: () => import('@/views/ContentPublish.vue'),
        meta: { title: '编辑', requireAuth: true }
      },
      {
        path: ':res(post|blog|qa)/:id(\\d+)',
        component: () => import('@/views/ContentDetail.vue'),
        meta: { title: '详情' }
      },
      {
        path: 'chat',
        component: () => import('@/views/ChatRoomList.vue'),
        meta: { title: '聊天室' }
      },
      {
        path: 'chat/:id(\\d+)',
        component: () => import('@/views/ChatRoom.vue'),
        meta: { title: '聊天室', requireAuth: true }
      },
      {
        path: 'search',
        component: () => import('@/views/SearchResult.vue'),
        meta: { title: '全站搜索' }
      },
      {
        path: 'assistant',
        component: () => import('@/views/AiAssistant.vue'),
        meta: { title: 'AI 开发助手' }
      },
      {
        path: 'profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', requireAuth: true }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 之友开发者社区` : '之友开发者社区'
  const token = localStorage.getItem('token')
  if (to.meta.requireAuth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router
