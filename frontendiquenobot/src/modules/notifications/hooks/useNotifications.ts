import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { notificationService } from '@/services/notification.service'
import { useAuthStore } from '@/core/auth/auth.store'
import { STALE_TIMES } from '@/config/constants'

export function useNotifications() {
  const userId = useAuthStore((state) => state.user?.id)

  return useQuery({
    queryKey: ['notifications', userId],
    queryFn: () => notificationService.getByUser(userId!),
    enabled: !!userId,
    staleTime: STALE_TIMES.SHORT,
  })
}

export function useUnreadCount() {
  const userId = useAuthStore((state) => state.user?.id)

  return useQuery({
    queryKey: ['notifications', 'unread-count', userId],
    queryFn: () => notificationService.getUnreadCount(userId!),
    enabled: !!userId,
    staleTime: STALE_TIMES.SHORT,
  })
}

export function useMarkAsRead() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => notificationService.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })
}

export function useMarkAllAsRead() {
  const queryClient = useQueryClient()
  const userId = useAuthStore.getState().user?.id

  return useMutation({
    mutationFn: () => notificationService.markAllAsRead(userId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })
}
