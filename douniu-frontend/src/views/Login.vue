<template>
  <div class="min-h-screen login-page-bg flex items-center justify-center p-4 relative overflow-hidden">
    <!-- 背景装饰图片 -->
    <div class="absolute inset-0 pointer-events-none">
      <!-- <img 
        :src="beautyImage" 
        alt="beauty" 
        class="absolute top-10 right-10 w-32 h-32 opacity-30 object-contain"
      />
      <img 
        :src="niuniuImage" 
        alt="niuniu" 
        class="absolute bottom-10 left-10 w-24 h-24 opacity-20 object-contain"
      /> -->
    </div>
    
    <div class="relative z-10 bg-gradient-to-br from-amber-50/95 to-yellow-50/95 backdrop-blur-sm rounded-2xl shadow-2xl p-8 w-full max-w-md border-2 border-amber-200">
      <!-- Logo -->
      <div class="flex justify-center mb-6">
        <img 
          :src="logoImage" 
          alt="logo" 
          class="w-24 h-24 object-contain"
        />
      </div>
      <h1 class="text-3xl font-bold text-center mb-8 text-gray-800">屌丝牛牛</h1>
      
      <div v-if="isRegister" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">账号</label>
          <input
            v-model="phone"
            type="text"
            placeholder="请输入账号"
            class="w-full px-4 py-3 border-2 border-amber-300 rounded-lg focus:ring-2 focus:ring-amber-500 focus:border-amber-500 bg-white/90"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">密码</label>
          <input
            v-model="password"
            type="password"
            placeholder="请输入密码"
            class="w-full px-4 py-3 border-2 border-amber-300 rounded-lg focus:ring-2 focus:ring-amber-500 focus:border-amber-500 bg-white/90"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">昵称</label>
          <input
            v-model="nickname"
            type="text"
            placeholder="请输入昵称"
            class="w-full px-4 py-3 border-2 border-amber-300 rounded-lg focus:ring-2 focus:ring-amber-500 focus:border-amber-500 bg-white/90"
          />
        </div>
        <button
          @click="handleRegister"
          :disabled="loading"
          class="w-full bg-gradient-to-r from-amber-500 to-yellow-500 text-white py-3 rounded-lg font-semibold hover:from-amber-600 hover:to-yellow-600 disabled:opacity-50 disabled:cursor-not-allowed transition shadow-lg"
        >
          {{ loading ? '注册中...' : '注册' }}
        </button>
        <p class="text-center text-sm text-gray-700">
          已有账号？
          <a @click="isRegister = false" class="text-amber-600 hover:text-amber-700 hover:underline cursor-pointer font-semibold">立即登录</a>
        </p>
      </div>

      <div v-else class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">账号</label>
          <input
            v-model="phone"
            type="text"
            placeholder="请输入账号"
            class="w-full px-4 py-3 border-2 border-amber-300 rounded-lg focus:ring-2 focus:ring-amber-500 focus:border-amber-500 bg-white/90"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">密码</label>
          <input
            v-model="password"
            type="password"
            placeholder="请输入密码"
            class="w-full px-4 py-3 border-2 border-amber-300 rounded-lg focus:ring-2 focus:ring-amber-500 focus:border-amber-500 bg-white/90"
          />
        </div>
        <button
          @click="handleLogin"
          :disabled="loading"
          class="w-full bg-gradient-to-r from-amber-500 to-yellow-500 text-white py-3 rounded-lg font-semibold hover:from-amber-600 hover:to-yellow-600 disabled:opacity-50 disabled:cursor-not-allowed transition shadow-lg"
        >
          {{ loading ? '登录中...' : '登录' }}
        </button>
        <p class="text-center text-sm text-gray-700">
          还没有账号？
          <a @click="isRegister = true" class="text-amber-600 hover:text-amber-700 hover:underline cursor-pointer font-semibold">立即注册</a>
        </p>
      </div>

      <div v-if="error" class="mt-4 p-3 bg-red-100/90 border-2 border-red-400 text-red-700 rounded-lg backdrop-blur-sm">
        {{ error }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page-bg {
  background: #072c44;
  background-attachment: fixed;
}
</style>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import beautyImage from '@/assets/beauty.png'
import niuniuImage from '@/assets/niuniu.png'
import logoImage from '@/assets/logo.png'

const router = useRouter()
const userStore = useUserStore()

const isRegister = ref(false)
const phone = ref('')
const password = ref('')
const nickname = ref('')
const loading = ref(false)
const error = ref('')

const handleLogin = async () => {
  if (!phone.value || !password.value) {
    error.value = '请填写完整信息'
    return
  }
  
  loading.value = true
  error.value = ''
  
  try {
    await userStore.login(phone.value, password.value)
    router.push('/room')
  } catch (err) {
    error.value = err.message || '登录失败'
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!phone.value || !password.value || !nickname.value) {
    error.value = '请填写完整信息'
    return
  }
  
  loading.value = true
  error.value = ''
  
  try {
    await userStore.register(phone.value, password.value, nickname.value)
    router.push('/room')
  } catch (err) {
    error.value = err.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

