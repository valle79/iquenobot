import { useEffect, useState } from 'react'
import { useAuthStore } from './auth.store'

export function useAuthHydration(): boolean {
  const [hasHydrated, setHasHydrated] = useState<boolean>(() =>
    useAuthStore.persist.hasHydrated(),
  )

  useEffect(() => {
    if (useAuthStore.persist.hasHydrated()) {
      setHasHydrated(true)
      return
    }

    const unsubscribe = useAuthStore.persist.onFinishHydration(() => {
      setHasHydrated(true)
    })

    return unsubscribe
  }, [])

  return hasHydrated
}
