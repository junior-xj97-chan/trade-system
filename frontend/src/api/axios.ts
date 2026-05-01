import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import JSONBig from 'json-bigint'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 大数字（雪花ID）精度处理：超过 Number.MAX_SAFE_INTEGER 的自动转为 BigNumber 字符串
// @ts-ignore json-bigint 类型定义与实际导出不符，运行时行为正确
const JSONBigString = JSONBig({ storeAsString: true })

// 创建 Axios 实例
const http: AxiosInstance = axios.create({
  baseURL: '/api',  // 使用 Vite proxy 代理到 Gateway，统一加 /api 前缀匹配 Gateway 路由
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  // 响应数据中的大数字自动转为字符串，避免精度丢失
  transformResponse: [(data) => {
    if (data && typeof data === 'string') {
      try {
        return JSONBigString.parse(data)
      } catch {
        return data
      }
    }
    return data
  }],
})

// 请求拦截器：自动注入 Token
http.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      // 后端返回的 token 已包含 "Bearer " 前缀，直接使用
      config.headers['Authorization'] = token
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器：统一错误处理
http.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    // code=0 或 code=200 均表示业务成功
    if (res.code !== 0 && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      // Token 过期或无效
      if (res.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return response
  },
  (error) => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          router.push('/login')
          break
        case 403:
          ElMessage.error('无权限访问')
          break
        case 404:
          ElMessage.error('请求资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(error.response.data?.message || '网络请求失败')
      }
    } else {
      ElMessage.error('网络连接失败，请检查网络')
    }
    return Promise.reject(error)
  },
)

export default http
