<template>
  <div class="min-h-screen game-page-bg relative" :style="{ backgroundImage: `url(${deskImage})`, backgroundSize: 'cover', backgroundPosition: 'center', backgroundRepeat: 'no-repeat' }">
    
    <!-- 顶部信息栏 -->
    <div class="relative z-20 pt-2 px-4">
      <div class="flex items-center justify-between mb-2">
        <!-- 左上角：准备按钮/自动准备/操作按钮 -->
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
          <label v-if="gamePhase === 'waiting'" class="flex items-center gap-1 text-white text-sm cursor-pointer">
            <input 
              type="checkbox" 
              v-model="autoReady" 
              class="w-4 h-4" 
              @change="handleAutoReadyChange"
            />
            <span>自动准备</span>
          </label>
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
        <div class="flex flex-col items-center gap-1">
          <div class="bg-white rounded-full px-6 py-2 shadow-lg">
            <p class="text-gray-800 font-bold text-base">
              {{ room?.currentRound || 0 }}/{{ room?.maxRounds || 0 }}局
            </p>
          </div>
          <!-- 底分显示 -->
          <!-- <div class="bg-amber-800 rounded-full px-4 py-1 shadow-md">
            <p class="text-white text-xs font-semibold">底分:1分</p>
          </div> -->
        </div>
        
        <!-- 右上角：复制链接按钮 -->
        <!-- <button
          @click="handleCopyLink"
          class="px-3 py-1.5 bg-blue-500 text-white rounded text-xs hover:bg-blue-600 whitespace-nowrap shadow-md"
        >
          复制链接
        </button> -->
      </div>
    </div>

    <!-- 查看牌型及倍数模态框 -->
    <div v-if="showCardTypesModal" class="fixed inset-0 z-[9999] flex items-center justify-center">
      <!-- 遮罩层 -->
      <div class="absolute inset-0 bg-black bg-opacity-50" @click="showCardTypesModal = false"></div>
      
      <!-- 模态框内容 -->
      <div class="relative bg-gradient-to-br from-amber-50 to-yellow-50 rounded-xl shadow-2xl border-2 border-amber-200 p-6 w-full max-w-lg mx-4 z-10 max-h-[90vh] overflow-y-auto">
        <h3 class="text-2xl font-bold mb-4 text-center text-gray-800">牌型及倍数说明</h3>
        
        <!-- 翻倍牌型（可选牌型） -->
        <div class="mb-6">
          <h4 class="text-lg font-bold text-gray-700 mb-3 flex items-center gap-2">
            <span class="px-2 py-1 bg-red-500 text-white text-xs rounded">翻倍</span>
            <span>翻倍牌型</span>
          </h4>
          <div class="space-y-2">
            <div
              v-for="type in specialCardTypes"
              :key="type.name"
              class="bg-white rounded-lg p-3 shadow-sm border-l-4 border-red-500"
            >
              <div class="flex justify-between items-center mb-1">
                <span class="font-bold text-gray-800">{{ type.name }}</span>
                <span class="px-3 py-1 bg-red-500 text-white text-sm font-bold rounded-full">
                  {{ type.multiplier }}倍
                </span>
              </div>
              <p class="text-sm text-gray-600">{{ type.description }}</p>
              <p class="text-xs text-gray-500 mt-1">{{ type.example }}</p>
            </div>
          </div>
        </div>
        
        <!-- 普通牌型（必选牌型） -->
        <div>
          <h4 class="text-lg font-bold text-gray-700 mb-3">普通牌型</h4>
          <div class="grid grid-cols-2 gap-2">
            <div
              v-for="type in normalCardTypes"
              :key="type.name"
              class="bg-white rounded-lg p-3 shadow-sm border-l-4 border-blue-500"
            >
              <div class="flex justify-between items-center">
                <span class="font-semibold text-gray-800">{{ type.name }}</span>
                <span class="px-2 py-1 bg-blue-500 text-white text-xs font-bold rounded">
                  {{ type.multiplier }}倍
                </span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 关闭按钮 -->
        <div class="mt-6 flex justify-end">
          <button
            @click="showCardTypesModal = false"
            class="px-6 py-2 bg-gradient-to-r from-gray-500 to-gray-600 text-white rounded-lg hover:from-gray-600 hover:to-gray-700 font-semibold shadow-md transition-all duration-200"
          >
            关闭
          </button>
        </div>
      </div>
    </div>

    <!-- 指定庄家模态框 -->
    <div v-if="showDealerModal" class="fixed inset-0 z-[9999] flex items-center justify-center">
      <!-- 遮罩层 -->
      <div class="absolute inset-0 bg-black bg-opacity-50" @click="showDealerModal = false"></div>
      
      <!-- 模态框内容 -->
      <div class="relative bg-white rounded-xl shadow-2xl p-6 w-full max-w-md mx-4 z-10">
        <h3 class="text-xl font-bold mb-4 text-gray-800">指定庄家</h3>
        <div class="space-y-2 max-h-96 overflow-y-auto">
          <div
            v-for="player in players"
            :key="player.userId"
            @click="handleSetDealer(player.userId)"
            :class="[
              'flex items-center justify-between p-3 rounded-lg cursor-pointer transition-all',
              player.isDealer === 1 
                ? 'bg-yellow-100 border-2 border-yellow-400' 
                : 'bg-gray-50 hover:bg-gray-100 border-2 border-transparent'
            ]"
          >
            <div class="flex items-center gap-3">
              <span class="font-semibold text-gray-800">
                {{ player.nickname || `玩家${player.seatNumber}` }}
              </span>
              <span v-if="player.isDealer === 1" class="px-2 py-1 bg-yellow-500 text-white text-xs rounded font-bold">
                当前庄家
              </span>
              <span v-if="player.userId === currentUserId" class="px-2 py-1 bg-blue-500 text-white text-xs rounded">
                我
              </span>
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

    <!-- 游戏桌容器（铺满整个页面） -->
    <div class="fixed inset-0 w-full h-full z-0">
      <!-- 美女装饰图 -->
      <div class="absolute inset-0 beauty-decoration pointer-events-none z-0">
        <img :src="beautyImage" alt="Beauty" class="beauty-image" />
      </div>
      
      <!-- 玩家座位容器 -->
      <div class="relative w-full h-full">
        <!-- 玩家座位 -->
        <div
          v-for="player in players"
          :key="player.userId"
          class="absolute z-10"
          :style="getPlayerPosition(player.seatNumber, player.userId)"
        >
          <PlayerSeat
            :player="player"
            :is-current-user="player.userId === currentUserId"
            :cards="getPlayerCards(player.userId)"
            :score-change="scoreChanges[player.userId]"
            :is-dealer="player.isDealer === 1"
            :bet-amount="gameBets[player.userId]"
            :seat-number="player.seatNumber"
            :is-ready="player.isReady === true"
            :card-type="playerCardTypes[player.userId]?.cardType"
            :multiplier="playerCardTypes[player.userId]?.multiplier"
            :is-online="player.isOnline !== false"
            :card-groups="playerCardTypes[player.userId]?.cardGroups"
            :back-count="getBackCount(player.userId)"
            @reveal="handleReveal"
          />
        </div>

        <!-- 中心区域（倒计时、结算倒计时） -->
        <div class="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 z-10">
        <!-- 准备倒计时 -->
        <div v-if="readyCountdown > 0" class="text-white text-center">
          <div class="bg-black bg-opacity-50 rounded-full px-8 py-4 backdrop-blur-sm">
            <p class="text-2xl font-bold text-yellow-300">{{ readyCountdown }}秒后开始</p>
            <p class="text-sm text-gray-300 mt-1">已准备: {{ readyCount }}人</p>
          </div>
        </div>
        <!-- 发牌中 -->
        <div v-else-if="gamePhase === 'dealing'" class="text-white text-center">
          <div class="bg-black bg-opacity-30 rounded-full px-6 py-3 backdrop-blur-sm">
            <p class="text-xl font-bold text-yellow-300">发牌中...</p>
          </div>
        </div>
        <!-- 开牌倒计时 -->
        <div v-else-if="gamePhase === 'revealing' && revealCountdown > 0" class="text-white text-center">
          <div class="bg-black bg-opacity-50 rounded-full px-8 py-4 backdrop-blur-sm">
            <p class="text-2xl font-bold text-yellow-300">开牌倒计时: {{ revealCountdown }}秒</p>
          </div>
        </div>
        <!-- 展示牌（8秒） -->
        <div v-else-if="cardDisplayTime > 0" class="text-white text-center">
          <div class="bg-black bg-opacity-50 rounded-full px-8 py-4 backdrop-blur-sm">
            <p class="text-2xl font-bold text-yellow-300">{{ cardDisplayTime +nextRoundCountdown}}秒后开始下一局</p>
          </div>
        </div>
        <!-- 结算后倒计时（非最后一局） -->
        <!-- <div v-else-if="gamePhase === 'settling' && !showFinalSettlement && room && room.currentRound < room.maxRounds" class="text-white text-center">
          <div class="bg-black bg-opacity-50 rounded-full px-8 py-4 backdrop-blur-sm">
            <p class="text-2xl font-bold text-yellow-300">{{ nextRoundCountdown }}秒后开始下一局</p>
          </div>
        </div> -->
        </div>
      </div>
    </div>

    <!-- 投注面板（只有非庄家玩家才显示） -->
    <BettingPanel
      v-if="gamePhase === 'betting' && !isCurrentUserDealer"
      :countdown="countdown"
      :has-bet="hasBet"
      @bet="handleBet"
    />

    <!-- 开牌按钮 -->
    <!-- <div v-if="gamePhase === 'revealing'" class="fixed bottom-4 left-1/2 transform -translate-x-1/2 z-50">
      <button
        @click="handleReveal"
        class="px-12 py-4 bg-gradient-to-r from-yellow-400 via-yellow-500 to-yellow-600 text-white rounded-full text-xl font-bold hover:from-yellow-500 hover:via-yellow-600 hover:to-yellow-700 shadow-2xl transform hover:scale-105 transition-all duration-200 border-2 border-yellow-300"
      >
        开牌
      </button>
    </div> -->

    <!-- 结算结果弹框（带遮罩层）- 只在最后一局完成或提前结算时显示 -->
    <div v-if="showFinalSettlement" class="fixed inset-0 z-[9999] flex items-center justify-center">
      <!-- 遮罩层 -->
      <div class="absolute inset-0 bg-black bg-opacity-70 backdrop-blur-sm"></div>
      
      <!-- 结算结果弹框 -->
      <div class="relative bg-gradient-to-br from-amber-50 to-yellow-50 rounded-xl shadow-2xl border-2 border-amber-200 p-6 w-full max-w-md mx-4 z-10">
        <h3 class="text-2xl font-bold mb-4 text-center text-gray-800">最终结算结果</h3>
        <!-- 玩家盈亏列表（按 totalScore 倒序排列） -->
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
       
        <!-- 返回首页按钮 -->
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
import { connectWebSocket, subscribe, sendMessage, disconnectWebSocket } from '@/utils/websocket'
import PlayerSeat from '@/components/PlayerSeat.vue'
import BettingPanel from '@/components/BettingPanel.vue'
import api from '@/utils/api'
import deskImage from '@/assets/desk.png'
import beautyImage from '@/assets/niuniu.png'

