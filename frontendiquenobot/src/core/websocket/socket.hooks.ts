import { useEffect, useCallback, useRef } from 'react'
import { useSocketStore } from './socket.store'

type MessageHandler = (payload: unknown) => void

export function useSocketEvent<T = unknown>(event: string, handler: (data: T) => void) {
  const handlerRef = useRef(handler)
  handlerRef.current = handler
  const on = useSocketStore((s) => s.on)
  const off = useSocketStore((s) => s.off)

  useEffect(() => {
    const wrapped: MessageHandler = (payload) => handlerRef.current(payload as T)
    on(event, wrapped)
    return () => {
      off(event, wrapped)
    }
  }, [event, on, off])
}

export function useSocketEmit() {
  const emit = useSocketStore((s) => s.emit)
  return useCallback((type: string, payload?: unknown) => emit(type, payload), [emit])
}

export function useSocketJoinConversation(conversationId: string) {
  const emit = useSocketEmit()
  const isConnected = useSocketStore((s) => s.isConnected)

  useEffect(() => {
    if (!isConnected || !conversationId) return

    emit('conversation:join', { conversationId })

    return () => {
      emit('conversation:leave', { conversationId })
    }
  }, [isConnected, conversationId, emit])
}
