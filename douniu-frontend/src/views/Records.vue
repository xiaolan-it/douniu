<template>
  <div class="min-h-screen bg-gradient-to-br from-green-400 to-blue-500 p-4">
    <div class="max-w-4xl mx-auto">
      <div class="bg-white rounded-lg shadow-lg p-6">
        <div class="flex justify-between items-center mb-6">
          <div class="flex items-center gap-4">
            <button
            
              @click="backToRooms"
              class="px-4 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600"
            >
              ← 
            </button>
            <h1 class="text-2xl font-bold">{{ selectedRoom ? '对局记录' : '房间记录' }}</h1>
          </div>
        
        </div>

        <!-- 房间列表 -->
        <div v-if="!selectedRoom">
          <div v-if="loading" class="text-center py-8">加载中...</div>
          <div v-else-if="rooms.length === 0" class="text-center py-8 text-gray-500">
            暂无房间记录
          </div>
          <div v-else class="space-y-3">
            <div
              v-for="room in rooms"
              :key="room.id"
              class="border-2 rounded-lg p-5 hover:border-blue-400 hover:shadow-md transition-all bg-white"
            >
              <div class="flex justify-between items-start gap-4">
                <!-- 左侧：房间信息 -->
                <div class="flex-1 min-w-0">
                  <!-- 第一行：发起人 -->
                  <div class="mb-3">
                    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                  <div class="flex flex-col">
                    <span class="font-bold text-lg text-gray-900">发起人: {{ room.creatorNickname || '未知' }}</span>
                 
                    </div>
                    <div class="flex flex-col" style="display: inline-block; text-align: right;">
                       <!-- 状态标签 -->
                   <span
                    :class="getStatusClass(room.status)"
                    class="px-3 py-1 text-xs font-semibold rounded-full whitespace-nowrap"
                  >
                    {{ getStatusText(room.status) }}
                  </span>
                    </div>
                    </div>
                  </div>
                  
                  <!-- 第二行：详细信息网格 -->
                  <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                    <div class="flex flex-col">
                      <span class="text-gray-500 text-xs mb-1">盈利</span>
                      <span class="font-semibold text-gray-800 truncate"> <span
                        :class="room.totalProfit > 0 ? 'text-green-600' : room.totalProfit < 0 ? 'text-red-600' : 'text-gray-500'"
                        class="font-bold text-lg"
                      >
                        {{ room.totalProfit > 0 ? '+' : '' }}{{ room.totalProfit || 0 }}
                      </span></span>
                    </div>
                    <div class="flex flex-col">
                      <span class="text-gray-500 text-xs mb-1">参与人数</span>
                      <span class="font-semibold text-gray-800">{{ room.playerCount || 0 }}</span>
                    </div>
                    <div class="flex flex-col">
                      <span class="text-gray-500 text-xs mb-1">局数</span>
                      <span class="font-semibold text-gray-800">第 {{ room.currentRound || 0 }}/{{ room.maxRounds || 0 }} 局</span>
                    </div>
                    <div class="flex flex-col">
                      <span class="text-gray-500 text-xs mb-1">创建时间</span>
                      <span class="font-semibold text-gray-800">{{ formatTime(room.createdAt) }}</span>
                    </div>
                  </div>
                  
                  <!-- 第三行：总盈利 -->
                  <div class="mt-3 pt-3 border-t border-gray-200">
                    <button
                    class="text-blue-600 hover:text-blue-800 hover:underline text-sm font-medium transition-colors"
                    @click.stop="selectRoom(room)"
                  >
                    查看对局 →
                  </button>
                  </div>
                </div>
                
                <!-- 右侧：状态和操作 -->
                <div class="flex flex-col items-end gap-3 flex-shrink-0">
                 
                  <!-- 查看对局链接 -->
                 
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 对局记录列表 -->
        <div v-else>
          <div class="mb-4 p-4 bg-gradient-to-r from-blue-50 to-blue-100 rounded-lg border border-blue-200">
            <p class="font-semibold text-lg text-gray-800 mb-3">发起人: {{ selectedRoom.creatorNickname || '未知' }}</p>
            <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
             
              <div class="flex flex-col">
                <span class="text-gray-600 text-xs mb-1">我收支</span>
                <span
                  :class="selectedRoom.totalProfit > 0 ? 'text-green-600' : selectedRoom.totalProfit < 0 ? 'text-red-600' : 'text-gray-500'"
                  class="font-bold text-lg"
                >
                  {{ selectedRoom.totalProfit > 0 ? '+' : '' }}{{ selectedRoom.totalProfit || 0 }}
                </span>
              </div>

              <div class="flex flex-col">
                <span class="text-gray-600 text-xs mb-1">参与人数</span>
                <span class="font-semibold text-gray-800">{{ selectedRoom.playerCount || 0 }}</span>
              </div>
              <div class="flex flex-col">
                <span class="text-gray-600 text-xs mb-1">对局数</span>
                <span class="font-semibold text-gray-800">第 {{ selectedRoom.currentRound || 0 }}/{{ selectedRoom.maxRounds || 0 }} 局</span>
              </div>
             
              <div class="flex flex-col">
                <span class="text-gray-600 text-xs mb-1">状态</span>
                <span
                  :class="getStatusClass(selectedRoom.status)"
                  class="px-2 py-1 text-xs font-semibold rounded-full inline-block w-fit"
                >
                  {{ getStatusText(selectedRoom.status) }}
                </span>
              </div>
            </div>
          </div>

          <!-- 房间参与玩家盈利情况 -->
          <div class="mb-4 p-4 bg-gradient-to-r from-purple-50 to-pink-50 rounded-lg border border-purple-200">
            <h3 class="font-semibold text-lg text-gray-800 mb-3">参与玩家盈利情况</h3>
            <div v-if="loadingPlayers" class="text-center py-4 text-gray-500 text-sm">
              加载中...
            </div>
            <div v-else-if="roomPlayers.length === 0" class="text-center py-4 text-gray-500 text-sm">
              暂无玩家信息
            </div>
            <div v-else class="overflow-x-auto">
              <table class="w-full">
                <thead>
                  <tr class="border-b border-purple-200">
                    <th class="text-left py-2 px-3 text-sm font-semibold text-gray-700">昵称</th>
                    <th class="text-right py-2 px-3 text-sm font-semibold text-gray-700">收支</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="player in roomPlayers"
                    :key="player.userId"
                    class="border-b border-purple-100 hover:bg-purple-50 transition-colors"
                  >
                    <td class="py-2 px-3">
                      <div class="flex items-center gap-2">
                        <span class="font-semibold text-gray-800">{{ player.nickname || `玩家${player.seatNumber}` }}</span>
                        <span v-if="player.isDealer === 1" class="px-2 py-0.5 bg-yellow-100 text-yellow-800 text-xs rounded">庄</span>
                        <span v-if="player.userId === currentUserId" class="px-2 py-0.5 bg-blue-100 text-blue-800 text-xs rounded">我</span>
                      </div>
                    </td>
                    <td class="py-2 px-3 text-right">
                      <span
                        :class="player.totalScore > 0 ? 'text-green-600 font-bold text-lg' : player.totalScore < 0 ? 'text-red-600 font-bold text-lg' : 'text-gray-500'"
                      >
                        {{ player.totalScore > 0 ? '+' : '' }}{{ player.totalScore || 0 }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          
          <div v-if="loadingRecords" class="text-center py-8">加载中...</div>
          <div v-else-if="records.length === 0" class="text-center py-8 text-gray-500">
            该房间暂无对局记录
          </div>
          <div v-else class="space-y-3">
            <div
              v-for="record in records"
              :key="record.id"
              class="border rounded-lg overflow-hidden"
            >
              <!-- 对局记录头部（可点击展开） -->
              <div
                class="p-4 hover:bg-gray-50 cursor-pointer transition-colors"
                @click="toggleRecordDetails(record.id)"
              >
                <div class="flex justify-between items-center">
                  <div class="flex-1">
                    <div class="flex items-center gap-3 mb-2">
                      <span class="font-semibold text-lg">第 {{ record.roundNumber }} 局</span>
                      <span v-if="record.cardType" class="px-2 py-1 bg-amber-100 text-amber-800 text-sm rounded">
                        {{ record.cardType }}
                      </span>
                      <span class="text-xs text-gray-500">
                        {{ expandedRecords[record.id] ? '▼' : '▶' }}
                      </span>
                    </div>
                    <div class="text-sm text-gray-600">
                      <span>{{ formatTime(record.startTime) }}</span>
                      <span v-if="record.betAmount" class="mx-2">|</span>
                      <span v-if="record.betAmount">投注: {{ record.betAmount }}元</span>
                    </div>
                  </div>
                  <div class="text-right">
                    <p class="text-sm text-gray-600 mb-1">收支</p>
                    <p
                      :class="record.scoreChange > 0 ? 'text-green-600 font-bold text-lg' : record.scoreChange < 0 ? 'text-red-600 font-bold text-lg' : 'text-gray-500'"
                    >
                      {{ record.scoreChange > 0 ? '+' : '' }}{{ record.scoreChange || 0 }}
                    </p>
                  </div>
                </div>
              </div>
              
              <!-- 展开的详情（所有玩家的牌面） -->
              <div
                v-if="expandedRecords[record.id]"
                class="border-t bg-gray-50 p-4"
              >
                <div v-if="loadingDetails[record.id]" class="text-center py-4 text-gray-500 text-sm">
                  加载中...
                </div>
                <div v-else-if="recordDetails[record.id] && recordDetails[record.id].length > 0" class="space-y-3">
                  <div
                    v-for="detail in recordDetails[record.id]"
                    :key="detail.id"
                    :class="[
                      'rounded-lg p-3 border-2 transition-all',
                      detail.userId === currentUserId
                        ? 'bg-gradient-to-r from-blue-50 to-blue-100 border-blue-400 shadow-md'
                        : 'bg-white border-gray-200'
                    ]"
                  >
                    <div class="flex items-center justify-between mb-2">
                      <div class="flex items-center gap-2">
                        <span :class="detail.userId === currentUserId ? 'font-bold text-blue-800' : 'font-semibold text-gray-800'">
                          {{ detail.nickname || `玩家${detail.seatNumber}` }}
                        </span>
                        <span v-if="detail.userId === currentUserId" class="px-2 py-0.5 bg-blue-500 text-white text-xs rounded font-bold">
                          我
                        </span>
                        <span v-if="detail.isDealer" class="px-2 py-0.5 bg-yellow-100 text-yellow-800 text-xs rounded">
                          庄
                        </span>
                        <span v-if="detail.cardType" class="px-2 py-0.5 bg-amber-100 text-amber-800 text-xs rounded">
                          {{ detail.cardType }}
                          <span v-if="detail.multiplier" class="ml-1">×{{ detail.multiplier }}</span>
                        </span>
                      </div>
                      <div class="text-right">
                        <span class="text-xs text-gray-500">投注: {{ detail.betAmount || 0 }}元</span>
                        <p
                          :class="detail.scoreChange > 0 ? 'text-green-600 font-bold' : detail.scoreChange < 0 ? 'text-red-600 font-bold' : 'text-gray-500'"
                          class="text-sm"
                        >
                          {{ detail.scoreChange > 0 ? '+' : '' }}{{ detail.scoreChange || 0 }}
                        </p>
                      </div>
                    </div>
                    <!-- 牌面显示 -->
                    <div v-if="detail.cardsArray && detail.cardsArray.length > 0" class="flex gap-2 mt-2">
                      <Card
                        v-for="(card, index) in detail.cardsArray"
                        :key="index"
                        :suit="card.suit"
                        :rank="card.rank"
                        :hidden="false"
                      />
                    </div>
                  </div>
                </div>
                <div v-else class="text-center py-4 text-gray-500 text-sm">
                  暂无详情
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'
import { useUserStore } from '@/stores/user'
import Card from '@/components/Card.vue'

