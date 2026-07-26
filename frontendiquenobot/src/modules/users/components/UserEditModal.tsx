import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { Input } from '@/shared/atoms/Input/Input'
import { Select } from '@/shared/atoms/Select/Select'
import { Button } from '@/shared/atoms/Button/Button'
import { useUpdateUser } from '../hooks/useUsers'
import type { UserDto } from '@/types/auth'

const schema = z.object({
  firstName: z.string().min(2, 'Mínimo 2 caracteres').max(100),
  lastName: z.string().min(2, 'Mínimo 2 caracteres').max(100),
  phone: z.string().optional(),
  role: z.string().min(1, 'Selecciona un rol'),
  status: z.string().min(1, 'Selecciona un estado'),
})

type FormData = z.infer<typeof schema>

const roleOptions = [
  { value: 'TENANT_ADMIN', label: 'Admin' },
  { value: 'SUPERVISOR', label: 'Supervisor' },
  { value: 'AGENT', label: 'Agente' },
]

const statusOptions = [
  { value: 'ACTIVE', label: 'Activo' },
  { value: 'INACTIVE', label: 'Inactivo' },
  { value: 'LOCKED', label: 'Bloqueado' },
]

interface UserEditModalProps {
  open: boolean
  user: UserDto | null
  onClose: () => void
}

export function UserEditModal({ open, user, onClose }: UserEditModalProps) {
  const updateUser = useUpdateUser()

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  useEffect(() => {
    if (user) {
      reset({
        firstName: user.firstName,
        lastName: user.lastName,
        phone: user.phone || '',
        role: user.role,
        status: user.status,
      })
    }
  }, [user, reset])

  const onSubmit = async (data: FormData) => {
    if (!user) return
    try {
      await updateUser.mutateAsync({
        id: user.id,
        dto: {
          firstName: data.firstName,
          lastName: data.lastName,
          phone: data.phone || undefined,
          role: data.role as any,
          status: data.status as any,
        },
      })
      onClose()
    } catch {
      // Error handled by mutation
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Editar usuario" description={`Editando: ${user?.fullName ?? ''}`} size="lg">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <Input label="Nombre" error={errors.firstName?.message} {...register('firstName')} />
          <Input label="Apellido" error={errors.lastName?.message} {...register('lastName')} />
        </div>
        <Input label="Teléfono" error={errors.phone?.message} {...register('phone')} />
        <Select label="Rol" error={errors.role?.message} options={roleOptions} {...register('role')} />
        <Select label="Estado" error={errors.status?.message} options={statusOptions} {...register('status')} />

        <div className="flex justify-end gap-3 pt-4">
          <Button type="button" variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" loading={isSubmitting || updateUser.isPending}>
            Guardar cambios
          </Button>
        </div>
      </form>
    </Modal>
  )
}
