import {
  useQuery,
  useMutation,
  useQueryClient,
} from '@tanstack/react-query'

import { notificationService } from '@/services/notification.service'
import { useAuthStore } from '@/core/auth/auth.store'
import { STALE_TIMES } from '@/config/constants'

export function useNotifications(
  page: number = 0,
  size: number = 20,
  onlyUnread: boolean = false,
) {

  const userId =
    useAuthStore((state) => state.user?.id)

  return useQuery({
    queryKey: [
      'notifications',
      userId,
      onlyUnread,
      page,
      size,
    ],

    queryFn: () => {

      if (!userId) {
        throw new Error('Usuario no autenticado')
      }

      return onlyUnread
        ? notificationService.getUnread(
            userId,
            { page, size },
          )
        : notificationService.getByUser(
            userId,
            { page, size },
          )
    },

    enabled: !!userId,

    staleTime: STALE_TIMES.SHORT,

    // Mantiene las notificaciones visibles
    // mientras React Query hace el refetch
    placeholderData: (previousData) => previousData,

    refetchOnWindowFocus: false,
  })
}

export function useUnreadCount() {

  const userId =
    useAuthStore((state) => state.user?.id)

  return useQuery({
    queryKey: [
      'notifications',
      'unread-count',
      userId,
    ],

    queryFn: () => {

      if (!userId) {
        throw new Error('Usuario no autenticado')
      }

      return notificationService.getUnreadCount(userId)
    },

    enabled: !!userId,

    staleTime: STALE_TIMES.SHORT,

    refetchOnWindowFocus: false,
  })
}

export function useMarkAsRead() {

  const queryClient = useQueryClient()

  return useMutation({

    mutationFn: (id: string) =>
      notificationService.markAsRead(id),

    onSuccess: () => {

      queryClient.invalidateQueries({
        queryKey: ['notifications'],
        exact: false,
      })

      queryClient.invalidateQueries({
        queryKey: ['notifications', 'unread-count'],
        exact: false,
      })
    },
  })
}

export function useMarkAsUnread() {

  const queryClient = useQueryClient()

  return useMutation({

    mutationFn: (id: string) =>
      notificationService.markAsUnread(id),

    onSuccess: () => {

      queryClient.invalidateQueries({
        queryKey: ['notifications'],
        exact: false,
      })

      queryClient.invalidateQueries({
        queryKey: ['notifications', 'unread-count'],
        exact: false,
      })
    },
  })
}

export function useMarkAllAsRead() {

  const queryClient = useQueryClient()

  const userId =
    useAuthStore((state) => state.user?.id)

  return useMutation({

    mutationFn: () => {

      if (!userId) {
        throw new Error('Usuario no autenticado')
      }

      return notificationService.markAllAsRead(userId)
    },

    onSuccess: () => {

      queryClient.invalidateQueries({
        queryKey: ['notifications'],
        exact: false,
      })

      queryClient.invalidateQueries({
        queryKey: ['notifications', 'unread-count'],
        exact: false,
      })
    },
  })
}

export function useDeleteNotification() {

  const queryClient = useQueryClient()

  return useMutation({

    mutationFn: (id: string) =>
      notificationService.delete(id),

    onSuccess: () => {

      queryClient.invalidateQueries({
        queryKey: ['notifications'],
        exact: false,
      })

      queryClient.invalidateQueries({
        queryKey: ['notifications', 'unread-count'],
        exact: false,
      })
    },
  })
}
