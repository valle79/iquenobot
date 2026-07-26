import { useQuery } from '@tanstack/react-query'
import { dashboardService } from '@/services/dashboard.service'
import { STALE_TIMES } from '@/config/constants'

export function useDashboard() {
  return useQuery({
    queryKey: ['dashboard', 'overview'],
    queryFn: () => dashboardService.getOverview(),
    staleTime: STALE_TIMES.MEDIUM,
  })
}

export function useUserActivity() {
  return useQuery({
    queryKey: ['dashboard', 'user-activity'],
    queryFn: () => dashboardService.getUserActivity(),
    staleTime: STALE_TIMES.MEDIUM,
  })
}
