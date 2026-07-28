import request from '@/utils/request'

export const search = (params) => request.get('/api/search', { params })
