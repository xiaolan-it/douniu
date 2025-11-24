// 修复 global 未定义的问题
if (typeof global === 'undefined') {
  var global = globalThis
}

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useUserStore } from './stores/user'
import axios from 'axios'
import './style.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)

// 应用启动时，如果有token，设置到axios
// 注意：不在启动时恢复用户信息，让路由守卫来处理
// 这样可以避免重复请求，并且路由守卫会等待恢复完成
const token = localStorage.getItem('token')
if (token) {
  // 设置token到axios，路由守卫会负责恢复用户信息
  axios.defaults.headers.common['satoken'] = token
}

app.mount('#app')

