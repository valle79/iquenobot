import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/core/auth/auth.store'
import { dashboardService } from '@/services/dashboard.service'
import { STALE_TIMES } from '@/config/constants'

function canAccessDashboard() {
  const role = useAuthStore.getState().user?.role
  return role === 'TENANT_ADMIN' || role === 'SUPERVISOR' || role === 'SUPER_ADMIN'
}

function canAccessUserActivity() {
  const role = useAuthStore.getState().user?.role
  return role === 'TENANT_ADMIN' || role === 'SUPER_ADMIN'
}

export function useDashboard() {
  return useQuery({
    queryKey: ['dashboard', 'overview'],
    queryFn: () => dashboardService.getOverview(),
    staleTime: STALE_TIMES.MEDIUM,
    enabled: canAccessDashboard(),
  })
}

export function useUserActivity() {
  return useQuery({
    queryKey: ['dashboard', 'user-activity'],
    queryFn: () => dashboardService.getUserActivity(),
    staleTime: STALE_TIMES.MEDIUM,
    enabled: canAccessUserActivity(),
  })
}
