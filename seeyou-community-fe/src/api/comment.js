import request from '@/utils/request'

export const createComment = (data) => request.post('/api/comment', data)
export const getCommentList = (postId) => request.get('/api/comment/list', { params: { postId } })
export const deleteComment = (id) => request.delete(`/api/comment/${id}`)
export const likeComment = (id) => request.post(`/api/comment/${id}/like`)
export const unlikeComment = (id) => request.post(`/api/comment/${id}/unlike`)
