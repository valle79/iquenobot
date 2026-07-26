import { MessageCircle, Users, Clock, LayoutDashboard } from 'lucide-react'
import type { ConversationStats, ContactStats, LeadStats } from '@/types/dashboard'

interface KpiCardsProps {
  conversations: ConversationStats
  contacts: ContactStats
  leads: LeadStats
}

function KpiCard({ title, value, icon, subtitle }: { title: string; value: string | number; icon: React.ReactNode; subtitle?: string }) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 transition-all hover:shadow-elevated dark:border-gray-700 dark:bg-gray-950">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-medium text-gray-500">{title}</p>
          <p className="mt-2 text-3xl font-bold text-gray-900 dark:text-gray-100">{value}</p>
          {subtitle && <p className="mt-1 text-xs text-gray-400">{subtitle}</p>}
        </div>
        <div className="rounded-lg bg-brand-50 p-3 text-brand-600 dark:bg-brand-900/20 dark:text-brand-400">
          {icon}
        </div>
      </div>
    </div>
  )
}

export function KpiCards({ conversations, contacts, leads }: KpiCardsProps) {
  return (
    <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
      <KpiCard
        title="Conversaciones activas"
        value={conversations.activeConversations}
        icon={<MessageCircle size={22} />}
        subtitle={`${conversations.conversationsToday} hoy`}
      />
      <KpiCard
        title="Contactos activos"
        value={contacts.activeContacts}
        icon={<Users size={22} />}
        subtitle={`${contacts.contactsToday} nuevos hoy`}
      />
      <KpiCard
        title="Tiempo promedio respuesta"
        value={`${Math.round(conversations.averageResponseTimeMinutes)}m`}
        icon={<Clock size={22} />}
      />
      <KpiCard
        title="Total hoy"
        value={conversations.conversationsToday + contacts.contactsToday + leads.leadsToday}
        icon={<LayoutDashboard size={22} />}
      />
    </div>
  )
}
