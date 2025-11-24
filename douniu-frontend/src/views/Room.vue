<template>
  <div class="min-h-screen room-page-bg p-4">
    <div class="max-w-4xl mx-auto">
      <!-- 顶部用户信息栏 -->
      <div class="bg-white rounded-lg shadow-lg p-4 mb-4">
        <div class="flex justify-between items-center mb-3">
          <!-- 用户信息 -->
          <div class="flex items-center gap-16">
            <div class="flex flex-col">
              <div class="text-xs text-gray-500 mb-1">昵称</div>
              <div class="text-base font-bold text-gray-800">{{ userStore.user?.nickname || '未知用户' }}</div>
            </div>
            <!-- <div class="flex flex-col">
              <div class="text-xs text-gray-500 mb-1">账号</div>
              <div class="text-base font-semibold text-gray-700">{{ userStore.user?.phone || '未知' }}</div>
            </div> -->
            <div class="flex flex-col">
              <div class="text-xs text-gray-500 mb-1">历史总盈亏</div>
              <div @click="router.push('/records')" style="text-decoration: underline;" class="text-base font-bold" :class="userStore.user?.balance >= 0 ? 'text-green-600' : 'text-red-600'">
                ¥{{ userStore.user?.balance || 0 }}
              </div>
            
            </div>
            <button
              @click="handleLogout"
              class="px-4 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 text-sm"
            >
              退出
            </button>
          </div>
        </div>
        
        <!-- 游戏模式切换开关（单独一行） -->
        <div class="flex items-center justify-center gap-2 pt-3 border-t border-gray-200">
          <span class="text-sm text-gray-600">牌桌</span>
          <label class="relative inline-flex items-center cursor-pointer">
            <input
              type="checkbox"
              v-model="useSimpleMode"
              @change="saveGameMode"
              class="sr-only peer"
            />
            <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
          </label>
          <span class="text-sm text-gray-600">列表模式</span>
        </div>
      </div>

      <!-- 主要内容区域 -->
      <div class="space-y-4">
        <!-- 创建/加入房间区域 -->
        <div class="bg-white rounded-lg shadow-lg p-6">
          <div class="flex justify-between items-center mb-4">
            <h2 class="text-xl font-bold text-gray-800">房间操作</h2>
            <button
              @click="showCreateModal = true"
              class="px-4 py-2 bg-gradient-to-r from-amber-500 to-yellow-500 text-white rounded-lg hover:from-amber-600 hover:to-yellow-600 text-sm font-medium shadow-md transition-all"
            >
              + 创建房间
            </button>
          </div>
          
          <!-- 加入房间输入 -->
          <!-- <div class="flex gap-2 mb-4">
            <input
              v-model="roomCodeInput"
              type="text"
              placeholder="输入房间号加入"
              class="flex-1 px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <button
              @click="handleJoinRoom"
              class="px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 font-medium"
            >
              加入
            </button>
          </div> -->
          
          <!-- 快捷加入：可用房间列表 -->
          <div class="border-t border-gray-200 pt-4">
            <div class="flex justify-between items-center mb-3">
              <h3 class="text-lg font-semibold text-gray-700">可加入的房间</h3>
              <button
                @click="loadAvailableRooms"
                class="px-3 py-1 text-sm bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200"
              >
                刷新
              </button>
            </div>
            
            <div v-if="loadingRooms" class="text-center py-8 text-gray-500">
              加载中...
            </div>
            <div v-else-if="availableRooms.length === 0" class="text-center py-8 text-gray-500">
              暂无可用房间
            </div>
            <div v-else class="space-y-2 max-h-64 overflow-y-auto">
              <div
                v-for="room in availableRooms"
                :key="room.id"
                @click="handleQuickJoin(room.roomCode)"
                class="p-4 border-2 border-gray-200 rounded-lg hover:border-blue-400 hover:bg-blue-50 cursor-pointer transition-all"
              >
                <div class="flex justify-between items-center">
                  <div class="flex-1">
                    <div class="flex items-center gap-2 mb-2">
                      <span class="font-semibold text-gray-800">创建人: {{ room.creatorNickname || '未知' }}</span>
                      <span
                        :class="{
                          'bg-yellow-100 text-yellow-800': room.status === 0,
                          'bg-green-100 text-green-800': room.status === 1
                        }"
                        class="px-2 py-0.5 text-xs rounded font-medium"
                      >
                        {{ room.status === 0 ? '等待中' : '游戏中' }}
                      </span>
                    </div>
                    <div class="text-sm text-gray-600 space-x-3">
                      <span>玩家: {{ room.playerCount || 0 }}</span>
                      <span>|</span>
                      <span>第 {{ room.currentRound || 0 }}/{{ room.maxRounds || 0 }} 局</span>
                    </div>
                  </div>
                  <button
                    class="ml-4 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 text-sm font-medium"
                    @click.stop="handleQuickJoin(room.roomCode)"
                  >
                    加入
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

      <!-- 创建房间模态框 -->
      <div v-if="showCreateModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div class="bg-white rounded-lg p-6 w-full max-w-md">
          <h2 class="text-xl font-bold mb-4">创建房间</h2>
          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium mb-2">总局数</label>
              <input
                v-model.number="createForm.maxRounds"
                type="number"
                min="1"
                max="100"
                class="w-full px-4 py-2 border rounded-lg"
                placeholder="默认20局"
              />
            </div>
            <div>
              <label class="block text-sm font-medium mb-2">可选牌型（可玩性更高、更刺激）</label>
              <div class="space-y-2">
                <label v-for="type in cardTypes" :key="type.name" class="flex items-start p-3 border rounded-lg hover:bg-gray-50 cursor-pointer">
                  <input
                    type="checkbox"
                    v-model="createForm.enabledCardTypes"
                    :value="type.name"
                    class="mt-1 mr-3"
                  />
                  <div class="flex-1">
                    <div class="font-semibold text-gray-800">
                      {{ type.name }} (×{{ type.multiplier }})
                    </div>
                    <div class="text-sm text-gray-600 mt-1">
                      {{ type.description }}
                    </div>
                    <div class="text-sm text-gray-500 mt-1">
                      {{ type.example }}
                    </div>
                  </div>
                </label>
              </div>
            </div>
            <div class="flex gap-2">
              <button
                @click="handleCreateRoom"
                class="flex-1 bg-green-500 text-white py-2 rounded-lg hover:bg-green-600"
              >
                创建
              </button>
              <button
                @click="showCreateModal = false"
                class="flex-1 bg-gray-300 text-gray-700 py-2 rounded-lg hover:bg-gray-400"
              >
                取消
              </button>
            </div>
          </div>
        </div>
      </div>

        <!-- 当前房间信息 -->
        <div v-if="currentRoom" class="bg-white rounded-lg shadow-lg p-6">
          <div class="flex justify-between items-center mb-4">
            <div>
              <h2 class="text-xl font-bold text-gray-800">我的房间</h2>
              <p class="text-sm text-gray-600 mt-1">房间号: {{ currentRoom.roomCode }}</p>
              <p class="text-sm text-gray-600">第 {{ currentRoom.currentRound }} / {{ currentRoom.maxRounds }} 局</p>
            </div>
            <button
              @click="handleEnterGame"
              class="px-6 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 font-medium"
            >
              进入游戏
            </button>
          </div>
          <div class="border-t border-gray-200 pt-4">
            <p class="text-sm text-gray-600 mb-2">房间链接：</p>
            <div class="flex gap-2">
              <input
                :value="roomUrl"
                readonly
                class="flex-1 px-4 py-2 border rounded-lg bg-gray-50 text-sm"
              />
              <button
                @click="copyRoomUrl"
                class="px-4 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 text-sm"
              >
                复制
              </button>
            </div>
          </div>
        </div>
      </div>
       
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import api from '@/utils/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const showCreateModal = ref(false)
const currentRoom = ref(null)
const roomCodeInput = ref('')
const availableRooms = ref([])
const loadingRooms = ref(false)
// 游戏模式：false=牌桌模式(game)，true=列表模式(gameSimple)
const useSimpleMode = ref(localStorage.getItem('gameMode') === 'simple')
const createForm = ref({
  maxRounds: 20,
  enabledCardTypes: ['五小牛','炸弹牛', '顺子', '五花牛'] 
})

