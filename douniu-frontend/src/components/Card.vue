<template>
  <div
    class="card"
    :class="{ 
      'card-hidden': hidden, 
      'card-visible': !hidden,
      'card-clickable': clickable
    }"
    :style="cardStyle"
    @click="handleClick"
  >
    <div v-if="showRevealText" class="reveal-text">点击开牌</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import CardsImage from '@/assets/Cards.png'

const props = defineProps({
  suit: {
    type: Number,
    default: -1 // -1表示背面
  },
  rank: {
    type: Number,
    default: -1
  },
  hidden: {
    type: Boolean,
    default: false
  },
  clickable: {
    type: Boolean,
    default: false
  },
  showRevealText: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

const handleClick = () => {
  if (props.clickable && props.hidden) {
    emit('click')
  }
}

const cardStyle = computed(() => {
  // 基础样式：所有牌都使用相同的大小
  const baseStyle = {
    width: '65px',
    height: '75px'
  }

  if (props.hidden || props.suit === -1 || props.rank === -1) {
    // 显示背面
    return {
      ...baseStyle,
      backgroundImage: `url(${CardsImage})`,
      backgroundPosition: '-845px 0px', // 第14列，第1行（背面）
      backgroundSize: '910px 300px'
    }
  }

  // 计算牌的位置
  // 每张牌大小：65x75
  // 列：rank - 1 (A=1, 2=2, ..., K=13)
  // 行：suit (0=黑桃, 1=红桃, 2=梅花, 3=方块)
  // 注意：Cards.png 图片中的排列是：0=黑桃, 1=红桃, 2=方块, 3=梅花
  // 所以需要将新的 suit 值映射到图片中的行号
  const x = -(props.rank - 1) * 65
  // 映射：0(黑桃)->0, 1(红桃)->1, 2(梅花)->3, 3(方块)->2
  const suitToImageRow = {
    0: 0, // 黑桃 -> 行0
    1: 1, // 红桃 -> 行1
    2: 3, // 梅花 -> 行3
    3: 2  // 方块 -> 行2
  }
  const imageRow = suitToImageRow[props.suit] ?? props.suit
  const y = -imageRow * 75

  return {
    ...baseStyle,
    backgroundImage: `url(${CardsImage})`,
    backgroundPosition: `${x}px ${y}px`,
    backgroundSize: '910px 300px'
  }
})
</script>

<style scoped>
.card {
  width: 65px;
  height: 75px;
  background-repeat: no-repeat;
  border-radius: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.card-visible {
  transform: scale(1);
  opacity: 1;
}

.card-hidden {
  transform: scale(1); /* 背面牌和正面牌一样大小 */
  opacity: 1; /* 保持完全不透明 */
}

.card-clickable {
  cursor: pointer;
  transition: all 0.3s ease;
}

.card-clickable:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
}

.reveal-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 12px;
  font-weight: bold;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.8);
  pointer-events: none;
  z-index: 10;
}

@media (max-width: 640px) {
  .card {
    width: 52px;
    height: 60px;
    background-size: 728px 240px;
  }
}
</style>

