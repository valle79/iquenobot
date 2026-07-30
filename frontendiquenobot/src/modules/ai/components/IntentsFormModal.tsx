import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { MessageCircle, Brain } from 'lucide-react'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { Input } from '@/shared/atoms/Input/Input'
import { Toggle } from '@/shared/atoms/Toggle/Toggle'
import { Button } from '@/shared/atoms/Button/Button'
import { useChatbotIntentMutations } from '../hooks/useChatbotIntents'
import type { ChatbotIntentDto } from '@/types/chatbot'

const schema = z.object({
  intentName: z.string().min(1, 'Requerido').max(100),
  description: z.string().optional(),
  trainingPhrases: z.string().min(1, 'Agrega al menos una frase'),
  responses: z.string().min(1, 'Agrega al menos una respuesta'),
  active: z.boolean(),
})

type FormData = z.infer<typeof schema>

interface Props {
  open: boolean
  intent: ChatbotIntentDto | null
  onClose: () => void
}

function parseLines(text: string): string[] {
  return text.split('\n').map(l => l.trim()).filter(l => l.length > 0)
}

function toLines(json: string | undefined | null): string {
  if (!json) return ''
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr.join('\n') : ''
  } catch {
    return json
  }
}

function toJSONArray(lines: string): string {
  return JSON.stringify(parseLines(lines))
}

function Section({ icon: Icon, iconColor, label, children }: {
  icon: typeof Brain
  iconColor?: string
  label: string
  children: React.ReactNode
}) {
  return (
    <div className="space-y-3">
      <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
        <Icon size={13} className={iconColor ?? 'text-brand-500'} />
        {label}
      </div>
      {children}
    </div>
  )
}

export function IntentsFormModal({ open, intent, onClose }: Props) {
  const isEdit = !!intent
  const { createMutation, updateMutation } = useChatbotIntentMutations()

  const { register, handleSubmit, reset, watch, setValue, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { intentName: '', description: '', trainingPhrases: '', responses: '', active: true },
  })
  const active = watch('active')

  useEffect(() => {
    if (intent) {
      reset({
        intentName: intent.intentName,
        description: intent.description || '',
        trainingPhrases: toLines(intent.trainingPhrases),
        responses: toLines(intent.responses),
        active: intent.active,
      })
    }
  }, [intent, reset])

  const onSubmit = async (data: FormData) => {
    try {
      const dto = {
        intentName: data.intentName,
        description: data.description || undefined,
        trainingPhrases: toJSONArray(data.trainingPhrases),
        responses: toJSONArray(data.responses),
        confidenceThreshold: 0.7,
        active: data.active,
        priority: 0,
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
    <Modal open={open} onClose={onClose} title={isEdit ? 'Editar auto-respuesta' : 'Nueva auto-respuesta'} size="lg">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">

        <Section icon={Brain} label="Identificación">
          <Input label="Nombre" placeholder="ej: saludo, consulta_horario, precio" {...register('intentName')} error={errors.intentName?.message} />
          <div className="space-y-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Descripción (opcional)</label>
            <textarea {...register('description')} rows={1}
              className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
              placeholder="¿Para qué sirve esta auto-respuesta?" />
          </div>
        </Section>

        <Section icon={MessageCircle} label="¿Qué escribe el cliente?">
          <div className="space-y-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
              Frases que activan esta respuesta</label>
            <textarea {...register('trainingPhrases')} rows={4}
              className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
              placeholder={"Hola\nBuenos días\nQué tal\nHey"} />
            {errors.trainingPhrases && <p className="text-xs text-red-500">{errors.trainingPhrases.message}</p>}
            <p className="text-xs text-gray-400">Una frase por línea. El bot detectará automáticamente cuando el cliente escriba algo similar.</p>
          </div>
        </Section>

        <Section icon={MessageCircle} label="¿Qué responde el bot?">
          <div className="space-y-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
              Mensaje que envía el bot automáticamente</label>
            <textarea {...register('responses')} rows={3}
              className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
              placeholder={"¡Hola! ¿En qué puedo ayudarte?\n¡Hola! Bienvenido, ¿cómo te puedo asistir?"} />
            {errors.responses && <p className="text-xs text-red-500">{errors.responses.message}</p>}
            <p className="text-xs text-gray-400">Si pones varias respuestas, el bot elegirá una al azar.</p>
          </div>
        </Section>

        <div className="flex items-center justify-between rounded-lg border border-gray-200 bg-gray-50 px-3 py-2.5 dark:border-gray-700 dark:bg-gray-800/50">
          <div>
            <p className="text-sm font-medium text-gray-900 dark:text-gray-100">Activo</p>
            <p className="text-xs text-gray-500">Si lo desactivas, el bot ignorará esta auto-respuesta</p>
          </div>
          <Toggle checked={active} onChange={(v) => setValue('active', v)} size="sm" />
        </div>

        <div className="flex justify-end gap-3 pt-1">
          <Button type="button" variant="outline" onClick={onClose}>Cancelar</Button>
          <Button type="submit" loading={isLoading}>{isEdit ? 'Guardar cambios' : 'Crear auto-respuesta'}</Button>
        </div>
      </form>
    </Modal>
  )
}
