import dayjs from 'dayjs'

/** 格式化时间：今天显示 HH:mm，今年显示 MM-DD HH:mm，其余 YYYY-MM-DD */
export function formatTime(time) {
  if (!time) return ''
  const d = dayjs(time)
  const now = dayjs()
  if (d.isSame(now, 'day')) return d.format('HH:mm')
  if (d.isSame(now, 'year')) return d.format('MM-DD HH:mm')
  return d.format('YYYY-MM-DD')
}

export function formatDateTime(time) {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm') : ''
}

/** 相对时间，如 3分钟前 */
export function fromNow(time) {
  if (!time) return ''
  const diff = Date.now() - dayjs(time).valueOf()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / minute)}分钟前`
  if (diff < day) return `${Math.floor(diff / hour)}小时前`
  if (diff < 30 * day) return `${Math.floor(diff / day)}天前`
  return dayjs(time).format('YYYY-MM-DD')
}

/** 注册天数 */
export function registerDays(createTime) {
  if (!createTime) return 1
  return Math.max(1, dayjs().diff(dayjs(createTime), 'day') + 1)
}

export const CONTENT_TYPE = { POST: 1, BLOG: 2, QA: 3 }
export const TYPE_NAME = { 1: '帖子', 2: '博客', 3: '问答' }
export const TYPE_ROUTE = { 1: '/post', 2: '/blog', 3: '/qa' }

/** 默认头像（首字符底色头像用 svg data uri） */
export function defaultAvatar(name = '友') {
  const ch = (name || '友').slice(0, 1)
  const colors = ['#e60012', '#168cff', '#00b42a', '#ff7d00', '#722ed1']
  const color = colors[ch.charCodeAt(0) % colors.length]
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="80" height="80"><rect width="80" height="80" fill="${color}"/><text x="40" y="52" font-size="36" fill="#fff" text-anchor="middle" font-family="sans-serif">${ch}</text></svg>`
  return 'data:image/svg+xml;utf8,' + encodeURIComponent(svg)
}
