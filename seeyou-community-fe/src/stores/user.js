import { defineStore } from 'pinia'
import { login as apiLogin, logout as apiLogout, getUserInfo } from '@/api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    // { userId, username, nickname, avatarUrl, role }
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
  }),
  getters: {
    isLogin: (s) => !!s.token,
    isAdmin: (s) => (s.userInfo?.role ?? 0) >= 1
  },
  actions: {
    async login(form) {
      const res = await apiLogin(form)
      const { token, ...info } = res.data
      this.token = token
      this.userInfo = info
      localStorage.setItem('token', token)
      localStorage.setItem('userInfo', JSON.stringify(info))
      // 补全 role 等完整信息
      this.fetchUserInfo().catch(() => {})
    },
    async fetchUserInfo() {
      const res = await getUserInfo()
      const info = {
        userId: res.data.id,
        username: res.data.username,
        nickname: res.data.nickname,
        avatarUrl: res.data.avatarUrl,
        role: res.data.role
      }
      this.userInfo = info
      localStorage.setItem('userInfo', JSON.stringify(info))
      return res.data
    },
    async logout() {
      try {
        await apiLogout()
      } catch {
        // 忽略登出接口异常，本地照常清理
      }
      this.clear()
    },
    clear() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