// 必选牌型（不在界面显示，后端会自动添加）
const requiredCardTypes = ['无牛', '牛1', '牛2', '牛3', '牛4', '牛5', '牛6', '牛7', '牛8', '牛9', '牛牛']

// 可选牌型（在界面显示，用户可以选择）
const cardTypes = [
  { 
    name: '五小牛', 
    multiplier: 5,
    description: '5张牌都小于5，且总和≤10',
    example: '例：AA223'
  },
  { 
    name: '炸弹牛', 
    multiplier: 4,
    description: '4张相同点数的牌',
    example: '例：AAAAK'
  },
  { 
    name: '五花牛', 
    multiplier: 4,
    description: '5张都是J、Q、K',
    example: '例：JJQQK'
  },
  { 
    name: '顺子', 
    multiplier: 4,
    description: '5张牌点数连续',
    example: '例：A2345'
  }
]

const roomUrl = computed(() => {
  if (currentRoom.value) {
    const gamePath = useSimpleMode.value ? 'game-simple' : 'game'
    // 使用当前页面的基础路径
    const basePath = window.location.pathname.includes('/room') 
      ? window.location.pathname.replace('/room', '').replace(/\/$/, '') || '/douniu'
      : '/douniu'
    return `${window.location.origin}${basePath}/${gamePath}/${currentRoom.value.roomCode}`
  }
  return ''
})

