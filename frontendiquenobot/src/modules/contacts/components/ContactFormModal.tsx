import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { User, Mail, Phone, Building2, Briefcase, Globe, Tag, CreditCard, MapPin } from 'lucide-react'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { Input } from '@/shared/atoms/Input/Input'
import { Select } from '@/shared/atoms/Select/Select'
import { Button } from '@/shared/atoms/Button/Button'
import { useCreateContact, useUpdateContact } from '../hooks/useContacts'
import type { ContactDto } from '@/types/contact'

const schema = z.object({
  firstName: z.string().min(1, 'Requerido').max(100),
  lastName: z.string().min(1, 'Requerido').max(100),
  email: z.string().email('Email inválido').optional().or(z.literal('')),
  phone: z.string().optional(),
  company: z.string().optional(),
  documentType: z.enum(['', 'DNI', 'RUC']).optional(),
  documentNumber: z
    .string()
    .optional()
    .refine((v) => !v || /^\d{8}$|^\d{11}$/.test(v), '8 dígitos (DNI) u 11 (RUC)'),
  address: z.string().max(255).optional(),
  jobTitle: z.string().optional(),
  notes: z.string().optional(),
  tags: z.string().optional(),
})

type FormData = z.infer<typeof schema>

interface ContactFormModalProps {
  open: boolean
  contact: ContactDto | null
  onClose: () => void
}

export function ContactFormModal({ open, contact, onClose }: ContactFormModalProps) {
  const isEdit = !!contact
  const createMutation = useCreateContact()
  const updateMutation = useUpdateContact()

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) })

  useEffect(() => {
    if (contact) {
      reset({
        firstName: contact.firstName,
        lastName: contact.lastName,
        email: contact.email || '',
        phone: contact.phone || '',
        company: contact.company || '',
        documentType: contact.documentType || '',
        documentNumber: contact.documentNumber || '',
        address: contact.address || '',
        jobTitle: contact.jobTitle || '',
        notes: contact.notes || '',
        tags: contact.tags || '',
      })
    } else {
      reset({ firstName: '', lastName: '', email: '', phone: '', company: '', documentType: '', documentNumber: '', address: '', jobTitle: '', notes: '', tags: '' })
    }
  }, [contact, reset])

  const onSubmit = async (data: FormData) => {
    try {
      if (isEdit && contact) {
        await updateMutation.mutateAsync({ id: contact.id, dto: data })
      } else {
        await createMutation.mutateAsync(data)
      }
      onClose()
    } catch { /* handled */ }
  }

  const isLoading = createMutation.isPending || updateMutation.isPending

  return (
    <Modal open={open} onClose={onClose} title={isEdit ? 'Editar contacto' : 'Nuevo contacto'} size="lg">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <Input label="Nombre" placeholder="Nombre" leftIcon={<User size={16} />}
            error={errors.firstName?.message} {...register('firstName')} />
          <Input label="Apellido" placeholder="Apellido" leftIcon={<User size={16} />}
            error={errors.lastName?.message} {...register('lastName')} />
        </div>
        <Input label="Email" type="email" placeholder="correo@ejemplo.com" leftIcon={<Mail size={16} />}
          error={errors.email?.message} {...register('email')} />
        <Input label="Teléfono" placeholder="+573001234567" leftIcon={<Phone size={16} />}
          error={errors.phone?.message} {...register('phone')} />
        <div className="grid grid-cols-2 gap-4">
          <Input label="Empresa" placeholder="Empresa" leftIcon={<Building2 size={16} />}
            error={errors.company?.message} {...register('company')} />
          <Input label="Cargo" placeholder="Cargo" leftIcon={<Briefcase size={16} />}
            error={errors.jobTitle?.message} {...register('jobTitle')} />
        </div>
        <div className="grid grid-cols-2 gap-4">
          <Select label="Tipo de documento" options={[
            { value: '', label: 'Seleccionar' },
            { value: 'DNI', label: 'DNI' },
            { value: 'RUC', label: 'RUC' },
          ]}
            error={errors.documentType?.message} {...register('documentType')} />
          <Input label="Número de documento" placeholder="12345678" leftIcon={<CreditCard size={16} />}
            error={errors.documentNumber?.message} {...register('documentNumber')} />
        </div>
        <Input label="Dirección" placeholder="Av. Los Olivos 123, Lima" leftIcon={<MapPin size={16} />}
          error={errors.address?.message} {...register('address')} />
        <Input label="Etiquetas" placeholder="vip, cliente, etc." leftIcon={<Tag size={16} />}
          error={errors.tags?.message} {...register('tags')} />
        <div className="space-y-1.5">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Notas</label>
          <textarea {...register('notes')} rows={3}
            className="h-20 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
            placeholder="Notas adicionales..." />
        </div>
        <div className="flex justify-end gap-3 pt-4">
          <Button type="button" variant="outline" onClick={onClose}>Cancelar</Button>
          <Button type="submit" loading={isLoading}>
            {isEdit ? 'Guardar cambios' : 'Crear contacto'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
