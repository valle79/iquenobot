import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Package, DollarSign, Hash, Tag } from 'lucide-react'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { Input } from '@/shared/atoms/Input/Input'
import { Select } from '@/shared/atoms/Select/Select'
import { Button } from '@/shared/atoms/Button/Button'
import { ImageUploader } from '@/shared/molecules/ImageUploader'
import { useCreateProduct, useUpdateProduct } from '../hooks/useProducts'
import { useActiveCategories } from '../hooks/useCategories'
import type { ProductDto } from '@/types/product'

const schema = z.object({
  name: z.string().min(1, 'Requerido').max(200),
  sku: z.string().optional(),
  price: z.string().min(1, 'Requerido'),
  stockQuantity: z.string().min(1, 'Requerido'),
  description: z.string().optional(),
  categoryId: z.string().optional(),
  status: z.string().min(1, 'Requerido'),
  tags: z.string().optional(),
})

type FormData = z.infer<typeof schema>

const statusOptions = [
  { value: 'ACTIVE', label: 'Activo' },
  { value: 'INACTIVE', label: 'Inactivo' },
  { value: 'OUT_OF_STOCK', label: 'Sin stock' },
  { value: 'DISCONTINUED', label: 'Descontinuado' },
]

interface ProductFormModalProps {
  open: boolean
  product: ProductDto | null
  onClose: () => void
}

export function ProductFormModal({ open, product, onClose }: ProductFormModalProps) {
  const isEdit = !!product
  const createMutation = useCreateProduct()
  const updateMutation = useUpdateProduct()
  const { data: categoryOptions } = useActiveCategories()
  const [imageUrl, setImageUrl] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) })

  useEffect(() => {
    if (product) {
      setImageUrl(product.imageUrl || null)
      reset({
        name: product.name,
        sku: product.sku || '',
        price: String(product.price),
        stockQuantity: String(product.stockQuantity),
        description: product.description || '',
        categoryId: product.category?.id || '',
        status: product.status,
        tags: product.tags || '',
      })
    } else {
      setImageUrl(null)
      reset({ name: '', sku: '', price: '', stockQuantity: '0', description: '', categoryId: '', status: 'ACTIVE', tags: '' })
    }
  }, [product, reset])

  const onSubmit = async (data: FormData) => {
    try {
      const dto = {
        name: data.name,
        sku: data.sku || undefined,
        price: Number(data.price),
        stockQuantity: Number(data.stockQuantity),
        description: data.description || undefined,
        categoryId: data.categoryId || undefined,
        status: data.status as any,
        tags: data.tags || undefined,
        imageUrl: imageUrl || undefined,
      }
      if (isEdit && product) {
        await updateMutation.mutateAsync({ id: product.id, dto })
      } else {
        await createMutation.mutateAsync(dto as any)
      }
      onClose()
    } catch { /* handled */ }
  }

  const isLoading = createMutation.isPending || updateMutation.isPending

  return (
    <Modal open={open} onClose={onClose} title={isEdit ? 'Editar producto' : 'Nuevo producto'} size="lg">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div className="flex items-start gap-6">
          <ImageUploader value={imageUrl} onChange={setImageUrl} label="Imagen del producto" />
          <div className="flex-1 space-y-4">
            <Input label="Nombre del producto" placeholder="Nombre" leftIcon={<Tag size={16} />}
              error={errors.name?.message} {...register('name')} />
            <div className="grid grid-cols-3 gap-4">
              <Input label="SKU" placeholder="SKU-001" leftIcon={<Hash size={16} />}
                error={errors.sku?.message} {...register('sku')} />
              <Input label="Precio" type="number" step="0.01" placeholder="0.00" leftIcon={<DollarSign size={16} />}
                error={errors.price?.message} {...register('price')} />
              <Input label="Stock" type="number" placeholder="0" leftIcon={<Package size={16} />}
                error={errors.stockQuantity?.message} {...register('stockQuantity')} />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Categoría</label>
                <select {...register('categoryId')}
                  className="h-9 w-full rounded-lg border border-gray-300 bg-white px-3 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100">
                  <option value="">Sin categoría</option>
                  {(categoryOptions ?? []).map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </div>
              <Select label="Estado" options={statusOptions} error={errors.status?.message} {...register('status')} />
            </div>
            <Input label="Etiquetas" placeholder="nuevo, oferta, etc." {...register('tags')} />
          </div>
        </div>
        <div className="space-y-1.5">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Descripción</label>
          <textarea {...register('description')} rows={3}
            className="h-20 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
            placeholder="Descripción del producto..." />
        </div>
        <div className="flex justify-end gap-3 pt-4">
          <Button type="button" variant="outline" onClick={onClose}>Cancelar</Button>
          <Button type="submit" loading={isLoading}>
            {isEdit ? 'Guardar cambios' : 'Crear producto'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
