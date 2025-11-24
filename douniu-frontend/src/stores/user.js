import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || '')

  // 设置token
  if (token.value) {
    axios.defaults.headers.common['satoken'] = token.value
  }

  const isLoggedIn = ref(!!token.value)

  const login = async (phone, password) => {
    try {
      const response = await axios.post('/api/auth/login', { phone, password })
      if (response.data.code === 200) {
        const loginData = response.data.data
        
        // 从响应体中获取用户信息和token
        user.value = loginData.user
        const newToken = loginData.token || 
                        response.headers['satoken'] || 
                        response.headers['satoken'] ||
                        token.value
        
        // 保存token到localStorage和store
        if (newToken) {
          token.value = newToken
          localStorage.setItem('token', newToken)
          axios.defaults.headers.common['satoken'] = newToken
          isLoggedIn.value = true
        }
        
        return true
      }
      throw new Error(response.data.message || '登录失败')
    } catch (error) {
      throw error
    }
  }

  const register = async (phone, password, nickname) => {
    try {
      const response = await axios.post('/api/auth/register', { phone, password, nickname })
      if (response.data.code === 200) {
        // 注册成功后自动登录
        return await login(phone, password)
      }
      throw new Error(response.data.message || '注册失败')
    } catch (error) {
      throw error
    }
  }

  const logout = async () => {
    try {
      await axios.post('/api/auth/logout')
    } catch (error) {
      console.error('退出登录失败', error)
    } finally {
      user.value = null
      token.value = ''
      localStorage.removeItem('token')
      delete axios.defaults.headers.common['satoken']
      isLoggedIn.value = false
    }
  }

  const fetchCurrentUser = async () => {
    try {
      const storedToken = localStorage.getItem('token')
      if (!storedToken) {
        isLoggedIn.value = false
        user.value = null
        return false
      }
      
      // 确保token已设置到axios
      axios.defaults.headers.common['satoken'] = storedToken
      token.value = storedToken
      
      const response = await axios.get('/api/auth/me')
      if (response.data.code === 200 && response.data.data) {
        user.value = response.data.data
        isLoggedIn.value = true
        return true
      } else {
        // 响应成功但没有用户数据，清除token
        localStorage.removeItem('token')
        token.value = ''
        isLoggedIn.value = false
        user.value = null
        return false
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
      // 如果获取用户信息失败，清除token
      localStorage.removeItem('token')
      token.value = ''
      isLoggedIn.value = false
      user.value = null
      return false
    }
  }

  return {
    user,
    token,
    isLoggedIn,
    login,
    register,
    logout,
    fetchCurrentUser
  }
})