// 获取游戏路由路径
const getGameRoute = (roomCode) => {
  return useSimpleMode.value ? `/game-simple/${roomCode}` : `/game/${roomCode}`
}

// 保存游戏模式到 localStorage
const saveGameMode = () => {
  localStorage.setItem('gameMode', useSimpleMode.value ? 'simple' : 'table')
}

const handleCreateRoom = async () => {
  try {
    const response = await api.post('/room/create', createForm.value)
    if (response.data.code === 200) {
      currentRoom.value = response.data.data
      showCreateModal.value = false
      // 自动跳转到游戏页面（根据模式选择）
      router.push(getGameRoute(currentRoom.value.roomCode))
    }
  } catch (error) {
    alert(error.response?.data?.message || '创建房间失败')
  }
}

const handleJoinRoom = async () => {
  if (!roomCodeInput.value) {
    alert('请输入房间号')
    return
  }
  
  // 验证房间是否存在
  try {
    const response = await api.get(`/room/code/${roomCodeInput.value}`)
    if (response.data.code === 200 && response.data.data) {
      // 房间存在，跳转到游戏页面（根据模式选择）
      router.push(getGameRoute(roomCodeInput.value))
    } else {
      alert('房间不存在')
    }
  } catch (error) {
    if (error.response?.status === 404 || error.response?.data?.code === 500) {
      alert(error.response?.data?.message || '房间不存在')
    } else {
      alert('加入房间失败，请稍后重试')
    }
  }
}

const handleEnterGame = () => {
  if (currentRoom.value) {
    router.push(getGameRoute(currentRoom.value.roomCode))
  }
}

const copyRoomUrl = () => {
  navigator.clipboard.writeText(roomUrl.value)
  alert('房间链接已复制')
}

const handleGoHome = () => {
  // 清除当前房间状态，返回首页
  currentRoom.value = null
  roomCodeInput.value = ''
  showCreateModal.value = false
  // 重新加载可用房间列表
  loadAvailableRooms()
  // 如果URL中有房间号参数，清除它
  if (route.params.roomCode) {
    router.push('/room')
  }
}

const handleLogout = async () => {
  await userStore.logout()
  router.push('/login')
}

const loadAvailableRooms = async () => {
  loadingRooms.value = true
  try {
    const response = await api.get('/room/available')
    if (response.data.code === 200) {
      availableRooms.value = response.data.data || []
    }
  } catch (error) {
    console.error('加载可用房间失败', error)
    availableRooms.value = []
  } finally {
    loadingRooms.value = false
  }
}

const handleQuickJoin = (roomCode) => {
  roomCodeInput.value = roomCode
  handleJoinRoom()
}

// 刷新数据的函数
const refreshData = async () => {
  // 刷新用户信息（包括余额）
  await userStore.fetchCurrentUser()
  // 刷新可加入的房间列表
  await loadAvailableRooms()
}

// 标记是否是首次加载
let isFirstMount = true

onMounted(() => {
  // 每次进入页面都刷新数据（包括从其他页面返回时）
  refreshData()
  
  // 如果是首次加载且有房间号参数，自动加入
  if (isFirstMount) {
    isFirstMount = false
    const roomCode = route.params.roomCode
    if (roomCode) {
      roomCodeInput.value = roomCode
      handleJoinRoom()
    }
  }
})
</script>