const router = useRouter()
const userStore = useUserStore()
const currentUserId = computed(() => userStore.user?.id)

const rooms = ref([])
const records = ref([])
const selectedRoom = ref(null)
const loading = ref(false)
const loadingRecords = ref(false)
const expandedRecords = ref({}) // 记录哪些对局已展开
const recordDetails = ref({}) // 存储每个对局的详情
const loadingDetails = ref({}) // 记录哪些对局详情正在加载
const roomPlayers = ref([]) // 房间参与玩家列表
const loadingPlayers = ref(false) // 是否正在加载玩家信息

// 加载用户参与的房间列表
const fetchRooms = async () => {
  loading.value = true
  try {
    const response = await api.get('/game-record/user/rooms')
    if (response.data.code === 200) {
      rooms.value = response.data.data || []
    }
  } catch (error) {
    console.error('获取房间列表失败', error)
    rooms.value = []
  } finally {
    loading.value = false
  }
}

// 选择房间，加载该房间的对局记录
const selectRoom = async (room) => {
  selectedRoom.value = room
  loadingRecords.value = true
  loadingPlayers.value = true
  
  // 并行加载对局记录和玩家信息
  try {
    const [recordsResponse, playersResponse] = await Promise.all([
      api.get(`/game-record/room/${room.id}`),
      api.get(`/room/${room.id}/players`)
    ])
    
    if (recordsResponse.data.code === 200) {
      records.value = recordsResponse.data.data || []
    }
    
    if (playersResponse.data.code === 200) {
      roomPlayers.value = playersResponse.data.data || []
    }
  } catch (error) {
    console.error('获取数据失败', error)
    records.value = []
    roomPlayers.value = []
  } finally {
    loadingRecords.value = false
    loadingPlayers.value = false
  }
}

