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

const joinLeaveTimers = new Map<string, ReturnType<typeof setTimeout>>()

export function useSocketJoinConversation(conversationId: string) {
  const emit = useSocketEmit()
  const isConnected = useSocketStore((s) => s.isConnected)
  const previousIdRef = useRef<string | null>(null)

  useEffect(() => {
    if (conversationId && previousIdRef.current && previousIdRef.current !== conversationId) {
      const oldId = previousIdRef.current
      emit('conversation:leave', { conversationId: oldId })
    }
  }, [conversationId, emit])

  useEffect(() => {
    if (!isConnected || !conversationId) return

    previousIdRef.current = conversationId

    const timerKey = `join:${conversationId}`
    const existing = joinLeaveTimers.get(timerKey)
    if (existing) clearTimeout(existing)

    const timer = setTimeout(() => {
      emit('conversation:join', { conversationId })
      joinLeaveTimers.delete(timerKey)
    }, 0)

    joinLeaveTimers.set(timerKey, timer)

    return () => {
      const existingTimer = joinLeaveTimers.get(timerKey)
      if (existingTimer) clearTimeout(existingTimer)
      joinLeaveTimers.delete(timerKey)
      emit('conversation:leave', { conversationId })
    }
  }, [isConnected, conversationId, emit])
}
