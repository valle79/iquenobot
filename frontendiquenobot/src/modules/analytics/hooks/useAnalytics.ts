import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/core/auth/auth.store'
import { dashboardService } from '@/services/dashboard.service'
import { STALE_TIMES } from '@/config/constants'

function canAccessAnalytics() {
  const role = useAuthStore.getState().user?.role
  return role === 'TENANT_ADMIN' || role === 'SUPERVISOR' || role === 'SUPER_ADMIN'
}

function canAccessUserActivity() {
  const role = useAuthStore.getState().user?.role
  return role === 'TENANT_ADMIN' || role === 'SUPER_ADMIN'
}

export function useAnalytics() {
  const enabled = canAccessAnalytics()

  const conversations = useQuery({
    queryKey: ['dashboard', 'conversations-stats'],
    queryFn: () => dashboardService.getConversationStats(),
    staleTime: STALE_TIMES.MEDIUM,
    enabled,
  })

  const leads = useQuery({
    queryKey: ['dashboard', 'leads-stats'],
    queryFn: () => dashboardService.getLeadStats(),
    staleTime: STALE_TIMES.MEDIUM,
    enabled,
  })

  const contacts = useQuery({
    queryKey: ['dashboard', 'contacts-stats'],
    queryFn: () => dashboardService.getContactStats(),
    staleTime: STALE_TIMES.MEDIUM,
    enabled,
  })

  const products = useQuery({
    queryKey: ['dashboard', 'products-stats'],
    queryFn: () => dashboardService.getProductStats(),
    staleTime: STALE_TIMES.MEDIUM,
    enabled,
  })

  const userActivity = useQuery({
    queryKey: ['dashboard', 'user-activity'],
    queryFn: () => dashboardService.getUserActivity(),
    staleTime: STALE_TIMES.MEDIUM,
    enabled: canAccessUserActivity(),
  })

  const isLoading = conversations.isLoading || leads.isLoading || contacts.isLoading || products.isLoading
  const isError = conversations.isError || leads.isError || contacts.isError || products.isError

  return {
    conversationStats: conversations.data,
    leadStats: leads.data,
    contactStats: contacts.data,
    productStats: products.data,
    userActivity: userActivity.data ?? [],
    isLoading,
    isError,
    refetch: () => {
      conversations.refetch()
      leads.refetch()
      contacts.refetch()
      products.refetch()
      userActivity.refetch()
    },
  }
}