// 牌桌尺寸
const deskAspectRatio = ref(1) // 默认1:1，加载图片后更新
const deskLoaded = ref(false)

// 加载图片并获取尺寸比例
const onDeskImageLoad = (event) => {
  const img = event.target
  if (img.naturalWidth && img.naturalHeight) {
    deskAspectRatio.value = img.naturalWidth / img.naturalHeight
    deskLoaded.value = true
  }
}

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
const hasStarted = ref(false) // 是否已经开始过游戏
const showDealerModal = ref(false) // 显示指定庄家模态框
const showCardTypesModal = ref(false) // 显示牌型及倍数模态框
const showFinalSettlement = ref(false) // 是否显示最终结算弹框
const showOperationMenu = ref(false) // 显示操作菜单
const nextRoundCountdown = ref(3) // 下一局倒计时（秒）
let nextRoundTimer = null // 下一局倒计时定时器
const soundEnabled = ref(true) // 声音开关
const autoReady = ref(false) // 自动准备
const readyCountdown = ref(0) // 准备倒计时
const readyCount = ref(0) // 已准备人数
const revealCountdown = ref(0) // 开牌倒计时
const cardDisplayTime = ref(0) // 展示牌时间
let revealCountdownTimer = null // 开牌倒计时定时器
let cardDisplayTimer = null // 展示牌定时器
const gameBets = computed(() => {
  return gameStore.gameBets || {}
})
    // 存储每个玩家的牌型信息 { userId: { cardType, multiplier, cards, cardGroups } }
    const playerCardTypes = ref({})
    // 存储每个玩家的背面牌数量 { userId: backCount }
    const playerBackCounts = ref({})
    let countdownTimer = null

