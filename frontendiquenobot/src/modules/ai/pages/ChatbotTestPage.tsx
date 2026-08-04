import { FlaskConical, RotateCcw, Info } from 'lucide-react'
import { useAuthStore } from '@/core/auth/auth.store'
import { useChatbotTest } from '@/modules/ai/hooks/useChatbotTest'
import { TestChatMessages } from '@/modules/ai/components/test-chat/TestChatMessages'
import { TestChatInput } from '@/modules/ai/components/test-chat/TestChatInput'
import { TestSuggestionChips } from '@/modules/ai/components/test-chat/TestSuggestionChips'

export default function ChatbotTestPage() {
  const { tenant } = useAuthStore()
  const { messages, isTyping, isPending, sendMessage, reset } = useChatbotTest()

  const botName = tenant?.companyName ? `Bot de ${tenant.companyName}` : 'Bot'

  return (
    <div className="mx-auto max-w-3xl space-y-5">
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-100 text-amber-600 dark:bg-amber-950 dark:text-amber-400">
            <FlaskConical size={20} />
          </span>
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Probar Chatbot</h1>
            <p className="mt-0.5 text-sm text-gray-500">
              Simula una conversación real con tu bot para verificar su comportamiento.
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={reset}
          className="flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm font-medium text-gray-600 transition hover:bg-gray-50 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-300 dark:hover:bg-gray-800"
        >
          <RotateCcw size={15} />
          Reiniciar
        </button>
      </div>

      <div className="flex items-start gap-2.5 rounded-xl border border-blue-200 bg-blue-50/70 px-4 py-3 text-sm text-blue-800 dark:border-blue-900/60 dark:bg-blue-950/40 dark:text-blue-300">
        <Info size={17} className="mt-0.5 shrink-0" />
        <p>
          <strong>Modo de prueba:</strong> el bot responde exactamente igual que en producción (usa tus
          auto-respuestas, flujos, conocimientos y configuración actuales), pero{' '}
          <strong>nada se guarda</strong>: no se crean contactos, leads, cotizaciones ni mensajes en tu CRM.
        </p>
      </div>

      <div className="mx-auto flex h-[70vh] max-h-[680px] w-full max-w-md flex-col overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-xl shadow-gray-200/50 dark:border-gray-700 dark:bg-gray-900 dark:shadow-black/30">
        <div className="flex items-center gap-3 border-b border-gray-200 bg-gray-50 px-4 py-3 dark:border-gray-700 dark:bg-gray-800">
          <span className="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-brand-500 to-brand-700 text-white shadow-sm">
            <FlaskConical size={16} />
          </span>
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-semibold text-gray-900 dark:text-gray-100">{botName}</p>
            <p className="flex items-center gap-1 text-[11px] text-gray-500 dark:text-gray-400">
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
              En línea · responde en tiempo real
            </p>
          </div>
          <span className="rounded-full border border-amber-300 bg-amber-100 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-amber-700 dark:border-amber-800 dark:bg-amber-950 dark:text-amber-400">
            Prueba
          </span>
        </div>

        <TestChatMessages messages={messages} isTyping={isTyping} botName={botName} />

        <TestSuggestionChips onPick={sendMessage} disabled={isTyping || isPending} />
        <TestChatInput onSend={sendMessage} disabled={isTyping || isPending} />
      </div>
    </div>
  )
}