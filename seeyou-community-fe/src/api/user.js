import request from '@/utils/request'

export const login = (data) => request.post('/api/user/login', data)
export const register = (data) => request.post('/api/user/register', data)
export const logout = () => request.post('/api/user/logout')
export const getUserInfo = () => request.get('/api/user/info')
// 注意：后端该接口字段为蛇形命名 avatar_url / blog_url / company_or_school
export const editUserInfo = (data) => request.post('/api/user/edit', data)

// 上传图片（头像等），返回公网 URL
// 注意：Content-Type 必须为 multipart/form-data，由浏览器自动生成 boundary；
// 不要手动设 JSON 头，否则后端 MultipartFile 解析不到 file 字段
export const uploadImage = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/api/user/upload/image', fd)
}
