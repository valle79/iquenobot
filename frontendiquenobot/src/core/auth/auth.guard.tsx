import { type ReactNode, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuthStore } from './auth.store'
import type { RoleType } from '@/types/enums'

interface AuthGuardProps {
  children: ReactNode
  roles?: RoleType[]
}

export function AuthGuard({ children, roles }: AuthGuardProps) {
  const navigate = useNavigate()
  const location = useLocation()
  const { isAuthenticated, user } = useAuthStore()

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login', { replace: true, state: { from: location } })
      return
    }

    if (roles && user && !roles.includes(user.role)) {
      const fallback = user.role === 'AGENT' || user.role === 'BOT'
        ? '/conversations'
        : user.role === 'SUPER_ADMIN'
          ? '/admin'
          : '/dashboard'
      navigate(fallback, { replace: true })
    }
  }, [isAuthenticated, user, roles, navigate, location])

  if (!isAuthenticated) return null

  if (roles && user && !roles.includes(user.role)) return null

  return <>{children}</>
}
