import { useEffect } from 'react'
import { BrowserRouter, useRoutes } from 'react-router-dom'
import { QueryProvider, ThemeProvider, SocketProvider } from '@/providers'
import { routes } from '@/routes'
import { useAuthStore } from '@/core/auth/auth.store'
import { useAuthHydration } from '@/core/auth/useAuthHydration'
import { useKeepAlive } from '@/hooks/useKeepAlive'
import { Toaster } from 'sonner'
import { ErrorBoundary } from '@/shared/molecules/ErrorBoundary'
import { Spinner } from '@/shared/atoms/Spinner/Spinner'
import '@/config/dayjs'
import '@/config/i18n'
import '@/styles/globals.css'

function AuthInitializer({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const refreshToken = useAuthStore((s) => s.refreshToken)
  const refreshAuth = useAuthStore((s) => s.refreshAuth)

  useEffect(() => {
    if (!isAuthenticated && refreshToken) {
      refreshAuth().catch(() => {})
    }
  }, [])

  return <>{children}</>
}

function AppRoutes() {
  return useRoutes(routes)
}

function AuthHydrationGate({ children }: { children: React.ReactNode }) {
  const hasHydrated = useAuthHydration()

  // Bloquear el render de rutas y layout hasta que el persist
  // de auth esté hidratado: nunca mostrar datos de otro tenant
  if (!hasHydrated) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-white dark:bg-gray-950">
        <Spinner size="lg" />
      </div>
    )
  }

  return <>{children}</>
}

export default function App() {
  useKeepAlive()

  return (
    <ThemeProvider>
      <QueryProvider>
        <AuthHydrationGate>
          <BrowserRouter>
            <AuthInitializer>
              <SocketProvider>
                <ErrorBoundary>
                  <AppRoutes />
                </ErrorBoundary>
                <Toaster
                  position="top-right"
                  richColors
                  closeButton
                  duration={4000}
                />
              </SocketProvider>
            </AuthInitializer>
          </BrowserRouter>
        </AuthHydrationGate>
      </QueryProvider>
    </ThemeProvider>
  )
}
