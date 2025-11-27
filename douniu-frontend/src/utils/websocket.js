import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

let stompClient = null
let reconnectTimer = null
let heartbeatTimer = null
let isReconnecting = false // 防止重复重连
let reconnectAttempts = 0 // 重连次数
const MAX_RECONNECT_ATTEMPTS = 10 // 最大重连次数
const INITIAL_RECONNECT_DELAY = 2000 // 初始重连延迟（毫秒）
const MAX_RECONNECT_DELAY = 30000 // 最大重连延迟（毫秒）

export const connectWebSocket = (token, onConnect, onError) => {
  // 如果已经连接，直接返回
  if (stompClient && stompClient.connected) {
    console.log('WebSocket已连接，跳过重复连接')
    return stompClient
  }

  // 如果正在重连，不重复连接
  if (isReconnecting) {
    console.log('WebSocket正在重连中，跳过重复连接')
    return stompClient
  }

  // 清理旧连接
  if (stompClient) {
    try {
      stompClient.deactivate()
    } catch (e) {
      console.warn('清理旧连接时出错:', e)
    }
    stompClient = null
  }

  // 重置重连状态
  isReconnecting = false
  reconnectAttempts = 0

  // 动态获取当前访问地址的 origin（协议+主机+端口）
  const wsUrl = `${window.location.origin}/api/ws`
  console.log('WebSocket连接地址:', wsUrl)
  
  const socket = new SockJS(wsUrl)
  stompClient = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: 0, // 禁用自动重连，手动控制
    heartbeatIncoming: 4000, // 4秒
    heartbeatOutgoing: 4000, // 4秒
    connectionTimeout: 5000, // 连接超时5秒
    onConnect: (frame) => {
      console.log('WebSocket连接成功', frame)
      
      // 重置重连状态
      isReconnecting = false
      reconnectAttempts = 0
      
      // 清除重连定时器
      if (reconnectTimer) {
        clearTimeout(reconnectTimer)
        reconnectTimer = null
      }
      
      // 发送认证token（如果需要）
      if (token) {
        stompClient.publish({
          destination: '/app/auth',
          body: JSON.stringify({ token })
        })
      }

      // 启动心跳
      startHeartbeat()

      if (onConnect) {
        onConnect(stompClient)
      }
    },
    onStompError: (frame) => {
      console.error('WebSocket STOMP错误', frame)
      if (onError) {
        onError(frame)
      }
      // 自动重连
      scheduleReconnect(token, onConnect, onError)
    },
    onWebSocketClose: (event) => {
      console.log('WebSocket连接关闭', event)
      stopHeartbeat()
      // 只有在非主动断开的情况下才重连
      if (!event.wasClean) {
        scheduleReconnect(token, onConnect, onError)
      }
    },
    onWebSocketError: (event) => {
      console.error('WebSocket底层错误', event)
      // SockJS的info请求可能没有Upgrade头，这是正常的，不需要重连
      // 只有在真正的连接错误时才重连
      if (event.type === 'error' && event.target && event.target.readyState !== WebSocket.CONNECTING) {
        scheduleReconnect(token, onConnect, onError)
      }
    }
  })

  stompClient.activate()
  return stompClient
}

const scheduleReconnect = (token, onConnect, onError) => {
  // 如果已经在重连，不重复调度
  if (isReconnecting || reconnectTimer) {
    return
  }

  // 检查重连次数
  if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
    console.error('WebSocket重连次数已达上限，停止重连')
    isReconnecting = false
    return
  }

  isReconnecting = true
  reconnectAttempts++

  // 指数退避算法：延迟时间逐渐增加
  const delay = Math.min(
    INITIAL_RECONNECT_DELAY * Math.pow(2, reconnectAttempts - 1),
    MAX_RECONNECT_DELAY
  )

  console.log(`WebSocket将在 ${delay}ms 后尝试重连 (第 ${reconnectAttempts} 次)`)
  
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    console.log('尝试重新连接WebSocket...')
    // 清理旧连接
    if (stompClient) {
      try {
        stompClient.deactivate()
      } catch (e) {
        console.warn('清理旧连接时出错:', e)
      }
      stompClient = null
    }
    connectWebSocket(token, onConnect, onError)
  }, delay)
}

const startHeartbeat = () => {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (stompClient && stompClient.connected) {
      try {
        stompClient.publish({
          destination: '/app/heartbeat',
          body: JSON.stringify({ timestamp: Date.now() })
        })
      } catch (e) {
        console.error('发送心跳失败:', e)
        stopHeartbeat()
      }
    } else {
      // 如果连接断开，停止心跳
      stopHeartbeat()
    }
  }, 10000) // 每10秒发送一次心跳（比配置的4秒间隔长，避免冲突）
}

const stopHeartbeat = () => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

export const disconnectWebSocket = () => {
  console.log('主动断开WebSocket连接')
  
  // 停止重连
  isReconnecting = false
  reconnectAttempts = 0
  
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  
  stopHeartbeat()
  
  if (stompClient) {
    try {
      stompClient.deactivate()
    } catch (e) {
      console.warn('断开WebSocket连接时出错:', e)
    }
    stompClient = null
  }
}

export const subscribe = (destination, callback) => {
  if (stompClient && stompClient.connected) {
    console.log('订阅主题:', destination)
    const subscription = stompClient.subscribe(destination, (message) => {
      console.log('收到消息，主题:', destination, '消息体:', message.body)
      try {
        const data = JSON.parse(message.body)
        callback(data)
      } catch (error) {
        console.error('解析消息失败', error, '原始消息:', message.body)
        callback(message.body)
      }
    })
    console.log('订阅成功，subscription:', subscription)
    return subscription
  } else {
    console.warn('WebSocket未连接，无法订阅:', destination, 'stompClient:', stompClient, 'connected:', stompClient?.connected)
  }
  return null
}

export const sendMessage = (destination, body) => {
  if (stompClient && stompClient.connected) {
    stompClient.publish({
      destination,
      body: typeof body === 'string' ? body : JSON.stringify(body)
    })
    return true
  }
  console.warn('WebSocket未连接，无法发送消息')
  return false
}

export const getStompClient = () => {
  return stompClient
}

