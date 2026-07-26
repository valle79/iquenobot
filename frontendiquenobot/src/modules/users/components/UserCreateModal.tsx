import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Building2 } from 'lucide-react'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { Input } from '@/shared/atoms/Input/Input'
import { Select } from '@/shared/atoms/Select/Select'
import { Button } from '@/shared/atoms/Button/Button'
import { useAuthStore } from '@/core/auth/auth.store'
import { useCreateUser } from '../hooks/useUsers'
import type { RoleType } from '@/types/enums'

const schema = z.object({
  email: z.string().email('Email inválido'),
  password: z.string().min(8, 'Mínimo 8 caracteres').regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*])/, 'Debe tener mayúscula, minúscula, número y carácter especial'),
  firstName: z.string().min(2, 'Mínimo 2 caracteres').max(100),
  lastName: z.string().min(2, 'Mínimo 2 caracteres').max(100),
  phone: z.string().optional(),
  role: z.string().min(1, 'Selecciona un rol'),
})

type FormData = z.infer<typeof schema>

const roleOptions = [
  { value: 'TENANT_ADMIN', label: 'Admin' },
  { value: 'SUPERVISOR', label: 'Supervisor' },
  { value: 'AGENT', label: 'Agente' },
]

interface UserCreateModalProps {
  open: boolean
  onClose: () => void
}

export function UserCreateModal({ open, onClose }: UserCreateModalProps) {
  const createUser = useCreateUser()
  const { tenant } = useAuthStore()

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  const onSubmit = async (data: FormData) => {
    try {
      await createUser.mutateAsync({
        email: data.email,
        password: data.password,
        firstName: data.firstName,
        lastName: data.lastName,
        phone: data.phone || undefined,
        role: data.role as RoleType,
      })
      reset()
      onClose()
    } catch {
      // Error handled by mutation
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Nuevo usuario" description="Crea un nuevo usuario en el sistema" size="lg">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {tenant && (
          <div className="flex items-center gap-2 rounded-lg bg-brand-50 px-4 py-3 text-sm text-brand-700 dark:bg-brand-900/20 dark:text-brand-400">
            <Building2 size={16} />
            <span>
              El usuario se creará en <strong>{tenant.companyName}</strong>
              <span className="ml-2 rounded-md bg-brand-100 px-2 py-0.5 text-xs dark:bg-brand-900/40">
                @{tenant.subdomain}
              </span>
            </span>
          </div>
        )}
        <div className="grid grid-cols-2 gap-4">
          <Input label="Nombre" error={errors.firstName?.message} {...register('firstName')} />
          <Input label="Apellido" error={errors.lastName?.message} {...register('lastName')} />
        </div>
        <Input label="Email" type="email" error={errors.email?.message} {...register('email')} />
        <Input label="Contraseña" type="password" error={errors.password?.message} {...register('password')} />
        <Input label="Teléfono (opcional)" error={errors.phone?.message} {...register('phone')} />
        <Select label="Rol" error={errors.role?.message} options={roleOptions} placeholder="Seleccionar rol" {...register('role')} />

        <div className="flex justify-end gap-3 pt-4">
          <Button type="button" variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" loading={isSubmitting || createUser.isPending}>
            Crear usuario
          </Button>
        </div>
      </form>
    </Modal>
  )
}