// 翻倍牌型（可选牌型）
const specialCardTypes = [
  { 
    name: '五小牛', 
    multiplier: 6,
    description: '5张牌都小于5，且总和≤10',
    example: '例：AA223'
  },
  { 
    name: '炸弹牛', 
    multiplier: 5,
    description: '4张相同点数的牌',
    example: '例：AAAAK'
  },
  { 
    name: '五花牛', 
    multiplier: 5,
    description: '5张都是J、Q、K',
    example: '例：JJQQK'
  },
  { 
    name: '顺子', 
    multiplier: 5,
    description: '5张牌点数连续',
    example: '例：A2345'
  }
]

// 普通牌型（必选牌型）
const normalCardTypes = [
  { name: '牛牛', multiplier: 4 },
  { name: '牛9', multiplier: 3 },
  { name: '牛8', multiplier: 2 }
]

const isAdmin = computed(() => {
  return room.value && room.value.adminId === currentUserId.value
})

// 判断当前用户是否是庄家
const isCurrentUserDealer = computed(() => {
  if (!players.value || !currentUserId.value) {
    return false
  }
  const currentPlayer = players.value.find(p => p.userId === currentUserId.value)
  return currentPlayer && currentPlayer.isDealer === 1
})

// 判断当前用户是否已准备
const isCurrentUserReady = computed(() => {
  if (!players.value || !currentUserId.value) {
    return false
  }
  const currentPlayer = players.value.find(p => p.userId === currentUserId.value)
  return currentPlayer && currentPlayer.isReady === true
})

