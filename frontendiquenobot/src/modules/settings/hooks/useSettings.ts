import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { settingService } from '@/services/setting.service'
import { STALE_TIMES } from '@/config/constants'
import { toast } from 'sonner'
import type { UpdateSettingsRequest } from '@/types/setting'

export function useSettings() {
  return useQuery({
    queryKey: ['settings'],
    queryFn: () => settingService.getAll(),
    staleTime: STALE_TIMES.MEDIUM,
  })
}

export function useSettingsByCategory(category: string | undefined) {
  return useQuery({
    queryKey: ['settings', category],
    queryFn: () => settingService.getByCategory(category!),
    enabled: !!category,
    staleTime: STALE_TIMES.MEDIUM,
  })
}

export function useUpdateSettings() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (dto: UpdateSettingsRequest) => settingService.update(dto),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['settings'] })
      queryClient.invalidateQueries({ queryKey: ['settings', variables.category] })
      toast.success('Configuración actualizada')
    },
  })
}
