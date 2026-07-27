import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Info, ChevronDown, ChevronUp, Sparkles, MessageCircle, Brain } from 'lucide-react'
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
  confidenceThreshold: z.number().min(0).max(1),
  priority: z.number().min(0),
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

function Tip({ text }: { text: string }) {
  return (
    <span className="group relative inline-flex ml-1">
      <Info size={13} className="text-gray-400 hover:text-gray-600 cursor-help" />
      <span className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-52 rounded-lg bg-gray-900 px-3 py-2 text-xs text-white opacity-0 shadow-lg transition-opacity group-hover:opacity-100 pointer-events-none z-50">
        {text}
      </span>
    </span>
  )
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
  const [showAdvanced, setShowAdvanced] = useState(false)

  const { register, handleSubmit, reset, watch, setValue, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { confidenceThreshold: 0.7, priority: 0, active: true },
  })

  const confidenceThreshold = watch('confidenceThreshold')
  const active = watch('active')

  useEffect(() => {
    if (intent) {
      reset({
        intentName: intent.intentName,
        description: intent.description || '',
        trainingPhrases: toLines(intent.trainingPhrases),
        responses: toLines(intent.responses),
        confidenceThreshold: intent.confidenceThreshold,
        priority: intent.priority ?? 0,
        active: intent.active,
      })
    } else {
      reset({ intentName: '', description: '', trainingPhrases: '', responses: '', confidenceThreshold: 0.7, priority: 0, active: true })
    }
    setShowAdvanced(false)
  }, [intent, reset])

  const onSubmit = async (data: FormData) => {
    try {
      const dto = {
        intentName: data.intentName,
        description: data.description || undefined,
        trainingPhrases: toJSONArray(data.trainingPhrases),
        responses: toJSONArray(data.responses),
        confidenceThreshold: data.confidenceThreshold,
        active: data.active,
        priority: data.priority,
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
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">

        <Section icon={Brain} label="Identificación">
          <Input label="Nombre de la intención" placeholder="saludo, consulta_precio, despedida..." {...register('intentName')} error={errors.intentName?.message} />
          <div className="space-y-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Descripción</label>
            <textarea {...register('description')} rows={1}
              className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
              placeholder="¿Qué detecta esta intención? (opcional)" />
          </div>
        </Section>

        <Section icon={MessageCircle} label="Conversación">
          <div className="space-y-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
              ¿Qué dice el cliente?<Tip text="Frases que los clientes escriben para activar esta intención. Mientras más frases, mejor entiende el bot." /></label>
            <textarea {...register('trainingPhrases')} rows={4}
              className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
              placeholder={"Hola\nBuenos días\nQué tal\nHey"} />
            {errors.trainingPhrases && <p className="text-xs text-red-500">{errors.trainingPhrases.message}</p>}
            <p className="text-xs text-gray-400">Una frase por línea</p>
          </div>

          <div className="space-y-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
              ¿Qué responde el bot?<Tip text="Respuestas que el bot enviará cuando detecte esta intención. Se elige una al azar." /></label>
            <textarea {...register('responses')} rows={3}
              className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
              placeholder={"¡Hola! ¿En qué puedo ayudarte?\n¡Hola! Bienvenido, ¿cómo te puedo asistir?"} />
            {errors.responses && <p className="text-xs text-red-500">{errors.responses.message}</p>}
            <p className="text-xs text-gray-400">Una respuesta por línea. Se elige una al azar.</p>
          </div>
        </Section>

        <Section icon={Sparkles} label="Configuración">
          <div className="grid grid-cols-3 gap-3">
            <div className="col-span-2 space-y-1.5">
              <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
                Umbral de confianza<Tip text="Qué tan segura debe estar el bot. 70% es recomendado. Si bajás, responde a más cosas pero puede equivocarse." /></label>
              <div className="flex items-center gap-3">
                <input type="range" min={0.3} max={1} step={0.05} value={confidenceThreshold}
                  onChange={(e) => setValue('confidenceThreshold', Number(e.target.value), { shouldValidate: true })}
                  className="flex-1 h-1.5 bg-gray-200 rounded-lg appearance-none cursor-pointer dark:bg-gray-700 accent-brand-500" />
                <span className="w-10 text-center text-xs font-bold text-brand-600 dark:text-brand-400">{Math.round(confidenceThreshold * 100)}%</span>
              </div>
            </div>
            <Input label="Prioridad" type="number" placeholder="0" {...register('priority', { valueAsNumber: true })} />
          </div>

          <div className="flex items-center justify-between rounded-lg border border-gray-200 bg-gray-50 px-3 py-2.5 dark:border-gray-700 dark:bg-gray-800/50">
            <div>
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">Activo</p>
              <p className="text-xs text-gray-500">Si está desactivado, el bot no usará esta intención</p>
            </div>
            <Toggle checked={active} onChange={(v) => setValue('active', v)} size="sm" />
          </div>
        </Section>

        <button type="button" onClick={() => setShowAdvanced(!showAdvanced)}
          className="flex w-full items-center justify-between rounded-lg border border-dashed border-gray-300 px-3 py-2 text-xs font-medium text-gray-500 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-400 dark:hover:bg-gray-800/50">
          Opciones avanzadas
          {showAdvanced ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
        </button>

        {showAdvanced && (
          <div className="grid grid-cols-2 gap-3">
            <Input label="Contexto requerido" placeholder="contexto_anterior" {...register('contextRequired')} />
            <Input label="Contexto de salida" placeholder="contexto_siguiente" {...register('contextOutput')} />
          </div>
        )}

        <div className="flex justify-end gap-3 pt-1">
          <Button type="button" variant="outline" onClick={onClose}>Cancelar</Button>
          <Button type="submit" loading={isLoading}>{isEdit ? 'Guardar cambios' : 'Crear intención'}</Button>
        </div>
      </form>
    </Modal>
  )
}
