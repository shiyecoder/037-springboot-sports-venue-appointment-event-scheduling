import axios from 'axios'
import { ElMessage } from 'element-plus'
import { store, clearLogin } from './store'

// 统一请求实例：自动附带 JWT，统一解包 R{code,msg,data}
const http = axios.create({ baseURL: '/api', timeout: 30000 })

http.interceptors.request.use(cfg => {
  if (store.token) cfg.headers.Authorization = 'Bearer ' + store.token
  return cfg
})

http.interceptors.response.use(
  res => {
    const body = res.data
    if (body && typeof body.code !== 'undefined') {
      if (body.code === 0) return body.data
      if (body.code === 401) {
        clearLogin()
        location.href = '/login'
        return Promise.reject(new Error(body.msg))
      }
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg))
    }
    return body
  },
  err => {
    const status = err.response && err.response.status
    if (status === 401 || status === 403) {
      ElMessage.error('登录已失效，请重新登录')
      clearLogin()
      location.href = '/login'
    } else {
      ElMessage.error(err.message || '网络异常')
    }
    return Promise.reject(err)
  }
)

export default http
