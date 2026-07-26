import { useEffect } from 'react'
import { BrowserRouter, useRoutes } from 'react-router-dom'
import { QueryProvider, ThemeProvider, SocketProvider } from '@/providers'
import { routes } from '@/routes'
import { useAuthStore } from '@/core/auth/auth.store'
import { Toaster } from 'sonner'
import { ErrorBoundary } from '@/shared/molecules/ErrorBoundary'
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

export default function App() {
  return (
    <ThemeProvider>
      <QueryProvider>
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
      </QueryProvider>
    </ThemeProvider>
  )
}
