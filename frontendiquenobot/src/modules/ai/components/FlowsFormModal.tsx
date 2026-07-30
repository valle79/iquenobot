import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { Input } from '@/shared/atoms/Input/Input'
import { Toggle } from '@/shared/atoms/Toggle/Toggle'
import { Button } from '@/shared/atoms/Button/Button'
import { useChatbotFlowMutations } from '../hooks/useChatbotFlows'
import type { ChatbotFlowDto } from '@/types/chatbot'

const schema = z.object({
  name: z.string().min(1, 'Requerido').max(100),
  description: z.string().optional(),
  triggerType: z.string().min(1, 'Requerido'),
  triggerKeywords: z.string().optional(),
  message: z.string().min(1, 'Escribe el mensaje que enviará el bot'),
  fallbackMessage: z.string().optional(),
  active: z.boolean(),
})

type FormData = z.infer<typeof schema>

const triggerOptions: { value: string; label: string; hint: string }[] = [
  { value: 'KEYWORD', label: 'Palabra clave', hint: 'Se activa cuando el cliente escribe cierta palabra' },
  { value: 'WELCOME', label: 'Bienvenida', hint: 'Se activa cuando el cliente inicia una conversación' },
  { value: 'INTENT', label: 'Tras detectar una intención', hint: 'Se activa después de que el bot reconoce el tema' },
]

interface Props {
  open: boolean
  flow: ChatbotFlowDto | null
  onClose: () => void
}

function extractMessage(flowConfig: string | undefined | null): string {
  if (!flowConfig) return ''
  try {
    const parsed = JSON.parse(flowConfig)
    return parsed.message || ''
  } catch {
    return ''
  }
}

function buildFlowConfig(message: string, fallbackMessage?: string): string {
  return JSON.stringify({ message, fallbackMessage: fallbackMessage || '' })
}

export function FlowsFormModal({ open, flow, onClose }: Props) {
  const isEdit = !!flow
  const { createMutation, updateMutation } = useChatbotFlowMutations()

  const { register, handleSubmit, reset, watch, setValue, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', description: '', triggerType: 'KEYWORD', triggerKeywords: '', message: '', fallbackMessage: '', active: true },
  })

  const triggerType = watch('triggerType')
  const active = watch('active')

  useEffect(() => {
    if (flow) {
      reset({
        name: flow.name,
        description: flow.description || '',
        triggerType: flow.triggerType,
        triggerKeywords: flow.triggerKeywords || '',
        message: extractMessage(flow.flowConfig),
        fallbackMessage: flow.fallbackMessage || '',
        active: flow.active,
      })
    }
  }, [flow, reset])

  const onSubmit = async (data: FormData) => {
    try {
      const dto = {
        name: data.name,
        description: data.description || undefined,
        triggerType: data.triggerType,
        triggerKeywords: data.triggerType === 'KEYWORD' ? data.triggerKeywords : undefined,
        flowConfig: buildFlowConfig(data.message, data.fallbackMessage),
        active: data.active,
        priority: 0,
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
    <Modal open={open} onClose={onClose} title={isEdit ? 'Editar conversación guiada' : 'Nueva conversación guiada'} size="lg">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">

        <Input label="Nombre" placeholder="ej: bienvenida, consulta_precio, agradecimiento" {...register('name')} error={errors.name?.message} />
        <div className="space-y-1.5">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Descripción (opcional)</label>
          <textarea {...register('description')} rows={1}
            className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
            placeholder="¿Cuándo se usa esta conversación?" />
        </div>

        <div className="space-y-1.5">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">¿Cuándo se activa?</label>
          <select {...register('triggerType')}
            className="h-9 w-full rounded-lg border border-gray-300 bg-white px-3 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100">
            {triggerOptions.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
          <p className="text-xs text-gray-400">{triggerOptions.find(o => o.value === triggerType)?.hint}</p>
        </div>

        {triggerType === 'KEYWORD' && (
          <div className="space-y-1.5">
            <Input label="Palabras clave" placeholder="ej: precio, costo, cuánto vale" {...register('triggerKeywords')} />
            <p className="text-xs text-gray-400">Separa con comas. El bot se activará cuando el cliente escriba alguna de estas palabras.</p>
          </div>
        )}

        <div className="space-y-1.5">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Mensaje que envía el bot</label>
          <textarea {...register('message')} rows={3}
            className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
            placeholder={"¡Hola! ¿En qué puedo ayudarte?\n\nOpciones:\n1. Consultar precio\n2. Hablar con un asesor"} />
          {errors.message && <p className="text-xs text-red-500">{errors.message.message}</p>}
        </div>

        <Input label="Mensaje si no entiende (opcional)" placeholder="ej: Lo siento, no entendí. ¿Puedes repetirlo?" {...register('fallbackMessage')} />

        <div className="flex items-center justify-between rounded-lg border border-gray-200 bg-gray-50 px-3 py-2.5 dark:border-gray-700 dark:bg-gray-800/50">
          <div>
            <p className="text-sm font-medium text-gray-900 dark:text-gray-100">Activo</p>
            <p className="text-xs text-gray-500">Si lo desactivas, el bot ignorará esta conversación</p>
          </div>
          <Toggle checked={active} onChange={(v) => setValue('active', v)} size="sm" />
        </div>

        <div className="flex justify-end gap-3 pt-1">
          <Button type="button" variant="outline" onClick={onClose}>Cancelar</Button>
          <Button type="submit" loading={isLoading}>{isEdit ? 'Guardar cambios' : 'Crear conversación'}</Button>
        </div>
      </form>
    </Modal>
  )
}
