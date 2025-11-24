import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useGameStore = defineStore('game', () => {
  const room = ref(null)
  const players = ref([])
  const currentGameRecord = ref(null)
  const gameCards = ref({}) // { userId: { cards: [], hiddenCard: {} } }
  const gameBets = ref({}) // { userId: betAmount }
  const gameDetails = ref({}) // { userId: GameDetail }
  const countdown = ref(0) // 倒计时（秒）
  const gamePhase = ref('waiting') // waiting, betting, dealing, revealing, settling

  const setRoom = (roomData) => {
    room.value = roomData
  }

  const setPlayers = (playersData) => {
    players.value = playersData
  }

  const setCurrentGameRecord = (record) => {
    currentGameRecord.value = record
  }

  const setGameCards = (cards) => {
    gameCards.value = cards
  }

  const setGameBet = (userId, betAmount) => {
    gameBets.value[userId] = betAmount
  }

  const setGameDetails = (details) => {
    gameDetails.value = details
  }

  const setCountdown = (seconds) => {
    countdown.value = seconds
  }

  const setGamePhase = (phase) => {
    gamePhase.value = phase
  }

  const reset = () => {
    room.value = null
    players.value = []
    currentGameRecord.value = null
    gameCards.value = {}
    gameBets.value = {}
    gameDetails.value = {}
    countdown.value = 0
    gamePhase.value = 'waiting'
  }

  return {
    room,
    players,
    currentGameRecord,
    gameCards,
    gameBets,
    gameDetails,
    countdown,
    gamePhase,
    setRoom,
    setPlayers,
    setCurrentGameRecord,
    setGameCards,
    setGameBet,
    setGameDetails,
    setCountdown,
    setGamePhase,
    reset
  }
})

