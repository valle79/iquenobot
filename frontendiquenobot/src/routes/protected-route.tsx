import { Suspense, type ReactNode } from 'react'
import { AuthGuard } from '@/core/auth/auth.guard'
import { Skeleton } from '@/shared/atoms/Skeleton/Skeleton'
import type { RoleType } from '@/types/enums'

interface ProtectedRouteProps {
  children: ReactNode
  roles?: RoleType[]
}

function RouteFallback() {
  return (
    <div className="flex h-screen items-center justify-center p-8">
      <div className="w-full max-w-4xl space-y-6">
        <div className="flex items-center gap-3">
          <Skeleton variant="circular" width={40} height={40} />
          <div className="space-y-2">
            <Skeleton width={160} height={20} />
            <Skeleton width={100} height={14} />
          </div>
        </div>
        <Skeleton variant="rectangular" width="100%" height={200} />
        <div className="grid grid-cols-3 gap-4">
          <Skeleton variant="rectangular" height={120} />
          <Skeleton variant="rectangular" height={120} />
          <Skeleton variant="rectangular" height={120} />
        </div>
      </div>
    </div>
  )
}

export function ProtectedRoute({ children, roles }: ProtectedRouteProps) {
  return (
    <AuthGuard roles={roles}>
      <Suspense fallback={<RouteFallback />}>
        {children}
      </Suspense>
    </AuthGuard>
  )
}
