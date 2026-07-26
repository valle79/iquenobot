import { useQuery } from '@tanstack/react-query'
import { dashboardService } from '@/services/dashboard.service'
import { STALE_TIMES } from '@/config/constants'

export function useAnalytics() {
  const conversations = useQuery({
    queryKey: ['dashboard', 'conversations-stats'],
    queryFn: () => dashboardService.getConversationStats(),
    staleTime: STALE_TIMES.MEDIUM,
  })

  const leads = useQuery({
    queryKey: ['dashboard', 'leads-stats'],
    queryFn: () => dashboardService.getLeadStats(),
    staleTime: STALE_TIMES.MEDIUM,
  })

  const contacts = useQuery({
    queryKey: ['dashboard', 'contacts-stats'],
    queryFn: () => dashboardService.getContactStats(),
    staleTime: STALE_TIMES.MEDIUM,
  })

  const products = useQuery({
    queryKey: ['dashboard', 'products-stats'],
    queryFn: () => dashboardService.getProductStats(),
    staleTime: STALE_TIMES.MEDIUM,
  })

  const userActivity = useQuery({
    queryKey: ['dashboard', 'user-activity'],
    queryFn: () => dashboardService.getUserActivity(),
    staleTime: STALE_TIMES.MEDIUM,
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
