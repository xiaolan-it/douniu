import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/room/:roomCode?',
    name: 'Room',
    component: () => import('@/views/Room.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/game/:roomCode',
    name: 'Game',
    component: () => import('@/views/Game.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/game-simple/:roomCode',
    name: 'GameSimple',
    component: () => import('@/views/GameSimple.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/records',
    name: 'Records',
    component: () => import('@/views/Records.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory('/douniu/'),
  routes
})

// 标记是否正在恢复用户信息
let isRestoringUser = false
let restorePromise = null

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  // 如果路由需要认证
  if (to.meta.requiresAuth) {
    // 检查是否有token
    const token = localStorage.getItem('token')
    
    // 如果没有token，直接跳转登录
    if (!token) {
      next('/login')
      return
    }
    
    // 如果有token但用户信息未加载，需要先恢复用户信息
    if (!userStore.user) {
      // 如果正在恢复，等待恢复完成
      if (isRestoringUser && restorePromise) {
        try {
          await restorePromise
        } catch (error) {
          // 恢复失败，继续下面的检查
        }
      } else if (!isRestoringUser) {
        // 开始恢复用户信息
        isRestoringUser = true
        restorePromise = userStore.fetchCurrentUser()
        
        try {
          const success = await restorePromise
          if (!success) {
            // 获取用户信息失败，清除无效token
            isRestoringUser = false
            restorePromise = null
            next('/login')
            return
          }
        } catch (error) {
          // 获取用户信息出错
          isRestoringUser = false
          restorePromise = null
          next('/login')
          return
        } finally {
          isRestoringUser = false
          restorePromise = null
        }
      }
    }
    
    // 最终检查：确保用户信息已加载且登录状态为true
    if (!userStore.user || !userStore.isLoggedIn) {
      next('/login')
      return
    }
  }
  
  // 如果访问登录页且已登录，重定向到房间页
  if (to.path === '/login' && userStore.isLoggedIn && userStore.user) {
    next('/room')
    return
  }
  
  next()
})

export default router

