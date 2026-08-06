import request from '@/utils/request'

export const getWelcome = () => request.get('/api/ai/welcome')

// RAG + LLM 同步长耗时接口，单独放大超时
export const askAssistant = (question) =>
  request.post('/api/ai/assistant', { question }, { timeout: 90000 })

export const rebuildKnowledge = () => request.post('/api/ai/knowledge/rebuild')
