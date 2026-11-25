import { reactive } from 'vue'

const K = { token: 'sv_token', user: 'sv_user', menus: 'sv_menus' }

// 全局登录态：token / 用户信息 / 动态菜单，持久化到 localStorage，刷新不丢
export const store = reactive({
  token: localStorage.getItem(K.token) || '',
  user: JSON.parse(localStorage.getItem(K.user) || 'null'),
  menus: JSON.parse(localStorage.getItem(K.menus) || '[]')
})

export function setLogin(data) {
  store.token = data.token
  store.user = data.user
  store.menus = data.menus || []
  localStorage.setItem(K.token, data.token)
  localStorage.setItem(K.user, JSON.stringify(data.user))
  localStorage.setItem(K.menus, JSON.stringify(store.menus))
}

export function setUser(user) {
  store.user = user
  localStorage.setItem(K.user, JSON.stringify(user))
}

export function clearLogin() {
  store.token = ''
  store.user = null
  store.menus = []
  Object.values(K).forEach(k => localStorage.removeItem(k))
}

/** 判断当前角色是否拥有某路径的菜单权限 */
export function hasPath(path) {
  const walk = list => list.some(m => m.path === path || (m.children && walk(m.children)))
  return walk(store.menus)
}
