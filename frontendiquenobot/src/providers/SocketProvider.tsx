import { type ReactNode, useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/core/auth/auth.store'
import { useSocketStore } from '@/core/websocket/socket.store'

export function SocketProvider({ children }: { children: ReactNode }) {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
  const isConnected = useSocketStore((state) => state.isConnected)
  const connect = useSocketStore((state) => state.connect)
  const disconnect = useSocketStore((state) => state.disconnect)
  const queryClient = useQueryClient()

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

  // Cada vez que el WebSocket se conecta (o se reconecta tras una caída),
  // refrescar conversaciones y mensajes desde la API: recupera lo que se
  // haya perdido mientras no había socket (eventos sin entregar).
  useEffect(() => {
    if (isConnected && isAuthenticated) {
      queryClient.invalidateQueries({ queryKey: ['conversations'] })
      queryClient.invalidateQueries({ queryKey: ['conversation'] })
      queryClient.invalidateQueries({ queryKey: ['messages'] })
    }
  }, [isConnected, isAuthenticated, queryClient])

  return <>{children}</>
}
