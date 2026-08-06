import { ElMessage } from 'element-plus/es/components/message/index'
import 'element-plus/es/components/message/style/css'
import axios from 'axios'
import JSONbig from 'json-bigint'
import router from '@/router'

// 雪花 ID（userId、聊天消息 ID 等）是 19 位 Long，超出 JS Number 安全范围，
// 用 json-bigint 把大整数保留为字符串，避免精度丢失。
const JSONbigString = JSONbig({ storeAsString: true })

// 开发环境 VITE_API_BASE 为空，走 vite 代理；生产环境指向线上网关
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/',
  timeout: 15000,
  transformResponse: [
    (data) => {
      if (typeof data !== 'string' || !data) return data
      try {
        return JSONbigString.parse(data)
      } catch {
        return data
      }
    }
  ]
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 非 R 包装（理论上不会）直接放行
    if (!res || typeof res.code === 'undefined') return res
    if (res.code === 200) return res

    // 业务失败
    if (res.code === 401) {
      handleUnauthorized(res.message)
    } else {
      ElMessage.error(res.message || '请求失败')
    }
    return Promise.reject(res)
  },
  (error) => {
    const config = error.config || {}
    // 业务方显式标记 silent：非关键路径，错误不弹给用户（仍 reject 让业务 catch 自行处理）
    if (config.silent) {
      return Promise.reject(error)
    }
    const status = error.response?.status
    const res = error.response?.data
    if (status === 401 || res?.code === 401) {
      handleUnauthorized(res?.message)
    } else {
      ElMessage.error(res?.message || error.message || '网络异常')
    }
    return Promise.reject(res || error)
  }
)

let unauthorizedHandling = false
function handleUnauthorized(message) {
  if (unauthorizedHandling) return
  unauthorizedHandling = true
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  ElMessage.warning(message || '登录已失效，请重新登录')
  const current = router.currentRoute.value
  if (current.path !== '/login') {
    router.push({ path: '/login', query: { redirect: current.fullPath } })
  }
  setTimeout(() => {
    unauthorizedHandling = false
  }, 1500)
}

export default request
