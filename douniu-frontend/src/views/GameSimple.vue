<template>
  <div class="min-h-screen bg-gray-50">
    <!-- 顶部信息栏 -->
    <div class="bg-white shadow-sm sticky top-0 z-50">
      <div class="px-4 py-3 flex items-center justify-between">
        <!-- 左上角：操作按钮 -->
        <div class="flex items-center gap-2 relative">
          <!-- 准备按钮 -->
          <button
            v-if="gamePhase === 'waiting' && !isCurrentUserReady"
            @click="handleReady"
            class="px-4 py-2 bg-green-500 text-white rounded text-sm hover:bg-green-600 whitespace-nowrap shadow-md"
          >
            准备
          </button>
          <span v-else-if="gamePhase === 'waiting' && isCurrentUserReady" class="px-4 py-2 bg-gray-400 text-white rounded text-sm whitespace-nowrap shadow-md">
            已准备
          </span>
          <!-- 管理员操作按钮（下拉菜单） -->
          <div v-if="isAdmin" class="relative operation-menu-container">
            <button
              @click.stop="showOperationMenu = !showOperationMenu"
              class="px-3 py-1.5 bg-blue-500 text-white rounded text-xs hover:bg-blue-600 whitespace-nowrap shadow-md flex items-center gap-1"
            >
              操作
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </button>
            
            <!-- 下拉菜单 -->
            <div
              v-if="showOperationMenu"
              class="absolute top-full left-0 mt-1 bg-white rounded-lg shadow-xl border border-gray-200 min-w-[120px] z-50"
              @click.stop
            >
              <!-- 开局 -->
              <button
                v-if="!hasStarted"
                @click="handleStartGame(); showOperationMenu = false"
                class="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-green-50 hover:text-green-600 transition-colors flex items-center gap-2"
              >
                <span class="w-2 h-2 rounded-full bg-green-500"></span>
                开局
              </button>
              
              <!-- 指定庄家 -->
              <button
                v-if="gamePhase === 'waiting'"
                @click="showDealerModal = true; showOperationMenu = false"
                class="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-yellow-50 hover:text-yellow-600 transition-colors flex items-center gap-2"
              >
                <span class="w-2 h-2 rounded-full bg-yellow-500"></span>
                指定庄家
              </button>
              
              <!-- 结束对局 -->
              <button
                @click="handleFinishGame(); showOperationMenu = false"
                class="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-orange-50 hover:text-orange-600 transition-colors flex items-center gap-2"
              >
                <span class="w-2 h-2 rounded-full bg-orange-500"></span>
                结束对局
              </button>
            </div>
          </div>
        </div>
        
        <!-- 中间：局数显示 -->
        <div class="bg-gray-200 rounded-full px-6 py-2">
          <p class="text-gray-800 font-bold text-base">
            {{ room?.currentRound || 0 }}/{{ room?.maxRounds || 0 }}局
          </p>
        </div>
        
        <!-- 右上角：查看牌型按钮 -->
        <button
          @click="showCardTypesModal = true"
          class="px-3 py-1.5 bg-orange-500 text-white rounded text-xs hover:bg-orange-600 whitespace-nowrap shadow-md"
        >
          查看牌型
        </button>
      </div>
    </div>

    <!-- 倒计时显示 -->
    <div v-if="readyCountdown > 0 || revealCountdown > 0 || cardDisplayTime > 0 || (gamePhase === 'settling' && nextRoundCountdown > 0)" class="fixed top-20 left-1/2 transform -translate-x-1/2 z-50">
      <div class="bg-black bg-opacity-70 rounded-full px-8 py-4 backdrop-blur-sm" >
        <p v-if="readyCountdown > 0" class="text-2xl font-bold text-yellow-300 text-center" style="font-size:14px;">
          {{ readyCountdown }}秒后开始
        </p>
        <p v-else-if="revealCountdown > 0" class="text-2xl font-bold text-yellow-300 text-center" style="font-size:14px;">
          开牌倒计时: {{ revealCountdown }}秒
        </p>
        <p v-else-if="cardDisplayTime > 0" class="text-2xl font-bold text-yellow-300 text-center" style="font-size:14px;">
          展示牌型: {{ cardDisplayTime }}秒
        </p>
        <p v-else-if="gamePhase === 'settling' && nextRoundCountdown > 0" class="text-2xl font-bold text-yellow-300 text-center" style="font-size:14px;">
          {{ nextRoundCountdown }}秒后开始下一局
        </p>
      </div>
    </div>

    <!-- 玩家列表 -->
    <div class="px-4 py-4">
      <!-- 表头 -->
      <div class="bg-white rounded-t-lg border-b border-gray-200 px-4 py-3 flex items-center">
        <div class="flex-1 min-w-[120px]">
          <span class="text-sm font-semibold text-gray-600">玩家</span>
        </div>
        <div class="flex-1 min-w-[150px] flex justify-center">
          <span class="text-sm font-semibold text-gray-600">牌面</span>
        </div>
        <div class="w-20 text-center">
          <span class="text-sm font-semibold text-gray-600">下注</span>
        </div>
        <div class="w-24 text-center">
          <span class="text-sm font-semibold text-gray-600">牌型</span>
        </div>
        <div class="w-24 text-right">
          <span class="text-sm font-semibold text-gray-600">盈亏</span>
        </div>
      </div>

      <!-- 玩家行 -->
      <div
        v-for="player in sortedPlayersList"
        :key="player.userId"
        class="bg-white border-b border-gray-100 px-2 py-2 flex items-center hover:bg-gray-50 transition-colors"
        :class="{ 'bg-blue-50': player.userId === currentUserId }"
      >
        <!-- 左侧：玩家信息 -->
        <div class="flex-1 min-w-[120px]">
          <div class="flex items-center gap-2">
            <span v-if="player.isDealer === 1" class="px-2 py-0.5 bg-orange-500 text-white text-xs rounded font-bold">庄</span>
            <span class="font-medium text-gray-900">{{ player.nickname || `玩家${player.seatNumber}` }}</span>
            <span v-if="player.userId === currentUserId" class="px-2 py-0.5 bg-blue-500 text-white text-xs rounded">我</span>
            <span v-if="player.isReady === true" class="px-2 py-0.5 bg-green-500 text-white text-xs rounded">已准备</span>
          </div>
          <div class="mt-1">
            <span class="text-sm text-gray-600">¥{{ player.totalScore || 0 }}</span>
          </div>
        </div>

        <!-- 中间：牌面 -->
        <div class="flex-1 min-w-[150px] flex justify-center items-center">
          <!-- 如果已开牌，显示牌型分组 -->
          <template v-if="playerCardTypes[player.userId]?.cardType && playerCardTypes[player.userId]?.cardGroups">
            <div v-if="playerCardTypes[player.userId].cardGroups.group1" class="card-group">
              <Card
                v-for="(card, index) in playerCardTypes[player.userId].cardGroups.group1"
                :key="'g1-' + index"
                :suit="card.suit"
                :rank="card.rank"
                :hidden="false"
                :class="{ 'first-card': index === 0 }"
              />
            </div>
            <span v-if="playerCardTypes[player.userId].cardGroups.group1 && playerCardTypes[player.userId].cardGroups.group2" class="mx-1 text-gray-400">|</span>
            <div v-if="playerCardTypes[player.userId].cardGroups.group2" class="card-group">
              <Card
                v-for="(card, index) in playerCardTypes[player.userId].cardGroups.group2"
                :key="'g2-' + index"
                :suit="card.suit"
                :rank="card.rank"
                :hidden="false"
                :class="{ 'first-card': index === 0 }"
              />
            </div>
          </template>
          <!-- 如果未开牌，显示自己的4+1或其他玩家的5张背面 -->
          <template v-else>
            <template v-if="player.userId === currentUserId && getPlayerCards(player.userId)">
              <!-- 自己的牌：4张正面 + 1张背面 -->
              <Card
                v-for="(card, index) in getPlayerCards(player.userId).cards"
                :key="'card-' + index"
                :suit="card.suit"
                :rank="card.rank"
                :hidden="false"
              />
              <Card
                v-if="getPlayerCards(player.userId).hiddenCard"
                :suit="getPlayerCards(player.userId).hiddenCard.suit"
                :rank="getPlayerCards(player.userId).hiddenCard.rank"
                :hidden="true"
                :clickable="true"
                :show-reveal-text="true"
                @click="handleReveal"
              />
            </template>
            <template v-else-if="getBackCount(player.userId) > 0">
              <!-- 其他玩家的牌：5张背面 -->
              <Card
                v-for="n in getBackCount(player.userId)"
                :key="'back-' + n"
                :suit="-1"
                :rank="-1"
                :hidden="true"
              />
            </template>
          </template>
        </div>

        <!-- 下注 -->
        <div class="w-20 text-center">
          <span v-if="gameBets[player.userId]" class="text-sm font-medium text-gray-700">
            {{ gameBets[player.userId] }}
          </span>
          <span v-else class="text-sm text-gray-400">-</span>
        </div>

        <!-- 牌型 -->
        <div class="w-24 text-center">
          <span v-if="playerCardTypes[player.userId]?.cardType" class="text-sm font-medium text-orange-600">
            {{ playerCardTypes[player.userId].cardType }}
          </span>
          <span v-else class="text-sm text-gray-400">-</span>
        </div>

        <!-- 盈亏 -->
        <div class="w-24 text-right">
          <span
            v-if="scoreChanges[player.userId] !== undefined"
            :class="scoreChanges[player.userId] > 0 ? 'text-green-600 font-bold' : scoreChanges[player.userId] < 0 ? 'text-red-600 font-bold' : 'text-gray-500'"
            class="text-sm"
          >
            {{ scoreChanges[player.userId] > 0 ? '+' : '' }}{{ scoreChanges[player.userId] }}
          </span>
          <span v-else class="text-sm text-gray-400">-</span>
        </div>
      </div>
    </div>

    <!-- 底部：准备按钮和投注面板 -->
    <div class="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-4 z-40">
      <!-- 准备按钮 -->
      <div v-if="gamePhase === 'waiting'" class="flex justify-center mb-4">
        <button
          v-if="!isCurrentUserReady"
          @click="handleReady"
          class="px-8 py-3 bg-gray-500 text-white rounded-lg text-base font-semibold hover:bg-gray-600 shadow-md"
        >
          准备
        </button>
        <span v-else class="px-8 py-3 bg-gray-400 text-white rounded-lg text-base font-semibold">
          已准备
        </span>
      </div>

      <!-- 投注面板（只有非庄家玩家才显示） -->
      <BettingPanel
        v-if="gamePhase === 'betting' && !isCurrentUserDealer"
        :countdown="countdown"
        :has-bet="hasBet"
        @bet="handleBet"
      />

      <!-- 开牌按钮 -->
      <div v-if="gamePhase === 'revealing' && !playerCardTypes[currentUserId]?.cardType" class="flex justify-center">
        <button
          @click="handleReveal"
          class="px-12 py-4 bg-gradient-to-r from-yellow-400 via-yellow-500 to-yellow-600 text-white rounded-full text-xl font-bold hover:from-yellow-500 hover:via-yellow-600 hover:to-yellow-700 shadow-2xl transform hover:scale-105 transition-all duration-200 border-2 border-yellow-300"
        >
          开牌
        </button>
      </div>
    </div>

    <!-- 指定庄家模态框 -->
    <div v-if="showDealerModal" class="fixed inset-0 z-[9999] flex items-center justify-center">
      <div class="absolute inset-0 bg-black bg-opacity-50" @click="showDealerModal = false"></div>
      <div class="relative bg-white rounded-xl shadow-2xl p-6 w-full max-w-md mx-4 z-10">
        <h3 class="text-xl font-bold mb-4">指定庄家</h3>
        <div class="space-y-2 max-h-96 overflow-y-auto">
          <div
            v-for="player in players"
            :key="player.userId"
            @click="handleSetDealer(player.userId)"
            class="p-3 bg-gray-50 rounded-lg hover:bg-gray-100 cursor-pointer transition-colors"
            :class="{ 'bg-blue-100': player.isDealer === 1 }"
          >
            <div class="flex items-center justify-between">
              <span class="font-semibold">{{ player.nickname || `玩家${player.seatNumber}` }}</span>
              <span v-if="player.isDealer === 1" class="px-2 py-1 bg-yellow-500 text-white text-xs rounded">当前庄家</span>
            </div>
          </div>
        </div>
        <div class="mt-4 flex justify-end">
          <button
            @click="showDealerModal = false"
            class="px-4 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400"
          >
            取消
          </button>
        </div>
      </div>
    </div>

    <!-- 查看牌型及倍数模态框 -->
    <div v-if="showCardTypesModal" class="fixed inset-0 z-[9999] flex items-center justify-center">
      <div class="absolute inset-0 bg-black bg-opacity-50" @click="showCardTypesModal = false"></div>
      <div class="relative bg-gradient-to-br from-amber-50 to-yellow-50 rounded-xl shadow-2xl border-2 border-amber-200 p-6 w-full max-w-lg mx-4 z-10 max-h-[90vh] overflow-y-auto">
        <h3 class="text-2xl font-bold mb-4 text-center text-gray-800">牌型及倍数说明</h3>
        
        <!-- 翻倍牌型（可选牌型） -->
        <div class="mb-6">
          <h4 class="text-lg font-semibold mb-3 text-gray-700">翻倍牌型</h4>
          <div class="space-y-2">
            <div
              v-for="type in specialCardTypes"
              :key="type.name"
              class="bg-white rounded-lg p-3 shadow-sm border border-amber-200"
            >
              <div class="flex items-center justify-between mb-1">
                <span class="font-bold text-gray-800">{{ type.name }}</span>
                <span class="px-2 py-1 bg-orange-500 text-white text-xs rounded font-bold">×{{ type.multiplier }}</span>
              </div>
              <p class="text-sm text-gray-600">{{ type.description }}</p>
              <p class="text-xs text-gray-500 mt-1">{{ type.example }}</p>
            </div>
          </div>
        </div>

        <!-- 普通牌型 -->
        <div>
          <h4 class="text-lg font-semibold mb-3 text-gray-700">普通牌型</h4>
          <div class="space-y-2">
            <div
              v-for="type in normalCardTypes"
              :key="type.name"
              class="bg-white rounded-lg p-3 shadow-sm border border-gray-200"
            >
              <div class="flex items-center justify-between">
                <span class="font-bold text-gray-800">{{ type.name }}</span>
                <span class="px-2 py-1 bg-gray-500 text-white text-xs rounded font-bold">×{{ type.multiplier }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-6 flex justify-end">
          <button
            @click="showCardTypesModal = false"
            class="px-6 py-2 bg-amber-500 text-white rounded-lg hover:bg-amber-600 font-semibold"
          >
            关闭
          </button>
        </div>
      </div>
    </div>

    <!-- 结算结果弹框（带遮罩层）- 只在最后一局完成或提前结算时显示 -->
    <div v-if="showFinalSettlement" class="fixed inset-0 z-[9999] flex items-center justify-center">
      <div class="absolute inset-0 bg-black bg-opacity-70 backdrop-blur-sm"></div>
      
      <div class="relative bg-gradient-to-br from-amber-50 to-yellow-50 rounded-xl shadow-2xl border-2 border-amber-200 p-6 w-full max-w-md mx-4 z-10">
        <h3 class="text-2xl font-bold mb-4 text-center text-gray-800">最终结算结果</h3>
        <div class="space-y-2 max-h-96 overflow-y-auto mb-4">
          <div
            v-for="player in sortedPlayersByScore"
            :key="player.userId"
            class="flex justify-between items-center p-3 bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow"
          >
            <div class="flex items-center gap-2">
              <span class="font-semibold text-gray-800">{{ player.nickname || `玩家${player.seatNumber}` }}</span>
              <span v-if="player.isDealer === 1" class="px-2 py-0.5 bg-yellow-100 text-yellow-800 text-xs rounded">庄</span>
              <span v-if="player.userId === currentUserId" class="px-2 py-0.5 bg-blue-100 text-blue-800 text-xs rounded">我</span>
            </div>
            <span 
              :class="player.totalScore >= 0 ? 'text-green-600 font-bold text-lg' : 'text-red-600 font-bold text-lg'"
            >
              {{ player.totalScore >= 0 ? '+' : '' }}{{ player.totalScore }}
            </span>
          </div>
        </div>
       
        <div class="mt-4 pt-4 border-t border-amber-300">
          <button
            @click="handleGoHome"
            class="w-full px-6 py-3 bg-gradient-to-r from-purple-500 to-purple-600 text-white rounded-lg hover:from-purple-600 hover:to-purple-700 font-semibold shadow-md transition-all duration-200"
          >
            返回首页
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useGameStore } from '@/stores/game'
import { connectWebSocket, disconnectWebSocket, sendMessage, subscribe } from '@/utils/websocket'
import Card from '@/components/Card.vue'
import BettingPanel from '@/components/BettingPanel.vue'
import api from '@/utils/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const gameStore = useGameStore()

