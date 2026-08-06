import { useEffect } from 'react'
import { API_BASE_URL } from '@/config/constants'

const KEEP_ALIVE_INTERVAL_MS = 10_000
const KEEP_ALIVE_TIMEOUT_MS = 5_000

export function useKeepAlive() {
  useEffect(() => {
    const ping = async () => {
      const controller = new AbortController()
      const timeout = window.setTimeout(() => controller.abort(), KEEP_ALIVE_TIMEOUT_MS)
      try {
        await fetch(`${API_BASE_URL}/ping`, {
          method: 'GET',
          headers: { Accept: 'application/json' },
          cache: 'no-store',
          signal: controller.signal,
        })
      } catch {
        // El fail del ping es esperado cuando la app duerme; se reenvía en el siguiente tick.
      } finally {
        window.clearTimeout(timeout)
      }
    }

    // Ping incondicional cada 10 segundos mientras la app esté abierta
    // (incluso con la pestaña en segundo plano), para que Render nunca duerma.
    void ping()
    const timer = window.setInterval(() => void ping(), KEEP_ALIVE_INTERVAL_MS)

    return () => {
      window.clearInterval(timer)
    }
  }, [])
}