// 计算当前房间所有用户的总盈亏
const totalRoomProfit = computed(() => {
  if (!players.value || players.value.length === 0) {
    return 0
  }
  return players.value.reduce((sum, player) => sum + (player.totalScore || 0), 0)
})

// 按 totalScore 倒序排列的玩家列表（用于最终结算显示）
const sortedPlayersByScore = computed(() => {
  if (!players.value || players.value.length === 0) {
    return []
  }
  return [...players.value].sort((a, b) => {
    const scoreA = a.totalScore || 0
    const scoreB = b.totalScore || 0
    return scoreB - scoreA // 倒序：从高到低
  })
})

const getPlayerPosition = (seatNumber, userId) => {
  // 当前用户始终在底部中间
  if (userId === currentUserId.value) {
    return {
      position: 'fixed',
      bottom: '20px',
      left: '50%',
      transform: 'translateX(-50%)',
      zIndex: 20
    }
  }
  
  // 其他玩家分布在左右和上方
  const otherPlayers = players.value.filter(p => p.userId !== currentUserId.value)
  const totalSeats = otherPlayers.length
  
  // 找到当前玩家在otherPlayers中的索引
  const playerIndex = otherPlayers.findIndex(p => {
    const pUserId = typeof p.userId === 'string' ? parseInt(p.userId) : p.userId
    const uId = typeof userId === 'string' ? parseInt(userId) : userId
    return pUserId === uId || p.seatNumber === seatNumber
  })
  
  if (playerIndex === -1) {
    // 如果找不到，使用seatNumber计算
    return getPositionByIndex(seatNumber - 1, totalSeats)
  }
  
  return getPositionByIndex(playerIndex, totalSeats)
}

// 根据索引计算位置（分布在左右和上方）
const getPositionByIndex = (index, total) => {
  // 布局规则：
  // - 顶部中间：index 0
  // - 中间左侧：index 1
  // - 中间右侧：index 2
  // - 下方左侧：index 3
  // - 下方右侧：index 4
  // 如果超过5个玩家，继续按这个模式排列
  
  const positions = []
  
  // 顶部中间
  positions.push({ top: '10%', left: '50%', transform: 'translateX(-50%)' })
  
  // 中间左侧
  positions.push({ top: '35%', left: '10%', transform: 'translateX(-50%)' })
  
  // 中间右侧
  positions.push({ top: '35%', right: '10%', transform: 'translateX(50%)' })
  
  // 下方左侧
  positions.push({ top: '60%', left: '15%', transform: 'translateX(-50%)' })
  
  // 下方右侧
  positions.push({ top: '60%', right: '15%', transform: 'translateX(50%)' })
  
  // 如果超过5个，继续循环
  const positionIndex = index % positions.length
  return {
    position: 'fixed',
    ...positions[positionIndex],
    zIndex: 10
  }
}

