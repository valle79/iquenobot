import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Building2, Globe, Mail, Phone } from 'lucide-react'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { Input } from '@/shared/atoms/Input/Input'
import { Button } from '@/shared/atoms/Button/Button'
import { useUpdateTenant } from '../hooks/useAdminTenants'
import type { TenantDto } from '@/types/auth'

const schema = z.object({
  companyName: z.string().min(2, 'Mínimo 2 caracteres').max(200),
  contactEmail: z.string().email('Email inválido'),
  contactPhone: z.string().optional(),
  websiteUrl: z.string().optional(),
})

type FormData = z.infer<typeof schema>

interface EditTenantModalProps {
  open: boolean
  tenant: TenantDto | null
  onClose: () => void
}

export function EditTenantModal({ open, tenant, onClose }: EditTenantModalProps) {
  const updateTenant = useUpdateTenant()

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  useEffect(() => {
    if (tenant) {
      reset({
        companyName: tenant.companyName,
        contactEmail: tenant.contactEmail,
        contactPhone: tenant.contactPhone || '',
        websiteUrl: tenant.websiteUrl || '',
      })
    }
  }, [tenant, reset])

  const onSubmit = async (data: FormData) => {
    if (!tenant) return
    try {
      await updateTenant.mutateAsync({
        id: tenant.id,
        dto: {
          companyName: data.companyName,
          contactEmail: data.contactEmail,
          contactPhone: data.contactPhone || undefined,
          websiteUrl: data.websiteUrl || undefined,
        },
      })
      onClose()
    } catch {
      // handled by mutation
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Editar empresa" description={`Editando: ${tenant?.companyName ?? ''}`} size="lg">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div className="rounded-lg bg-gray-50 px-4 py-3 text-sm text-gray-600 dark:bg-gray-800 dark:text-gray-400">
          Subdominio: <strong>@{tenant?.subdomain}</strong> (no editable)
        </div>
        <Input label="Nombre de la empresa" leftIcon={<Building2 size={16} />}
          error={errors.companyName?.message} {...register('companyName')} />
        <Input label="Email de contacto" type="email" leftIcon={<Mail size={16} />}
          error={errors.contactEmail?.message} {...register('contactEmail')} />
        <Input label="Teléfono" leftIcon={<Phone size={16} />}
          error={errors.contactPhone?.message} {...register('contactPhone')} />
        <Input label="Sitio web" leftIcon={<Globe size={16} />}
          error={errors.websiteUrl?.message} {...register('websiteUrl')} />

        <div className="flex justify-end gap-3 pt-4">
          <Button type="button" variant="outline" onClick={onClose}>Cancelar</Button>
          <Button type="submit" loading={isSubmitting || updateTenant.isPending}>Guardar cambios</Button>
        </div>
      </form>
    </Modal>
  )
}
