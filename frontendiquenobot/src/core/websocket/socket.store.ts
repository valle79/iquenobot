import { create } from 'zustand'
import { WS_URL } from '@/config/constants'
import { useAuthStore } from '@/core/auth/auth.store'

type MessageHandler = (payload: unknown) => void

interface SocketState {
  socket: WebSocket | null
  isConnected: boolean
  error: string | null
  listeners: Map<string, Set<MessageHandler>>
  connect: () => void
  disconnect: () => void
  clear: () => void
  on: (event: string, handler: MessageHandler) => void
  off: (event: string, handler: MessageHandler) => void
  emit: (type: string, payload?: unknown) => void
}

let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let currentWs: WebSocket | null = null

function createConnection(token: string): WebSocket {
  const base = WS_URL || window.location.origin
  const url = `${base.replace(/^http/, 'ws')}/ws/notifications?token=${token}`
  const ws = new WebSocket(url)

  ws.onopen = () => {
    useSocketStore.setState({ isConnected: true, error: null })
  }

  ws.onclose = (event) => {
    useSocketStore.setState({ isConnected: false })
    if (event.code !== 1000) {
      scheduleReconnect()
    }
  }

  ws.onerror = () => {
    useSocketStore.setState({ error: 'Error en conexión WebSocket' })
  }

  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      const { type, payload } = msg
      if (type) {
        const { listeners } = useSocketStore.getState()
        const handlers = listeners.get(type)
        if (handlers) {
          handlers.forEach((handler) => handler(payload ?? msg))
        }
      }
    } catch {
      // ignore malformed messages
    }
  }

  currentWs = ws
  return ws
}

function scheduleReconnect() {
  if (reconnectTimer) clearTimeout(reconnectTimer)
  reconnectTimer = setTimeout(() => {
    const token = useAuthStore.getState().accessToken
    if (token) {
      const ws = createConnection(token)
      useSocketStore.setState({ socket: ws })
    }
  }, 5000)
}

export const useSocketStore = create<SocketState>((set, get) => ({
  socket: null,
  isConnected: false,
  error: null,
  listeners: new Map(),

  connect: () => {
    const token = useAuthStore.getState().accessToken
    if (!token) return

    // Ya hay una conexión activa o en curso: no recrearla.
    // (Crear otra vez el WebSocket reiniciaría el mapa de listeners y
    // los hooks registrados con useSocketEvent dejarían de recibir eventos.)
    if (
      currentWs &&
      (currentWs.readyState === WebSocket.OPEN ||
        currentWs.readyState === WebSocket.CONNECTING)
    ) {
      return
    }

    if (currentWs) {
      currentWs.close(1000)
    }

    const ws = createConnection(token)
    // NO reiniciar listeners: los eventos siguen llegando a los hooks
    // registrados (message:new, conversation:updated, etc.).
    set({ socket: ws })
  },

  disconnect: () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (currentWs) {
      currentWs.close(1000)
      currentWs = null
    }
    set({ socket: null, isConnected: false, error: null, listeners: new Map() })
  },

  clear: () => {
    get().disconnect()
  },

  on: (event: string, handler: MessageHandler) => {
    set((state) => {
      const listeners = new Map(state.listeners)
      if (!listeners.has(event)) {
        listeners.set(event, new Set())
      }
      listeners.get(event)!.add(handler)
      return { listeners }
    })
  },

  off: (event: string, handler: MessageHandler) => {
    set((state) => {
      const listeners = new Map(state.listeners)
      const handlers = listeners.get(event)
      if (handlers) {
        handlers.delete(handler)
        if (handlers.size === 0) listeners.delete(event)
      }
      return { listeners }
    })
  },

  emit: (type: string, payload?: unknown) => {
    const ws = get().socket
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type, payload }))
    }
  },
}))