// 返回房间列表
const backToRooms = () => {
  if(selectedRoom.value){
    selectedRoom.value = null
    records.value = []
    roomPlayers.value = []
  } else {
    router.push('/room')
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

const getStatusText = (status) => {
  if (status === 0) return '等待中'
  if (status === 1) return '游戏中'
  return '已结束'
}

const getStatusClass = (status) => {
  if (status === 0) return 'bg-yellow-100 text-yellow-800'
  if (status === 1) return 'bg-green-100 text-green-800'
  return 'bg-gray-100 text-gray-800'
}

// 切换对局详情展开/收起
const toggleRecordDetails = async (recordId) => {
  if (expandedRecords.value[recordId]) {
    // 收起
    expandedRecords.value[recordId] = false
  } else {
    // 展开
    expandedRecords.value[recordId] = true
    
    // 如果还没有加载过详情，则加载
    if (!recordDetails.value[recordId]) {
      await fetchRecordDetails(recordId)
    }
  }
}

// 获取对局详情（所有玩家的牌面）
const fetchRecordDetails = async (recordId) => {
  loadingDetails.value[recordId] = true
  try {
    const response = await api.get(`/game-record/${recordId}/details`)
    if (response.data.code === 200) {
      const details = response.data.data || []
      
      // 解析牌面JSON
      const detailsWithCards = details.map((detail) => {
        try {
          // 解析牌面JSON
          const cardsArray = typeof detail.cards === 'string' 
            ? JSON.parse(detail.cards) 
            : detail.cards || []
          
          return {
            ...detail,
            cardsArray,
            nickname: detail.nickname || `玩家${detail.seatNumber}`,
            isDealer: detail.isDealer || false
          }
        } catch (e) {
          console.error('解析牌面失败', e)
          return {
            ...detail,
            cardsArray: [],
            nickname: detail.nickname || `玩家${detail.seatNumber}`,
            isDealer: detail.isDealer || false
          }
        }
      })
      
      recordDetails.value[recordId] = detailsWithCards
    }
  } catch (error) {
    console.error('获取对局详情失败', error)
    recordDetails.value[recordId] = []
  } finally {
    loadingDetails.value[recordId] = false
  }
}

onMounted(() => {
  fetchRooms()
})
</script>

