import { type ReactNode, useEffect } from 'react'
import { useAuthStore } from '@/core/auth/auth.store'
import { useSocketStore } from '@/core/websocket/socket.store'

export function SocketProvider({ children }: { children: ReactNode }) {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
  const connect = useSocketStore((state) => state.connect)
  const disconnect = useSocketStore((state) => state.disconnect)

  useEffect(() => {
    if (isAuthenticated) {
      connect()
    } else {
      disconnect()
    }

    return () => {
      disconnect()
    }
  }, [isAuthenticated, connect, disconnect])

  return <>{children}</>
}
