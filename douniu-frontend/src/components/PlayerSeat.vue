<template>
  <div class="player-seat">
    <!-- 投注金额显示（在玩家卡片上方，桌子内） -->
    <div v-if="betAmount" class="bet-amount-on-table" :class="betPositionClass">
      <div class="bet-amount-badge">
        {{ betAmount }}元
      </div>
    </div>

    <!-- 玩家信息卡片 -->
    <div 
      class="player-card" 
      :class="{ 
        'dealer-card': isDealer, 
        'current-user-card': isCurrentUser,
        'offline-card': !isOnline
      }"
    >
      <!-- 庄家标识 -->
      <div v-if="isDealer" class="dealer-badge">
        <span class="dealer-text">庄</span>
      </div>
      
      <!-- 在线/离线状态标识 -->
      <!-- <div class="online-status-badge" :class="{ 'online': isOnline, 'offline': !isOnline }">
        <span class="status-dot"></span>
        <span class="status-text">{{ isOnline ? '在线' : '离线' }}</span>
      </div> -->
      
      <!-- 玩家昵称 -->
      <div class="player-name">
        {{ player.nickname || `玩家${player.seatNumber}` }}
        <div class="score-value" :class="player.totalScore >= 0 ? 'positive' : 'negative'">
           <!-- 房间总积分（直接显示金额） -->
          ¥{{ player.totalScore }}
        </div>
      </div>
      
      
      <!-- 牌型显示（开牌后显示） -->
      <div v-if="cardType" class="card-type-display">
        <div class="card-type-name">{{ cardType }} <span v-if="multiplier" class="card-type-multiplier">×{{ multiplier }}</span></div> 
       
      </div>

      <!-- 积分变化动画 -->
      <transition name="score-change">
        <div
          v-if="scoreChange !== undefined && scoreChange !== 0"
          class="score-change-animation"
          :class="scoreChange > 0 ? 'positive' : 'negative'"
        >
          {{ scoreChange > 0 ? '+' : '' }}{{ scoreChange }}
        </div>
      </transition>
    </div>

    <!-- 牌面 -->
    <div v-if="cardType || cards || backCount" class="cards-container">
      <!-- 如果已开牌，按牌型分组显示 -->
      <template v-if="cardType && cardGroups && (cardGroups.group1 || cardGroups.group2)">
        <div v-if="cardGroups.group1 && Array.isArray(cardGroups.group1) && cardGroups.group1.length > 0" class="card-group">
          <Card
            v-for="(card, index) in cardGroups.group1"
            :key="'g1-' + index"
            :suit="card.suit"
            :rank="card.rank"
            :hidden="false"
          />
        </div>
        <div v-if="cardGroups.group2 && Array.isArray(cardGroups.group2) && cardGroups.group2.length > 0" class="card-group-separator">|</div>
        <div v-if="cardGroups.group2 && Array.isArray(cardGroups.group2) && cardGroups.group2.length > 0" class="card-group">
          <Card
            v-for="(card, index) in cardGroups.group2"
            :key="'g2-' + index"
            :suit="card.suit"
            :rank="card.rank"
            :hidden="false"
          />
        </div>
      </template>
      <!-- 如果未开牌，显示自己的4张+1张背面，或其他玩家的5张背面 -->
      <template v-else-if="isCurrentUser && cards && cards.cards && Array.isArray(cards.cards) && !cardType">
        <!-- 自己的牌：4张正面 + 1张背面（可点击） -->
        <Card
          v-for="(card, index) in cards.cards"
          :key="'card-' + index"
          :suit="card.suit"
          :rank="card.rank"
          :hidden="false"
        />
        <Card
          v-if="cards.hiddenCard"
          :suit="cards.hiddenCard.suit"
          :rank="cards.hiddenCard.rank"
          :hidden="true"
          :clickable="true"
          :show-reveal-text="true"
          @click="handleRevealCard"
        />
      </template>
      <template v-else-if="!isCurrentUser && backCount && backCount > 0 && !cardType">
        <!-- 其他玩家的牌：5张背面 -->
        <Card
          v-for="n in backCount"
          :key="'back-' + n"
          :suit="-1"
          :rank="-1"
          :hidden="true"
        />
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import Card from './Card.vue'

