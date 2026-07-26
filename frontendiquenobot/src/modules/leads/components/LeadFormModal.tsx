import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Target, DollarSign, Percent, User as UserIcon, Globe, Tag } from 'lucide-react'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { Input } from '@/shared/atoms/Input/Input'
import { Select } from '@/shared/atoms/Select/Select'
import { Button } from '@/shared/atoms/Button/Button'
import { useCreateLead, useUpdateLead } from '../hooks/useLeads'
import type { LeadDto } from '@/types/contact'
import type { LeadStatus, LeadSource } from '@/types/enums'

const schema = z.object({
  title: z.string().min(1, 'Requerido').max(200),
  description: z.string().optional(),
  status: z.string().min(1, 'Requerido'),
  source: z.string().min(1, 'Requerido'),
  estimatedValue: z.string().optional(),
  probability: z.string().optional(),
  tags: z.string().optional(),
})

type FormData = z.infer<typeof schema>

const statusOptions = [
  { value: 'NEW', label: 'Nuevo' },
  { value: 'CONTACTED', label: 'Contactado' },
  { value: 'QUALIFIED', label: 'Calificado' },
  { value: 'CONVERTED', label: 'Convertido' },
  { value: 'LOST', label: 'Perdido' },
  { value: 'DISQUALIFIED', label: 'Descalificado' },
]

const sourceOptions = [
  { value: 'WHATSAPP', label: 'WhatsApp' },
  { value: 'WEB_FORM', label: 'Formulario web' },
  { value: 'PHONE', label: 'Teléfono' },
  { value: 'EMAIL', label: 'Email' },
  { value: 'SOCIAL_MEDIA', label: 'Redes sociales' },
  { value: 'REFERRAL', label: 'Referido' },
  { value: 'ADVERTISING', label: 'Publicidad' },
  { value: 'EVENT', label: 'Evento' },
  { value: 'DIRECT', label: 'Directo' },
  { value: 'OTHER', label: 'Otro' },
]

interface LeadFormModalProps {
  open: boolean
  lead: LeadDto | null
  onClose: () => void
}

export function LeadFormModal({ open, lead, onClose }: LeadFormModalProps) {
  const isEdit = !!lead
  const createMutation = useCreateLead()
  const updateMutation = useUpdateLead()

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) })

  useEffect(() => {
    if (lead) {
      reset({
        title: lead.title,
        description: lead.description || '',
        status: lead.status,
        source: lead.source,
        estimatedValue: String(lead.estimatedValue ?? ''),
        probability: String(lead.probability ?? ''),
        tags: lead.tags || '',
      })
    } else {
      reset({ title: '', description: '', status: 'NEW', source: 'OTHER', estimatedValue: '', probability: '', tags: '' })
    }
  }, [lead, reset])

  const onSubmit = async (data: FormData) => {
    try {
      const dto: any = {
        title: data.title,
        description: data.description || undefined,
        status: data.status as LeadStatus,
        source: data.source as LeadSource,
        estimatedValue: data.estimatedValue ? Number(data.estimatedValue) : undefined,
        probability: data.probability ? Number(data.probability) : undefined,
        tags: data.tags || undefined,
      }
      if (isEdit && lead) {
        await updateMutation.mutateAsync({ id: lead.id, dto })
      } else {
        await createMutation.mutateAsync(dto)
      }
      onClose()
    } catch { /* handled */ }
  }

  const isLoading = createMutation.isPending || updateMutation.isPending

  return (
    <Modal open={open} onClose={onClose} title={isEdit ? 'Editar lead' : 'Nuevo lead'} size="lg">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input label="Título" placeholder="Ej: Seguimiento cliente potencial" leftIcon={<Target size={16} />}
          error={errors.title?.message} {...register('title')} />
        <div className="grid grid-cols-2 gap-4">
          <Select label="Estado" options={statusOptions} error={errors.status?.message} {...register('status')} />
          <Select label="Fuente" options={sourceOptions} error={errors.source?.message} {...register('source')} />
        </div>
        <div className="grid grid-cols-2 gap-4">
          <Input label="Valor estimado" type="number" step="0.01" placeholder="0.00" leftIcon={<DollarSign size={16} />}
            error={errors.estimatedValue?.message} {...register('estimatedValue')} />
          <Input label="Probabilidad (%)" type="number" min="0" max="100" placeholder="50" leftIcon={<Percent size={16} />}
            error={errors.probability?.message} {...register('probability')} />
        </div>
        <Input label="Etiquetas" placeholder="cliente-potencial, follow-up, etc." leftIcon={<Tag size={16} />}
          error={errors.tags?.message} {...register('tags')} />
        <div className="space-y-1.5">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Descripción</label>
          <textarea {...register('description')} rows={3}
            className="h-20 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
            placeholder="Notas sobre el lead..." />
        </div>
        <div className="flex justify-end gap-3 pt-4">
          <Button type="button" variant="outline" onClick={onClose}>Cancelar</Button>
          <Button type="submit" loading={isLoading}>
            {isEdit ? 'Guardar cambios' : 'Crear lead'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
