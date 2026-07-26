import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Tag, AlignLeft, Hash, Eye } from 'lucide-react'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { Input } from '@/shared/atoms/Input/Input'
import { Toggle } from '@/shared/atoms/Toggle/Toggle'
import { Button } from '@/shared/atoms/Button/Button'
import { useCategoryMutations } from '../hooks/useCategories'
import type { CategoryDto } from '@/types/product'

const schema = z.object({
  name: z.string().min(1, 'Requerido').max(100),
  description: z.string().optional(),
  displayOrder: z.string().optional(),
  active: z.boolean(),
})

type FormData = z.infer<typeof schema>

interface CategoryFormModalProps {
  open: boolean
  category: CategoryDto | null
  onClose: () => void
}

export function CategoryFormModal({ open, category, onClose }: CategoryFormModalProps) {
  const isEdit = !!category
  const { createMutation, updateMutation } = useCategoryMutations()

  const { register, handleSubmit, reset, setValue, watch, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', description: '', displayOrder: '0', active: true },
  })

  const activeValue = watch('active')

  useEffect(() => {
    if (category) {
      reset({
        name: category.name,
        description: category.description || '',
        displayOrder: String(category.displayOrder ?? 0),
        active: category.active,
      })
    } else {
      reset({ name: '', description: '', displayOrder: '0', active: true })
    }
  }, [category, reset])

  const onSubmit = async (data: FormData) => {
    try {
      const dto = {
        name: data.name,
        description: data.description || undefined,
        displayOrder: data.displayOrder ? Number(data.displayOrder) : undefined,
        active: data.active,
      }
      if (isEdit && category) {
        await updateMutation.mutateAsync({ id: category.id, dto })
      } else {
        await createMutation.mutateAsync(dto)
      }
      onClose()
    } catch { /* handled */ }
  }

  const isLoading = createMutation.isPending || updateMutation.isPending

  return (
    <Modal open={open} onClose={onClose} title={isEdit ? 'Editar categoría' : 'Nueva categoría'} size="md">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input label="Nombre" placeholder="Nombre de la categoría" leftIcon={<Tag size={16} />}
          error={errors.name?.message} {...register('name')} />

        <div className="space-y-1.5">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Descripción</label>
          <textarea {...register('description')} rows={2}
            className="h-16 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
            placeholder="Descripción opcional..." />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Input label="Orden" type="number" placeholder="0" leftIcon={<Hash size={16} />}
            error={errors.displayOrder?.message} {...register('displayOrder')} />
          <div className="flex items-end pb-2">
            <Toggle
              label="Activo"
              checked={activeValue}
              onChange={(v) => setValue('active', v)}
              leftIcon={<Eye size={16} />}
            />
          </div>
        </div>

        <div className="flex justify-end gap-3 pt-4">
          <Button type="button" variant="outline" onClick={onClose}>Cancelar</Button>
          <Button type="submit" loading={isLoading}>
            {isEdit ? 'Guardar cambios' : 'Crear categoría'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