const roomCode = route.params.roomCode
const room = computed(() => gameStore.room)
const players = computed(() => gameStore.players)
const gameCards = computed(() => gameStore.gameCards)
const gameDetails = computed(() => gameStore.gameDetails)
const countdown = computed(() => gameStore.countdown)
const gamePhase = computed(() => gameStore.gamePhase)
const currentUserId = computed(() => userStore.user?.id)

const scoreChanges = ref({})
const hasBet = ref(false)
const hasStarted = ref(false)
const showDealerModal = ref(false)
const showCardTypesModal = ref(false)
const showFinalSettlement = ref(false)
const showOperationMenu = ref(false)
const nextRoundCountdown = ref(3)
let nextRoundTimer = null
const soundEnabled = ref(true)
const autoReady = ref(false)
const readyCountdown = ref(0)
const readyCount = ref(0)
const revealCountdown = ref(0)
const cardDisplayTime = ref(0)
let revealCountdownTimer = null
let cardDisplayTimer = null
const gameBets = computed(() => {
  return gameStore.gameBets || {}
})
const playerCardTypes = ref({})
const playerBackCounts = ref({})
let countdownTimer = null

// 翻倍牌型（可选牌型）
const specialCardTypes = [
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

// 普通牌型（必选牌型）
const normalCardTypes = [
  { name: '牛牛', multiplier: 3 },
  { name: '牛9', multiplier: 2 },
  { name: '牛8', multiplier: 2 }
]

const isAdmin = computed(() => {
  return room.value && room.value.adminId === currentUserId.value
})

const isCurrentUserDealer = computed(() => {
  if (!players.value || !currentUserId.value) {
    return false
  }
  const currentPlayer = players.value.find(p => p.userId === currentUserId.value)
  return currentPlayer && currentPlayer.isDealer === 1
})

const isCurrentUserReady = computed(() => {
  if (!players.value || !currentUserId.value) {
    return false
  }
  const currentPlayer = players.value.find(p => p.userId === currentUserId.value)
  return currentPlayer && currentPlayer.isReady === true
})

const sortedPlayersByScore = computed(() => {
  if (!players.value || players.value.length === 0) {
    return []
  }
  return [...players.value].sort((a, b) => {
    const scoreA = a.totalScore || 0
    const scoreB = b.totalScore || 0
    return scoreB - scoreA
  })
})

// 排序玩家列表：庄家在前，然后按座位号排序
const sortedPlayersList = computed(() => {
  if (!players.value || players.value.length === 0) {
    return []
  }
  return [...players.value].sort((a, b) => {
    // 庄家优先
    if (a.isDealer === 1 && b.isDealer !== 1) return -1
    if (a.isDealer !== 1 && b.isDealer === 1) return 1
    // 然后按座位号排序
    return (a.seatNumber || 0) - (b.seatNumber || 0)
  })
})

const getPlayerCards = (userId) => {
  const uid = typeof userId === 'string' ? parseInt(userId) : userId
  
  if (playerCardTypes.value[uid]?.cardType) {
    return null
  }
  const cards = gameCards.value[uid]
  return cards
}

const getBackCount = (userId) => {
  const uid = typeof userId === 'string' ? parseInt(userId) : userId
  
  if (uid === currentUserId.value) {
    if (gameCards.value[uid]) {
      return 0
    }
  }
  const backCount = playerBackCounts.value[uid] || 0
  return backCount
}

const handleStartGame = () => {
  if (!hasStarted.value) {
    hasStarted.value = true
    playSound('gamestart.mp3')
  }
  sendMessage('/app/game/start', { 
    roomId: room.value.id,
    userId: currentUserId.value
  })
}

const handleSetDealer = (dealerId) => {
  sendMessage('/app/game/setDealer', {
    roomId: room.value.id,
    dealerId: dealerId,
    userId: currentUserId.value
  })
  showDealerModal.value = false
}

const handleFinishGame = () => {
  if (confirm('确定要提前结算吗？')) {
    sendMessage('/app/game/finish', {
      roomId: room.value.id,
      userId: currentUserId.value
    })
  }
}

const handleBet = (betAmount) => {
  if (gameStore.currentGameRecord && !hasBet.value) {
    sendMessage('/app/game/bet', {
      gameRecordId: gameStore.currentGameRecord.id,
      userId: currentUserId.value,
      betAmount: betAmount
    })
    hasBet.value = true
  }
}

const handleReveal = () => {
  if (gameStore.currentGameRecord && !playerCardTypes.value[currentUserId.value]) {
    sendMessage('/app/game/reveal', {
      gameRecordId: gameStore.currentGameRecord.id,
      userId: currentUserId.value
    })
  }
}

const handleReady = () => {
  if (!room.value || !currentUserId.value) {
    return
  }
  sendMessage('/app/game/ready', {
    roomId: room.value.id,
    userId: currentUserId.value
  })
}

const handleGoHome = () => {
  showFinalSettlement.value = false
  disconnectWebSocket()
  router.push('/room')
}

const playSound = (filename) => {
  if (!soundEnabled.value) return
  
  try {
    const audio = new Audio(`/assets/sound/${filename}`)
    audio.volume = 0.5
    audio.play().catch(err => {
      console.warn('播放声音失败:', err)
    })
  } catch (error) {
    console.warn('播放声音失败:', error)
  }
}

const playCardTypeSound = (cardType) => {
  if (!soundEnabled.value) return
  
  const soundMap = {
    '无牛': 'wuniu.mp3',
    '牛1': 'niu1.mp3',
    '牛2': 'niu2.mp3',
    '牛3': 'niu3.mp3',
    '牛4': 'niu4.mp3',
    '牛5': 'niu5.mp3',
    '牛6': 'niu6.mp3',
    '牛7': 'niu7.mp3',
    '牛8': 'niu8.mp3',
    '牛9': 'niu9.mp3',
    '牛牛': 'niu10.mp3',
    '顺子': 'niu11shunzi.mp3',
    '五花牛': 'niu12wuhuaniu.mp3',
    '炸弹牛': 'niu13zhadan.mp3',
    '五小牛': 'niu13zhadan.mp3'
  }

  const soundFile = soundMap[cardType]
  if (!soundFile) {
    console.warn('未找到牌型对应的声音文件:', cardType)
    return
  }

  playSound(soundFile)
}

const startCountdown = (seconds) => {
  gameStore.setCountdown(seconds)
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
  countdownTimer = setInterval(() => {
    const current = gameStore.countdown
    if (current > 0) {
      gameStore.setCountdown(current - 1)
    } else {
      clearInterval(countdownTimer)
      countdownTimer = null
      if (gameStore.gamePhase === 'betting' && !hasBet.value && !isCurrentUserDealer.value) {
        console.log('投注倒计时结束，自动投注10元')
        handleBet(10)
      } else if (gameStore.gamePhase === 'revealing') {
        if (!playerCardTypes.value[currentUserId.value]) {
          handleReveal()
        }
      }
    }
  }, 1000)
}

const handleClickOutside = (event) => {
  if (showOperationMenu.value && !event.target.closest('.operation-menu-container')) {
    showOperationMenu.value = false
  }
}

onMounted(async () => {
  document.addEventListener('click', handleClickOutside)
  
  const token = userStore.token
  connectWebSocket(token, (client) => {
    // 订阅房间更新
    subscribe(`/topic/room/${room.value?.id}/update`, (data) => {
      if (data.code === 200) {
        gameStore.setRoom(data.data.room)
        gameStore.setPlayers(data.data.players)
      }
    })
    
    // 订阅准备倒计时
    subscribe(`/topic/room/${room.value?.id}/game/ready/countdown`, (data) => {
      if (data.code === 200) {
        readyCountdown.value = data.data.countdown || 0
        readyCount.value = data.data.readyCount || 0
      }
    })
    
    // 订阅开牌倒计时
    subscribe(`/topic/room/${room.value?.id}/game/reveal/countdown`, (data) => {
      if (data.code === 200) {
        revealCountdown.value = data.data.countdown || 0
      }
    })
    
    // 订阅展示牌
    subscribe(`/topic/room/${room.value?.id}/game/card/display`, (data) => {
      if (data.code === 200) {
        cardDisplayTime.value = data.data.displayTime || 8
        if (cardDisplayTimer) {
          clearInterval(cardDisplayTimer)
        }
        cardDisplayTimer = setInterval(() => {
          if (cardDisplayTime.value > 0) {
            cardDisplayTime.value--
          } else {
            clearInterval(cardDisplayTimer)
            cardDisplayTimer = null
          }
        }, 1000)
      }
    })

    // 订阅庄家变更通知
    subscribe(`/topic/room/${room.value?.id}/dealer/changed`, (data) => {
      if (data.code === 200) {
        const dealerNickname = data.data.dealerNickname || '未知玩家'
        console.log(`庄家已变更为：${dealerNickname}`)
      }
    })

    // 订阅游戏开始
    subscribe(`/topic/room/${room.value?.id}/game/start`, (data) => {
      if (data.code === 200) {
        gameStore.setCurrentGameRecord(data.data.gameRecord)
        gameStore.setGamePhase('betting')
        hasBet.value = false
        playerCardTypes.value = {}
        gameStore.setGameCards({})
        playerBackCounts.value = {}
        readyCountdown.value = 0
        revealCountdown.value = 0
        cardDisplayTime.value = 0
        playSound('kaishixiazhu.mp3')
        startCountdown(15)
      }
    })

    // 订阅投注
    subscribe(`/topic/room/${room.value?.id}/game/bet`, (data) => {
      if (data.code === 200) {
        const userId = typeof data.data.userId === 'string' ? parseInt(data.data.userId) : data.data.userId
        const betAmount = data.data.betAmount
        console.log('收到投注消息:', userId, betAmount)
        gameStore.setGameBet(userId, betAmount)
      }
    })

    // 订阅发牌（点对点）
    if (currentUserId.value) {
      const dealTopic = `/user/${currentUserId.value}/queue/game/deal`
      subscribe(dealTopic, (data) => {
        console.log('收到发牌消息（点对点）:', data)
        if (data.code === 200 && data.data) {
          const targetUserId = data.data.targetUserId
          if (targetUserId && targetUserId !== currentUserId.value) {
            return
          }
          
          const cardsData = data.data.cards || {}
          const updatedCards = {}
          const updatedBackCounts = {}
          
          for (const [playerUserId, cardInfo] of Object.entries(cardsData)) {
            let userId
            if (typeof playerUserId === 'string') {
              userId = parseInt(playerUserId)
            } else {
              userId = playerUserId
            }
            
            if (!cardInfo) continue
            
            if (cardInfo.isSelf === true) {
              if (cardInfo.cards && cardInfo.hiddenCard) {
                updatedCards[userId] = {
                  cards: cardInfo.cards,
                  hiddenCard: cardInfo.hiddenCard
                }
                updatedBackCounts[userId] = 0
              }
            } else if (cardInfo.backCount && cardInfo.backCount > 0) {
              updatedBackCounts[userId] = cardInfo.backCount
            }
          }
          
          if (Object.keys(updatedCards).length > 0 || Object.keys(updatedBackCounts).length > 0) {
            gameStore.setGameCards(updatedCards)
            playerBackCounts.value = updatedBackCounts
            gameStore.setGamePhase('revealing')
            playSound('fapai.mp3')
          }
        }
      })
      
      // 备用：订阅广播方式的发牌消息
      subscribe(`/topic/room/${room.value?.id}/game/deal/user/${currentUserId.value}`, (data) => {
        console.log('收到发牌消息（广播）:', data)
        if (data.code === 200 && data.data) {
          const targetUserId = data.data.targetUserId
          if (targetUserId && targetUserId !== currentUserId.value) {
            return
          }
          
          const cardsData = data.data.cards || {}
          const updatedCards = {}
          const updatedBackCounts = {}
          
          for (const [playerUserId, cardInfo] of Object.entries(cardsData)) {
            let userId = typeof playerUserId === 'string' ? parseInt(playerUserId) : playerUserId
            
            if (!cardInfo) continue
            
            if (cardInfo.isSelf === true) {
              if (cardInfo.cards && cardInfo.hiddenCard) {
                updatedCards[userId] = {
                  cards: cardInfo.cards,
                  hiddenCard: cardInfo.hiddenCard
                }
                updatedBackCounts[userId] = 0
              }
            } else if (cardInfo.backCount && cardInfo.backCount > 0) {
              updatedBackCounts[userId] = cardInfo.backCount
            }
          }
          
          if (Object.keys(updatedCards).length > 0 || Object.keys(updatedBackCounts).length > 0) {
            gameStore.setGameCards(updatedCards)
            playerBackCounts.value = updatedBackCounts
            gameStore.setGamePhase('revealing')
            playSound('fapai.mp3')
          }
        }
      })
    }
    
    // 订阅发牌完成广播
    subscribe(`/topic/room/${room.value?.id}/game/deal`, (data) => {
      if (data.code === 200) {
        playSound('fapai.mp3')
        gameStore.setGamePhase('revealing')
      }
    })

    // 订阅开牌
    subscribe(`/topic/room/${room.value?.id}/game/reveal`, (data) => {
      if (data.code === 200) {
        const userId = data.data.userId
        const cardType = data.data.cardType
        const multiplier = data.data.multiplier
        const cards = data.data.cards
        const cardGroups = data.data.cardGroups

        const uid = typeof userId === 'string' ? parseInt(userId) : userId

        playerCardTypes.value[uid] = {
          cardType,
          multiplier,
          cards,
          cardGroups
        }

        const updatedCards = { ...gameCards.value }
        delete updatedCards[uid]
        gameStore.setGameCards(updatedCards)
        
        delete playerBackCounts.value[uid]

        playCardTypeSound(cardType)
        
        // 检查是否所有玩家都开牌了
        const allPlayers = players.value || []
        const allRevealed = allPlayers.every(p => {
          const pUid = typeof p.userId === 'string' ? parseInt(p.userId) : p.userId
          return playerCardTypes.value[pUid]?.cardType
        })
        
        if (allRevealed && allPlayers.length > 0) {
          revealCountdown.value = 0
        }
      }
    })

    // 订阅结算
    subscribe(`/topic/room/${room.value?.id}/game/settle`, (data) => {
      if (data.code === 200) {
        gameStore.setGameDetails(data.data.details)
        
        if (data.data.gameRecord) {
          if (room.value) {
            room.value.currentRound = data.data.gameRecord.roundNumber
          }
        }
        
        if (data.data.roomFinished) {
          if (room.value) {
            room.value.status = 2
          }
          showFinalSettlement.value = true
          gameStore.setGamePhase('settling')
        } else {
          gameStore.setGamePhase('settling')
        }
        
        for (const [userId, detail] of Object.entries(data.data.details)) {
          scoreChanges.value[userId] = detail.scoreChange
          setTimeout(() => {
            delete scoreChanges.value[userId]
          }, 3000)
        }
        
        if (!data.data.roomFinished) {
          if (nextRoundTimer) {
            clearInterval(nextRoundTimer)
          }
          
          nextRoundCountdown.value = 3
          nextRoundTimer = setInterval(() => {
            nextRoundCountdown.value--
            if (nextRoundCountdown.value <= 0) {
              clearInterval(nextRoundTimer)
              nextRoundTimer = null
              nextRoundCountdown.value = 0
              
              if (room.value && room.value.currentRound < room.value.maxRounds && room.value.status !== 2) {
                hasBet.value = false
                playerCardTypes.value = {}
                gameStore.setGameCards({})
                playerBackCounts.value = {}
                handleStartGame()
              }
            }
          }, 1000)
        } else {
          if (nextRoundTimer) {
            clearInterval(nextRoundTimer)
            nextRoundTimer = null
          }
          nextRoundCountdown.value = 0
        }
      }
    })
    
    // 订阅游戏结束（提前结算）
    subscribe(`/topic/room/${room.value?.id}/game/finish`, (data) => {
      if (data.code === 200) {
        if (room.value) {
          room.value.status = 2
        }
        showFinalSettlement.value = true
        gameStore.setGamePhase('settling')
      }
    })

    // 加入房间
    sendMessage('/app/room/join', { 
      roomCode,
      userId: currentUserId.value
    })
  })

  // 获取房间信息
  try {
    const response = await api.get(`/room/code/${roomCode}`)
    if (response.data.code === 200) {
      gameStore.setRoom(response.data.data)
      const playersResponse = await api.get(`/room/${response.data.data.id}/players`)
      if (playersResponse.data.code === 200) {
        gameStore.setPlayers(playersResponse.data.data)
      }
    }
  } catch (error) {
    alert('获取房间信息失败')
    router.push('/room')
  }
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
  if (nextRoundTimer) {
    clearInterval(nextRoundTimer)
    nextRoundTimer = null
  }
  nextRoundCountdown.value = 0
  disconnectWebSocket()
})
</script>

<style scoped>
/* 牌组样式 - 重叠70% */
.card-group {
  display: flex;
  align-items: center;
}

.card-group :deep(.card) {
  margin-left: -45px; /* 重叠约70%（65px * 0.7 ≈ 45px） */
  transition: all 0.3s ease;
}

.card-group :deep(.card.first-card) {
  margin-left: 0; /* 第一张牌不重叠 */
}

.card-group :deep(.card:hover) {
  transform: translateY(-10px) scale(1.1);
  z-index: 10;
  position: relative;
}
</style>

