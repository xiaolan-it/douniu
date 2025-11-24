<template>
  <div style="margin-bottom: 160px" v-if="!hasBet" class="fixed bottom-4 left-1/2 transform -translate-x-1/2 bg-gradient-to-br from-amber-50 to-yellow-50 rounded-lg shadow-xl p-3 z-50 border-2 border-amber-200">
    <!-- 倒计时小框 -->
    <div class="absolute -top-10 left-1/2 transform -translate-x-1/2 bg-gradient-to-br from-red-500 to-red-600 text-white rounded-full w-10 h-10 flex items-center justify-center text-base font-bold shadow-lg border-2 border-white">
      {{ countdown }}
    </div>
    
    <div class="text-center mb-2">
      <h3 class="text-sm font-semibold text-gray-800">选择投注金额</h3>
    </div>
    
    <div class="flex gap-2 justify-center">
      <button
        v-for="amount in betAmounts"
        :key="amount"
        @click="handleBet(amount)"
        :disabled="hasBet"
        class="px-3 py-1.5 bg-gradient-to-r from-green-500 to-green-600 text-white rounded-md text-sm font-semibold hover:from-green-600 hover:to-green-700 disabled:opacity-50 transition-all duration-200 shadow-md transform hover:scale-105 min-w-[60px]"
      >
        {{ amount }}元
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  countdown: {
    type: Number,
    default: 0
  },
  hasBet: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['bet'])

const betAmounts = [10, 20, 30, 40, 50]

const handleBet = (amount) => {
  emit('bet', amount)
}
</script>

