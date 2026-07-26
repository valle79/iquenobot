import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { Input } from '@/shared/atoms/Input/Input'
import { Button } from '@/shared/atoms/Button/Button'
import { useChatbotIntentMutations } from '../hooks/useChatbotIntents'
import type { ChatbotIntentDto } from '@/types/chatbot'

const schema = z.object({
  intentName: z.string().min(1, 'Requerido').max(100),
  description: z.string().optional(),
  trainingPhrases: z.string().min(1, 'Requerido'),
  responses: z.string().min(1, 'Requerido'),
  entities: z.string().optional(),
  contextRequired: z.string().optional(),
  contextOutput: z.string().optional(),
  actions: z.string().optional(),
  confidenceThreshold: z.string().min(1, 'Requerido'),
  active: z.string(),
  priority: z.string().optional(),
})

type FormData = z.infer<typeof schema>

interface Props {
  open: boolean
  intent: ChatbotIntentDto | null
  onClose: () => void
}

export function IntentsFormModal({ open, intent, onClose }: Props) {
  const isEdit = !!intent
  const { createMutation, updateMutation } = useChatbotIntentMutations()

  const { register, handleSubmit, reset, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  useEffect(() => {
    if (intent) {
      reset({
        intentName: intent.intentName,
        description: intent.description || '',
        trainingPhrases: intent.trainingPhrases,
        responses: intent.responses,
        entities: intent.entities || '',
        contextRequired: intent.contextRequired || '',
        contextOutput: intent.contextOutput || '',
        actions: intent.actions || '',
        confidenceThreshold: String(intent.confidenceThreshold),
        active: String(intent.active),
        priority: String(intent.priority ?? 0),
      })
    } else {
      reset({
        intentName: '', description: '', trainingPhrases: '', responses: '',
        entities: '', contextRequired: '', contextOutput: '', actions: '',
        confidenceThreshold: '0.7', active: 'true', priority: '0',
      })
    }
  }, [intent, reset])

  const onSubmit = async (data: FormData) => {
    try {
      const dto = {
        intentName: data.intentName,
        description: data.description || undefined,
        trainingPhrases: data.trainingPhrases,
        responses: data.responses,
        entities: data.entities || undefined,
        contextRequired: data.contextRequired || undefined,
        contextOutput: data.contextOutput || undefined,
        actions: data.actions || undefined,
        confidenceThreshold: Number(data.confidenceThreshold),
        active: data.active === 'true',
        priority: data.priority ? Number(data.priority) : undefined,
      }
      if (isEdit && intent) {
        await updateMutation.mutateAsync({ id: intent.id, dto })
      } else {
        await createMutation.mutateAsync(dto)
      }
      onClose()
    } catch { /* handled */ }
  }

  const isLoading = createMutation.isPending || updateMutation.isPending

  return (
    <Modal open={open} onClose={onClose} title={isEdit ? 'Editar intención' : 'Nueva intención'} size="lg">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input label="Nombre de la intención" placeholder="saludo.agradecimiento" {...register('intentName')} error={errors.intentName?.message} />
        <div className="space-y-1.5">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Descripción</label>
          <textarea {...register('description')} rows={2}
            className="h-16 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
            placeholder="Descripción opcional..." />
        </div>
        <div className="space-y-1.5">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Frases de entrenamiento <span className="text-red-500">*</span></label>
          <textarea {...register('trainingPhrases')} rows={3}
            className="h-20 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
            placeholder="Una frase por línea" />
          {errors.trainingPhrases && <p className="text-xs text-red-500">{errors.trainingPhrases.message}</p>}
        </div>
        <div className="space-y-1.5">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Respuestas <span className="text-red-500">*</span></label>
          <textarea {...register('responses')} rows={3}
            className="h-20 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
            placeholder="Una respuesta por línea" />
          {errors.responses && <p className="text-xs text-red-500">{errors.responses.message}</p>}
        </div>
        <div className="grid grid-cols-3 gap-4">
          <Input label="Umbral de confianza" type="number" step="0.05" placeholder="0.7" {...register('confidenceThreshold')} error={errors.confidenceThreshold?.message} />
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
        <Input label="Entidades (JSON)" placeholder='{"origen": "web"}' {...register('entities')} />
        <Input label="Contexto requerido" placeholder="contexto_anterior" {...register('contextRequired')} />
        <Input label="Contexto de salida" placeholder="contexto_siguiente" {...register('contextOutput')} />
        <Input label="Acciones (JSON)" placeholder='{"redirect": "flow_x"}' {...register('actions')} />
        <div className="flex justify-end gap-3 pt-4">
          <Button type="button" variant="outline" onClick={onClose}>Cancelar</Button>
          <Button type="submit" loading={isLoading}>{isEdit ? 'Guardar cambios' : 'Crear intención'}</Button>
        </div>
      </form>
    </Modal>
  )
}