const getPlayerNickname = (userId) => {
  // userId可能是字符串或数字，需要转换比较
  const userIdNum = typeof userId === 'string' ? parseInt(userId) : userId
  const player = players.value.find(p => {
    const pUserId = typeof p.userId === 'string' ? parseInt(p.userId) : p.userId
    return pUserId === userIdNum
  })
  return player?.nickname || `玩家${userId}`
}

    // 获取玩家卡片数据
    const getPlayerCards = (userId) => {
      // 确保 userId 类型一致（统一转为数字）
      const uid = typeof userId === 'string' ? parseInt(userId) : userId
      
      // 如果已开牌，返回null（因为会使用cardGroups显示）
      if (playerCardTypes.value[uid]?.cardType) {
        return null
      }
      // 未开牌时，返回发牌时的格式
      const cards = gameCards.value[uid]
      if (cards) {
        console.log('getPlayerCards 返回:', uid, cards)
      }
      return cards
    }
    
    // 获取玩家背面牌数量
    const getBackCount = (userId) => {
      // 确保 userId 类型一致（统一转为数字）
      const uid = typeof userId === 'string' ? parseInt(userId) : userId
      
      if (uid === currentUserId.value) {
        // 自己的牌，如果有cards则返回0（因为会显示4+1）
        if (gameCards.value[uid]) {
          return 0
        }
      }
      // 其他玩家的牌，返回背面牌数量
      const backCount = playerBackCounts.value[uid] || 0
      if (backCount > 0) {
        console.log('getBackCount 返回:', uid, backCount)
      }
      return backCount
    }

// 启动发牌动画
const handleStartGame = () => {
  // 只在第一局点击开始时播放游戏开始音效
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
    hasBet.value = true
    sendMessage('/app/game/bet', {
      gameRecordId: gameStore.currentGameRecord.id,
      betAmount,
      userId: currentUserId.value
    })
  }
}

const handleReveal = () => {
  if (gameStore.currentGameRecord) {
    sendMessage('/app/game/reveal', {
      gameRecordId: gameStore.currentGameRecord.id,
      userId: currentUserId.value
    })
  }
}

const handleNextRound = () => {
  // 自动开始下一局
  if (room.value && room.value.currentRound < room.value.maxRounds) {
    handleStartGame()
  }
}

// 播放声音的通用函数
const playSound = (soundFile) => {
  if (!soundFile) return

  try {
    // 在Vite中，使用 new URL 和 import.meta.url 来正确加载静态资源
    const audio = new Audio(new URL(`../assets/sound/${soundFile}`, import.meta.url).href)
    audio.volume = 0.7 // 设置音量
    audio.play().catch(error => {
      console.warn('播放声音失败:', error)
    })
  } catch (error) {
    console.warn('创建音频对象失败:', error)
  }
}

// 播放牌型对应的声音
const playCardTypeSound = (cardType) => {
  if (!cardType) return

  // 牌型到声音文件的映射（根据实际assets/sound目录中的文件名）
  const soundMap = {
    '无牛': 'meiniu.mp3',
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
    '五小牛': 'niu13zhadan.mp3' // 如果没有专门的五小牛声音，使用炸弹牛
  }

  const soundFile = soundMap[cardType]
  if (!soundFile) {
    console.warn('未找到牌型对应的声音文件:', cardType)
    return
  }

  playSound(soundFile)
}

// 复制链接
const handleCopyLink = () => {
  const url = window.location.href
  navigator.clipboard.writeText(url).then(() => {
    alert('链接已复制到剪贴板')
  }).catch(() => {
    // 降级方案
    const textArea = document.createElement('textarea')
    textArea.value = url
    document.body.appendChild(textArea)
    textArea.select()
    document.execCommand('copy')
    document.body.removeChild(textArea)
    alert('链接已复制到剪贴板')
  })
}

// 切换声音
const toggleSound = () => {
  soundEnabled.value = !soundEnabled.value
  // 这里可以添加实际的静音/取消静音逻辑
  console.log('声音开关:', soundEnabled.value)
}

// 准备
const handleReady = () => {
  if (!room.value || !currentUserId.value) {
    return
  }
  sendMessage('/app/game/ready', {
    roomId: room.value.id,
    userId: currentUserId.value
  })
}

// 自动准备变化时立即准备
const handleAutoReadyChange = () => {
  if (autoReady.value && gamePhase.value === 'waiting' && !isCurrentUserReady.value) {
    // 立即提交准备操作
    handleReady()
  }
}

const handleGoHome = () => {
  // 关闭结算弹框
  showFinalSettlement.value = false
  // 断开WebSocket连接
  disconnectWebSocket()
  // 跳转到房间首页
  router.push('/room')
}

