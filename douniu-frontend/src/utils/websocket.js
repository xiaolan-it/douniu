import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

let stompClient = null
let reconnectTimer = null
let heartbeatTimer = null

export const connectWebSocket = (token, onConnect, onError) => {
  if (stompClient && stompClient.connected) {
    return stompClient
  }

  // 动态获取当前访问地址的 origin（协议+主机+端口）
  const wsUrl = `${window.location.origin}/api/ws`
  console.log('WebSocket连接地址:', wsUrl)
  const socket = new SockJS(wsUrl)
  stompClient = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: 500,
    heartbeatIncoming: 3000,
    heartbeatOutgoing: 3000,
    onConnect: (frame) => {
      console.log('WebSocket连接成功', frame)
      
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
      console.error('WebSocket错误', frame)
      if (onError) {
        onError(frame)
      }
      // 自动重连
      scheduleReconnect(token, onConnect, onError)
    },
    onWebSocketClose: () => {
      console.log('WebSocket连接关闭')
      stopHeartbeat()
      // 自动重连
      scheduleReconnect(token, onConnect, onError)
    }
  })

  stompClient.activate()
  return stompClient
}

const scheduleReconnect = (token, onConnect, onError) => {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
  }
  reconnectTimer = setTimeout(() => {
    console.log('尝试重新连接WebSocket...')
    connectWebSocket(token, onConnect, onError)
  }, 5000)
}

const startHeartbeat = () => {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (stompClient && stompClient.connected) {
      stompClient.publish({
        destination: '/app/heartbeat',
        body: JSON.stringify({ timestamp: Date.now() })
      })
    }
  }, 30000) // 每30秒发送一次心跳
}

const stopHeartbeat = () => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

export const disconnectWebSocket = () => {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  stopHeartbeat()
  if (stompClient) {
    stompClient.deactivate()
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

