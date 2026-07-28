import request from '@/utils/request'

// res: post | blog | qa，三类接口同构
export const publishContent = (res, data) => request.post(`/api/${res}`, data)
export const updateContent = (res, id, data) => request.put(`/api/${res}/${id}`, data)
export const deleteContent = (res, id) => request.delete(`/api/${res}/${id}`)
export const getContentList = (res, params) => request.get(`/api/${res}/list`, { params })
export const getContentDetail = (res, id) => request.get(`/api/${res}/${id}`)
export const likeContent = (res, id) => request.post(`/api/${res}/${id}/like`)
export const unlikeContent = (res, id) => request.post(`/api/${res}/${id}/unlike`)
