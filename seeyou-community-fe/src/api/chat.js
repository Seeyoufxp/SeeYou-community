import request from '@/utils/request'

export const createRoom = (data) => request.post('/api/chat/room', data)
export const updateRoom = (id, data) => request.put(`/api/chat/room/${id}`, data)
export const deleteRoom = (id) => request.delete(`/api/chat/room/${id}`)
export const getRoomList = (params) => request.get('/api/chat/room/list', { params })
export const getRoomDetail = (id) => request.get(`/api/chat/room/${id}`)
export const getOnlineCount = (id) => request.get(`/api/chat/room/${id}/online/count`)
export const getOnlineMembers = (id) => request.get(`/api/chat/room/${id}/online/members`)
export const sendMessage = (data) => request.post('/api/chat/message/send', data)
export const getMessageHistory = (params) => request.get('/api/chat/message/history', { params })
