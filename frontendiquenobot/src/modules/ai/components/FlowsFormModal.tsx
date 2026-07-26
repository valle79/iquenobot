import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { Input } from '@/shared/atoms/Input/Input'
import { Button } from '@/shared/atoms/Button/Button'
import { useChatbotFlowMutations } from '../hooks/useChatbotFlows'
import type { ChatbotFlowDto, ChatbotFlowTrigger } from '@/types/chatbot'

const schema = z.object({
  name: z.string().min(1, 'Requerido').max(100),
  description: z.string().optional(),
  triggerType: z.string().min(1, 'Requerido'),
  triggerKeywords: z.string().optional(),
  triggerPattern: z.string().optional(),
  flowConfig: z.string().min(1, 'Requerido'),
  priority: z.string().optional(),
  active: z.string(),
  useAI: z.string(),
  aiPrompt: z.string().optional(),
  fallbackMessage: z.string().optional(),
})

type FormData = z.infer<typeof schema>

const triggerOptions: { value: string; label: string }[] = [
  { value: 'KEYWORD', label: 'Palabra clave' },
  { value: 'PATTERN', label: 'Patrón' },
  { value: 'AI', label: 'IA' },
  { value: 'SCHEDULED', label: 'Programado' },
  { value: 'EVENT', label: 'Evento' },
]

interface Props {
  open: boolean
  flow: ChatbotFlowDto | null
  onClose: () => void
}

export function FlowsFormModal({ open, flow, onClose }: Props) {
  const isEdit = !!flow
  const { createMutation, updateMutation } = useChatbotFlowMutations()

  const { register, handleSubmit, reset, watch, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  const triggerType = watch('triggerType')

  useEffect(() => {
    if (flow) {
      reset({
        name: flow.name,
        description: flow.description || '',
        triggerType: flow.triggerType,
        triggerKeywords: flow.triggerKeywords || '',
        triggerPattern: flow.triggerPattern || '',
        flowConfig: flow.flowConfig,
        priority: String(flow.priority ?? 0),
        active: String(flow.active),
        useAI: String(flow.useAI),
        aiPrompt: flow.aiPrompt || '',
        fallbackMessage: flow.fallbackMessage || '',
      })
    } else {
      reset({
        name: '', description: '', triggerType: 'KEYWORD', triggerKeywords: '', triggerPattern: '',
        flowConfig: '{"messages": []}', priority: '0', active: 'true', useAI: 'false',
        aiPrompt: '', fallbackMessage: '',
      })
    }
  }, [flow, reset])

  const onSubmit = async (data: FormData) => {
    try {
      const dto = {
        name: data.name,
        description: data.description || undefined,
        triggerType: data.triggerType as ChatbotFlowTrigger,
        triggerKeywords: data.triggerKeywords || undefined,
        triggerPattern: data.triggerPattern || undefined,
        flowConfig: data.flowConfig,
        priority: data.priority ? Number(data.priority) : undefined,
        active: data.active === 'true',
        useAI: data.useAI === 'true',
        aiPrompt: data.aiPrompt || undefined,
        fallbackMessage: data.fallbackMessage || undefined,
      }
      if (isEdit && flow) {
        await updateMutation.mutateAsync({ id: flow.id, dto })
      } else {
        await createMutation.mutateAsync(dto)
      }
      onClose()
    } catch { /* handled */ }
  }

  const isLoading = createMutation.isPending || updateMutation.isPending

  return (
    <Modal open={open} onClose={onClose} title={isEdit ? 'Editar flujo' : 'Nuevo flujo'} size="lg">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input label="Nombre del flujo" placeholder="Flujo de bienvenida" {...register('name')} error={errors.name?.message} />
        <div className="space-y-1.5">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Descripción</label>
          <textarea {...register('description')} rows={2}
            className="h-16 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
            placeholder="Descripción opcional..." />
        </div>
        <div className="grid grid-cols-3 gap-4">
          <div className="space-y-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Tipo de disparador</label>
            <select {...register('triggerType')}
              className="h-9 w-full rounded-lg border border-gray-300 bg-white px-3 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100">
              {triggerOptions.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </div>
          <Input label="Prioridad" type="number" placeholder="0" {...register('priority')} />
          <div className="space-y-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Activo</label>
            <select {...register('active')}
              className="h-9 w-full rounded-lg border border-gray-300 bg-white px-3 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100">
              <option value="true">Sí</option>
              <option value="false">No</option>
            </select>
          </div>
        </div>
        {triggerType === 'KEYWORD' && (
          <Input label="Palabras clave" placeholder="hola, buenos días, saludos" {...register('triggerKeywords')} />
        )}
        {triggerType === 'PATTERN' && (
          <Input label="Patrón (regex)" placeholder="hola|buenos (días|tardes)" {...register('triggerPattern')} />
        )}
        <div className="space-y-1.5">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Configuración del flujo (JSON) <span className="text-red-500">*</span></label>
          <textarea {...register('flowConfig')} rows={4}
            className="h-24 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-mono placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
            placeholder='{"messages": [{"type": "text", "content": "Hola"}]}' />
          {errors.flowConfig && <p className="text-xs text-red-500">{errors.flowConfig.message}</p>}
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Usar IA</label>
            <select {...register('useAI')}
              className="h-9 w-full rounded-lg border border-gray-300 bg-white px-3 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100">
              <option value="false">No</option>
              <option value="true">Sí</option>
            </select>
          </div>
          <Input label="Prompt de IA" placeholder="Eres un asistente..." {...register('aiPrompt')} />
        </div>
        <Input label="Mensaje de fallback" placeholder="Lo siento, no entendí..." {...register('fallbackMessage')} />
        <div className="flex justify-end gap-3 pt-4">
          <Button type="button" variant="outline" onClick={onClose}>Cancelar</Button>
          <Button type="submit" loading={isLoading}>{isEdit ? 'Guardar cambios' : 'Crear flujo'}</Button>
        </div>
      </form>
    </Modal>
  )
}
