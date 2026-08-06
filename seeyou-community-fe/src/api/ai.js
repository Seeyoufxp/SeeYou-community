import request from '@/utils/request'

// 首页欢迎语：AI 接口慢/超时不应弹错给用户，silent 跳过全局错误提示
export const getWelcome = () => request.get('/api/ai/welcome', { silent: true })

// RAG + LLM 同步长耗时接口，单独放大超时
export const askAssistant = (question) =>
  request.post('/api/ai/assistant', { question }, { timeout: 90000 })

export const rebuildKnowledge = () => request.post('/api/ai/knowledge/rebuild')
