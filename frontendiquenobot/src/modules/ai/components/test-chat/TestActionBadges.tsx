import { FileText, Target, PhoneCall, MessageSquare, type LucideIcon } from 'lucide-react'
import type { SimulatedActionDto } from '@/types/chatbot'

const ACTION_META: Record<string, { icon: LucideIcon; className: string }> = {
  SEND_TEXT: { icon: MessageSquare, className: 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-300' },
  CREATE_LEAD: { icon: Target, className: 'bg-blue-50 text-blue-600 dark:bg-blue-950 dark:text-blue-400' },
  SEND_QUOTE: { icon: FileText, className: 'bg-amber-50 text-amber-600 dark:bg-amber-950 dark:text-amber-400' },
  TRANSFER_CONVERSATION: { icon: PhoneCall, className: 'bg-rose-50 text-rose-600 dark:bg-rose-950 dark:text-rose-400' },
}

function metaFor(actionType: string) {
  return ACTION_META[actionType] ?? ACTION_META.SEND_TEXT
}

export function TestActionBadges({ actions }: { actions: SimulatedActionDto[] }) {
  if (actions.length === 0) return null

  return (
    <div className="mt-2 max-w-full rounded-xl border border-amber-200 bg-amber-50/70 px-3 py-2 dark:border-amber-900/60 dark:bg-amber-950/40">
      <p className="flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wide text-amber-700 dark:text-amber-500">
        <span className="relative flex h-1.5 w-1.5">
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-amber-500 opacity-75" />
          <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-amber-500" />
        </span>
        En producción haría esto
      </p>
      <ul className="mt-1.5 space-y-1.5">
        {actions.map((action, i) => {
          const meta = metaFor(action.actionType)
          const Icon = meta.icon
          return (
            <li key={`${action.actionType}-${String(i)}`} className="flex items-start gap-2" title={action.description}>
              <span className={`mt-0.5 shrink-0 rounded-md p-1 ${meta.className}`}>
                <Icon size={12} />
              </span>
              <div className="min-w-0">
                <p className="text-xs font-medium text-gray-800 dark:text-gray-100">{action.label}</p>
                <p className="text-[11px] leading-snug text-gray-500 dark:text-gray-400">{action.description}</p>
              </div>
            </li>
          )
        })}
      </ul>
    </div>
  )
}