const handleLeaveRoom = () => {
  sendMessage('/app/room/leave', {
    roomCode,
    userId: currentUserId.value
  })
  disconnectWebSocket()
  router.push('/room')
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
      // 倒计时结束后的自动操作
      if (gameStore.gamePhase === 'betting' && !hasBet.value && !isCurrentUserDealer.value) {
        // 如果投注倒计时结束且未投注（且不是庄家），默认投注10元（最低投注额）
        // 投注后，后端会自动检查是否所有非庄家玩家都投注了，如果是则立即发牌
        console.log('投注倒计时结束，自动投注10元')
        handleBet(10)
      } else if (gameStore.gamePhase === 'revealing') {
        // 如果开牌倒计时结束，自动开牌
        if (!playerCardTypes.value[currentUserId.value]) {
          handleReveal()
        }
      }
    }
  }, 1000)
}

// 点击外部关闭操作菜单
const handleClickOutside = (event) => {
  if (showOperationMenu.value && !event.target.closest('.operation-menu-container')) {
    showOperationMenu.value = false
  }
}

onMounted(async () => {
  // 预加载牌桌图片以获取尺寸
  const img = new Image()
  img.onload = onDeskImageLoad
  img.src = deskImage
  
  // 添加点击外部关闭菜单的事件监听
  document.addEventListener('click', handleClickOutside)
  
  // 连接WebSocket
  const token = userStore.token
  connectWebSocket(token, (client) => {
    // 订阅房间更新
    subscribe(`/topic/room/${room.value?.id}/update`, (data) => {
      if (data.code === 200) {
        gameStore.setRoom(data.data.room)
        gameStore.setPlayers(data.data.players)
        
        // 如果自动准备开启且未准备，自动准备（延迟执行，避免在房间更新时立即触发）
        if (autoReady.value && gamePhase.value === 'waiting' && !isCurrentUserReady.value) {
          // 延迟执行，避免在房间更新广播时所有玩家同时自动准备
          setTimeout(() => {
            if (autoReady.value && gamePhase.value === 'waiting' && !isCurrentUserReady.value) {
              handleReady()
            }
          }, 300)
        }
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
        // 启动展示牌倒计时
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
        // 房间更新消息已经包含了最新的玩家列表（包括庄家信息）
        // 这里主要是显示提示信息，让用户知道庄家已变更
        const dealerNickname = data.data.dealerNickname || '未知玩家'
        console.log(`庄家已变更为：${dealerNickname}`)
      }
    })

    // 订阅游戏开始
    subscribe(`/topic/room/${room.value?.id}/game/start`, (data) => {
      if (data.code === 200) {
        gameStore.setCurrentGameRecord(data.data.gameRecord)
        gameStore.setGamePhase('betting')
        hasBet.value = false // 重置投注状态
        playerCardTypes.value = {} // 清空牌型信息
        gameStore.setGameCards({}) // 清空牌面
        playerBackCounts.value = {} // 清空背面牌数量
        readyCountdown.value = 0 // 清除准备倒计时
        revealCountdown.value = 0 // 清除开牌倒计时
        cardDisplayTime.value = 0 // 清除展示牌时间
        // 展示下注选项时播放开始下注音效
        playSound('kaishixiazhu.mp3')
        startCountdown(15) // 15秒投注倒计时
        
        // 如果自动准备开启，自动准备下一局
        if (autoReady.value) {
          // 等待游戏状态更新后再准备
          setTimeout(() => {
            if (gamePhase.value === 'waiting') {
              handleReady()
            }
          }, 1000)
        }
      }
    })

    // 订阅投注（所有玩家都能看到其他玩家的投注额）
    subscribe(`/topic/room/${room.value?.id}/game/bet`, (data) => {
      if (data.code === 200) {
        // 确保 userId 是数字类型
        const userId = typeof data.data.userId === 'string' ? parseInt(data.data.userId) : data.data.userId
        const betAmount = data.data.betAmount
        console.log('收到投注消息:', userId, betAmount)
        gameStore.setGameBet(userId, betAmount)
      }
    })

    // 订阅发牌（点对点，接收所有玩家的牌信息）
    console.log('准备订阅发牌消息，currentUserId:', currentUserId.value)
    if (currentUserId.value) {
      const dealTopic = `/user/${currentUserId.value}/queue/game/deal`
      console.log('订阅发牌主题:', dealTopic)
      subscribe(dealTopic, (data) => {
        console.log('收到发牌消息（点对点）:', data)
        if (data.code === 200 && data.data) {
          // 检查是否是发送给自己的消息
          const targetUserId = data.data.targetUserId
          if (targetUserId && targetUserId !== currentUserId.value) {
            console.log('消息不是发送给当前用户的，跳过:', targetUserId, '当前用户:', currentUserId.value)
            return
          }
          
          const cardsData = data.data.cards || {}
          const updatedCards = {}
          const updatedBackCounts = {}
          
          console.log('发牌数据:', JSON.stringify(cardsData), '当前用户ID:', currentUserId.value)
          console.log('cardsData类型:', typeof cardsData, 'keys:', Object.keys(cardsData))
          
          // 遍历所有玩家的牌
          for (const [playerUserId, cardInfo] of Object.entries(cardsData)) {
            // 确保userId是数字类型
            let userId
            if (typeof playerUserId === 'string') {
              userId = parseInt(playerUserId)
            } else {
              userId = playerUserId
            }
            
            console.log('处理玩家牌:', userId, 'cardInfo:', cardInfo)
            
            if (!cardInfo) {
              console.warn('cardInfo为空，跳过玩家:', userId)
              continue
            }
            
            if (cardInfo.isSelf === true) {
              // 自己的牌：4张正面 + 1张背面
              console.log('检测到自己的牌:', userId, 'cards:', cardInfo.cards, 'hiddenCard:', cardInfo.hiddenCard)
              if (cardInfo.cards && cardInfo.hiddenCard) {
                updatedCards[userId] = {
                  cards: cardInfo.cards,
                  hiddenCard: cardInfo.hiddenCard
                }
                updatedBackCounts[userId] = 0
                console.log('✓ 设置自己的牌:', userId, cardInfo.cards.length, '张正面 + 1张背面')
              } else {
                console.warn('自己的牌数据不完整:', cardInfo)
              }
            } else if (cardInfo.backCount && cardInfo.backCount > 0) {
              // 其他玩家的牌：5张背面
              updatedBackCounts[userId] = cardInfo.backCount
              console.log('✓ 设置其他玩家背面牌:', userId, cardInfo.backCount, '张')
            } else {
              console.warn('未识别的牌信息格式:', cardInfo)
            }
          }
          
          console.log('更新后的牌数据:', updatedCards)
          console.log('更新后的背面牌数据:', updatedBackCounts)
          
          if (Object.keys(updatedCards).length > 0 || Object.keys(updatedBackCounts).length > 0) {
            gameStore.setGameCards(updatedCards)
            playerBackCounts.value = updatedBackCounts
            gameStore.setGamePhase('revealing')
            startCountdown(10) // 10秒开牌倒计时
            playSound('fapai.mp3') // 播放发牌音效
            console.log('✓ 发牌完成，更新后的牌:', updatedCards, '背面牌:', updatedBackCounts)
            console.log('gameStore.gameCards:', gameStore.gameCards)
          } else {
            console.warn('⚠ 没有收到任何牌的信息，cardsData:', cardsData)
          }
        } else {
          console.warn('发牌消息格式错误:', data)
        }
      })
      
      // 备用：订阅广播方式的发牌消息（如果点对点失败）
      subscribe(`/topic/room/${room.value?.id}/game/deal/user/${currentUserId.value}`, (data) => {
        console.log('收到发牌消息（广播）:', data)
        if (data.code === 200 && data.data) {
          // 检查是否是发送给自己的消息
          const targetUserId = data.data.targetUserId
          if (targetUserId && targetUserId !== currentUserId.value) {
            console.log('消息不是发送给当前用户的，跳过:', targetUserId, '当前用户:', currentUserId.value)
            return
          }
          
          const cardsData = data.data.cards || {}
          const updatedCards = {}
          const updatedBackCounts = {}
          
          // 遍历所有玩家的牌
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
    } else {
      console.warn('当前用户ID为空，无法订阅发牌消息')
    }
    
    // 订阅发牌完成广播（通知所有玩家发牌阶段开始）
    subscribe(`/topic/room/${room.value?.id}/game/deal`, (data) => {
      if (data.code === 200) {
        console.log('收到发牌广播消息',data)
        // 播放发牌声音
        playSound('fapai.mp3')
        gameStore.setGamePhase('revealing')
        // 开牌倒计时由后端控制
      }
    })

        // 订阅开牌
        subscribe(`/topic/room/${room.value?.id}/game/reveal`, (data) => {
          if (data.code === 200) {
            const userId = data.data.userId
            const cardType = data.data.cardType
            const multiplier = data.data.multiplier
            const cards = data.data.cards // 完整的5张牌数组
            const cardGroups = data.data.cardGroups // 牌型分组

            // 确保 userId 是数字类型
            const uid = typeof userId === 'string' ? parseInt(userId) : userId

            // 更新该玩家的牌型信息
            playerCardTypes.value[uid] = {
              cardType,
              multiplier,
              cards, // 完整的5张牌
              cardGroups // 牌型分组
            }

            // 清除该玩家的发牌数据（已开牌，不再显示4+1或背面牌）
            // 使用 gameStore.setGameCards 来更新，确保响应式更新
            const updatedCards = { ...gameCards.value }
            delete updatedCards[uid]
            gameStore.setGameCards(updatedCards)
            
            // 清除背面牌数量
            delete playerBackCounts.value[uid]

            console.log('开牌后更新，userId:', uid, 'cardType:', cardType, 'cardGroups:', cardGroups)
            console.log('开牌后 gameCards:', gameCards.value, 'playerCardTypes:', playerCardTypes.value)

            // 播放牌型对应的声音
            playCardTypeSound(cardType)
            
            // 检查是否所有玩家都开牌了，如果是则清除开牌倒计时
            const allPlayers = players.value || []
            const allRevealed = allPlayers.every(p => {
              const pUid = typeof p.userId === 'string' ? parseInt(p.userId) : p.userId
              return playerCardTypes.value[pUid]?.cardType
            })
            
            if (allRevealed && allPlayers.length > 0) {
              // 所有玩家都开牌了，清除开牌倒计时
              revealCountdown.value = 0
            }
          }
        })

    // 订阅结算
    subscribe(`/topic/room/${room.value?.id}/game/settle`, (data) => {
      if (data.code === 200) {
        gameStore.setGameDetails(data.data.details)
        
        // 更新房间信息（包括状态）
        if (data.data.gameRecord) {
          // 更新当前局数
          if (room.value) {
            room.value.currentRound = data.data.gameRecord.roundNumber
          }
        }
        
        // 如果所有对局已完成，显示最终结算弹框
        if (data.data.roomFinished) {
          if (room.value) {
            room.value.status = 2 // 已结束
          }
          // 显示最终结算弹框
          showFinalSettlement.value = true
          gameStore.setGamePhase('settling')
        } else {
          // 不是最后一局，不显示结算弹框，只显示动画和自动开始下一局
          gameStore.setGamePhase('settling')
        }
        
        // 显示积分变化动画
        for (const [userId, detail] of Object.entries(data.data.details)) {
          scoreChanges.value[userId] = detail.scoreChange
          setTimeout(() => {
            delete scoreChanges.value[userId]
          }, 3000)
        }
        
        // 如果不是最后一局，显示倒计时并自动开始下一局
        if (!data.data.roomFinished) {
          // 清除之前的倒计时
          if (nextRoundTimer) {
            clearInterval(nextRoundTimer)
          }
          
          // 开始倒计时（3秒）
          nextRoundCountdown.value = 3
          nextRoundTimer = setInterval(() => {
            nextRoundCountdown.value--
            if (nextRoundCountdown.value <= 0) {
              clearInterval(nextRoundTimer)
              nextRoundTimer = null
              nextRoundCountdown.value = 0
              
              // 开始下一局（不需要手动开始，等待玩家准备即可）
              // 重置状态，等待玩家准备
              if (room.value && room.value.currentRound < room.value.maxRounds && room.value.status !== 2) {
                hasBet.value = false // 重置投注状态
                playerCardTypes.value = {} // 清空牌型信息
                gameStore.setGameCards({}) // 清空牌面
                playerBackCounts.value = {} // 清空背面牌数量
                gameStore.setGamePhase('waiting') // 设置为等待状态，等待玩家准备
              }
            }
          }, 1000)
        } else {
          // 最后一局，清除倒计时
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
        // 更新房间状态为已结束
        if (room.value) {
          room.value.status = 2 // 已结束
        }
        // 显示最终结算弹框
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
  // 移除事件监听
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
/* 游戏页面背景 */
.game-page-bg {
  background: #072c44;
  background-attachment: fixed;
}

/* 游戏桌样式 */
.game-table {
  position: relative;
  /* overflow: hidden; */
}

.game-table-bg {
  width: 100%;
  height: 100%;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  opacity: 1;
}

/* 美女装饰图 */
.beauty-decoration {
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
  opacity: 0.3;
}

.beauty-image {
  max-width: 40%;
  max-height: 40%;
  object-fit: contain;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.3));
  /* animation: float 3s ease-in-out infinite; */
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-10px);
  }
}

/* 金币动画 */
</style>

