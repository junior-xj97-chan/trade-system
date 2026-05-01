import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/api'
import type { User } from '@/types'

export const useUserStore = defineStore('user', () => {
  // 读取时清理 "Bearer " 前缀（兼容旧数据）
  const storedToken = localStorage.getItem('token') || ''
  const token = ref<string | null>(storedToken.replace(/^Bearer\s*/i, ''))
  const userInfo = ref<User | null>(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)

  async function login(username: string, password: string) {
    const res = await userApi.login({ username, password })
    const loginData = res.data.data
    // 去掉 "Bearer " 前缀，只存储纯 token
    // axios 拦截器会再加 "Bearer " 前缀
    token.value = loginData.token.replace(/^Bearer\s*/i, '')
    userInfo.value = {
      id: loginData.userId,
      username: loginData.username,
    }
    localStorage.setItem('token', token.value)
    localStorage.setItem('user', JSON.stringify(userInfo.value))
  }

  /** 加载完整用户资料 */
  async function fetchProfile() {
    if (!userInfo.value?.id) return
    try {
      const res = await userApi.getInfo(userInfo.value.id)
      if (res.data.code === 0 || res.data.code === 200) {
        userInfo.value = { ...userInfo.value, ...res.data.data }
        localStorage.setItem('user', JSON.stringify(userInfo.value))
      }
    } catch {
      // ignore
    }
  }

  /** 退出登录：先调后端使 Token 失效，再清本地 */
  async function logout() {
    try {
      await userApi.logout()
    } catch {
      // 即使接口失败也清本地
    }
    token.value = null
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    logout,
    fetchProfile,
  }
})