const props = defineProps({
  player: {
    type: Object,
    required: true
  },
  isCurrentUser: {
    type: Boolean,
    default: false
  },
  cards: {
    type: Object,
    default: null
  },
  scoreChange: {
    type: Number,
    default: undefined
  },
  isDealer: {
    type: Boolean,
    default: false
  },
  betAmount: {
    type: Number,
    default: null
  },
  seatNumber: {
    type: Number,
    default: 1
  },
  cardType: {
    type: String,
    default: null
  },
  multiplier: {
    type: Number,
    default: null
  },
  isOnline: {
    type: Boolean,
    default: true
  },
  cardGroups: {
    type: Object,
    default: null
  },
  backCount: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['reveal'])

// 调试：监听 cards 和 backCount 的变化
watch(() => props.cards, (newCards) => {
  console.log(`PlayerSeat [${props.player.userId}] cards 变化:`, newCards)
}, { immediate: true, deep: true })

watch(() => props.backCount, (newBackCount) => {
  console.log(`PlayerSeat [${props.player.userId}] backCount 变化:`, newBackCount)
}, { immediate: true })

watch(() => props.isCurrentUser, (isCurrent) => {
  console.log(`PlayerSeat [${props.player.userId}] isCurrentUser:`, isCurrent, 'cards:', props.cards, 'backCount:', props.backCount)
}, { immediate: true })

const handleRevealCard = () => {
  if (props.isCurrentUser) {
    emit('reveal')
  }
}

// 根据座位号计算投注金额显示位置
const betPositionClass = computed(() => {
  // 根据座位号判断位置（圆形布局）
  // 假设10个座位，座位1在顶部，座位6在底部
  // 右侧座位：2, 3, 4, 5
  // 左侧座位：7, 8, 9, 10
  // 顶部和底部：1, 6
  const seat = props.seatNumber
  if (seat >= 2 && seat <= 5) {
    return 'bet-right' // 右侧
  } else if (seat >= 7 && seat <= 10) {
    return 'bet-left' // 左侧
  } else {
    return 'bet-center' // 顶部或底部
  }
})

// 计算所有5张牌（开牌后使用）
const allCards = computed(() => {
  if (props.cardType) {
    // 已开牌，返回所有5张牌
    if (Array.isArray(props.cards)) {
      // 如果cards直接是数组（开牌后后端返回的格式）
      return props.cards
    } else if (props.cards && props.cards.cards && props.cards.hiddenCard) {
      // 如果cards是对象格式（包含cards和hiddenCard）
      return [...props.cards.cards, props.cards.hiddenCard]
    }
  }
  return []
})
</script>

<style scoped>
.player-seat {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.player-card {
  position: relative;
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  padding: 8px 12px;
  min-width: 110px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

/* 离线状态样式 - 全局变灰色 */
.offline-card {
  background: linear-gradient(135deg, #e5e7eb 0%, #d1d5db 100%) !important;
  border-color: #9ca3af !important;
  opacity: 0.6;
  filter: grayscale(100%);
}

.offline-card .player-name,
.offline-card .score-value,
.offline-card .card-type-display {
  color: #6b7280 !important;
}

.offline-card .cards-container {
  opacity: 0.5;
}

.player-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.current-user-card {
  border: 3px solid #3b82f6;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.4);
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
}

.dealer-card {
  background: linear-gradient(135deg, #ffd700 0%, #ffed4e 100%);
  border: 2px solid #ffb300;
  box-shadow: 0 4px 16px rgba(255, 215, 0, 0.4);
}

.dealer-badge {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #ff6b00 0%, #ff8c00 100%);
  border: 2px solid #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(255, 107, 0, 0.5);
  z-index: 5;
}

/* 在线/离线状态标识 */
.online-status-badge {
  position: absolute;
  top: -10px;
  left: -10px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 6px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: bold;
  z-index: 5;
  border: 1px solid #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
}

.online-status-badge.online {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

.online-status-badge.offline {
  background: linear-gradient(135deg, #6b7280 0%, #4b5563 100%);
  color: white;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  display: inline-block;
}

.status-text {
  font-size: 9px;
  white-space: nowrap;
}

.dealer-text {
  color: #fff;
  font-weight: bold;
  font-size: 14px;
}

.player-name {
  font-size: 14px;
  font-weight: bold;
  color: #333;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.current-user-card .player-name {
  color: #1e40af;
  font-weight: 900;
}

.current-user-badge {
  display: inline-block;
  margin-left: 4px;
  padding: 2px 6px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border-radius: 10px;
  font-size: 10px;
  font-weight: bold;
  vertical-align: middle;
}

.dealer-card .player-name {
  color: #8b4513;
}

.total-score {
  text-align: center;
  margin: 6px 0;
  padding: 4px 0;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
}

.dealer-card .total-score {
  border-color: rgba(139, 69, 19, 0.2);
}

.score-value {
  font-size: 18px;
  font-weight: bold;
  font-family: 'Arial', sans-serif;
}

.score-value.positive {
  color: #10b981;
}

.score-value.negative {
  color: #ef4444;
}

/* 牌型显示 */
.card-type-display {
  text-align: center;
  margin-top: 6px;
  padding: 4px 8px;
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(251, 191, 36, 0.3);
}

.card-type-name {
  font-size: 14px;
  font-weight: bold;
  color: #78350f;
}

.card-type-multiplier {
  font-size: 12px;
  color: #92400e;
  margin-top: 2px;
}

/* 投注金额显示在桌子上方 */
.bet-amount-on-table {
  position: absolute;
  z-index: 5;
}

.bet-amount-on-table.bet-right {
  top: -40px;
  right: -60px;
}

.bet-amount-on-table.bet-left {
  top: -40px;
  left: -60px;
}

.bet-amount-on-table.bet-center {
  top: -40px;
  left: 50%;
  transform: translateX(-50%);
}

.bet-amount-badge {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: bold;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.4);
  white-space: nowrap;
  border: 2px solid white;
}

.cards-container {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;
  position: relative;
}

.cards-container :deep(.card) {
  margin-left: -45px; /* 重叠70%，卡片宽度65px，重叠约45px */
  position: relative;
  z-index: 1;
  transition: all 0.3s ease;
}

.cards-container :deep(.card:first-child) {
  margin-left: 0; /* 第一张牌不重叠 */
}

.cards-container :deep(.card:hover) {
  z-index: 10; /* 悬停时提升层级 */
  transform: translateY(-5px) scale(1.1);
}

.card-group {
  display: flex;
  align-items: center;
  position: relative;
}

.card-group :deep(.card) {
  margin-left: -45px; /* 重叠70% */
  position: relative;
  z-index: 1;
  transition: all 0.3s ease;
}

.card-group :deep(.card:first-child) {
  margin-left: 0; /* 第一张牌不重叠 */
}

.card-group :deep(.card:hover) {
  z-index: 10; /* 悬停时提升层级 */
  transform: translateY(-5px) scale(1.1);
}

.card-group-separator {
  font-size: 24px;
  font-weight: bold;
  color: #fff;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
  margin: 0 12px;
  z-index: 5;
  position: relative;
}

.score-change-animation {
  position: absolute;
  top: -50px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 28px;
  font-weight: bold;
  font-family: 'Arial', sans-serif;
  z-index: 10;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  pointer-events: none;
}

.score-change-animation.positive {
  color: #10b981;
}

.score-change-animation.negative {
  color: #ef4444;
}

.score-change-enter-active,
.score-change-leave-active {
  transition: all 0.5s ease;
}

.score-change-enter-from {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px) scale(0.8);
}

.score-change-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-30px) scale(1.2);
}
</style>

