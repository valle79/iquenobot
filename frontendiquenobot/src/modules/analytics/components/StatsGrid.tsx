import type { ConversationStats, LeadStats, ContactStats, ProductStats } from '@/types/dashboard'

interface StatsGridProps {
  conversations: ConversationStats
  leads: LeadStats
  contacts: ContactStats
  products: ProductStats
}

function StatBox({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-lg border border-gray-100 bg-gray-50 p-4 dark:border-gray-800 dark:bg-gray-900">
      <p className="text-xs font-medium text-gray-500">{label}</p>
      <p className="mt-1 text-xl font-bold text-gray-900 dark:text-gray-100">{value}</p>
    </div>
  )
}

export function StatsGrid({ conversations, leads, contacts, products }: StatsGridProps) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <div className="space-y-3">
        <h4 className="text-xs font-semibold uppercase tracking-wider text-gray-400">Conversaciones</h4>
        <StatBox label="Totales" value={conversations.totalConversations} />
        <StatBox label="Activas" value={conversations.activeConversations} />
        <StatBox label="Tiempo promedio respuesta" value={`${Math.round(conversations.averageResponseTimeMinutes)} min`} />
        <StatBox label="Tiempo promedio resolución" value={`${Math.round(conversations.averageResolutionTimeHours)} h`} />
      </div>
      <div className="space-y-3">
        <h4 className="text-xs font-semibold uppercase tracking-wider text-gray-400">Leads</h4>
        <StatBox label="Totales" value={leads.totalLeads} />
        <StatBox label="Sin asignar" value={leads.unassignedLeads} />
        <StatBox label="Score promedio" value={Math.round(leads.averageLeadScore)} />
        <StatBox label="Tasa conversión" value={`${leads.conversionRate.toFixed(1)}%`} />
      </div>
      <div className="space-y-3">
        <h4 className="text-xs font-semibold uppercase tracking-wider text-gray-400">Contactos</h4>
        <StatBox label="Totales" value={contacts.totalContacts} />
        <StatBox label="Activos" value={contacts.activeContacts} />
        <StatBox label="Bloqueados" value={contacts.blockedContacts} />
        <StatBox label="Con conversaciones" value={contacts.contactsWithConversations} />
      </div>
      <div className="space-y-3">
        <h4 className="text-xs font-semibold uppercase tracking-wider text-gray-400">Productos</h4>
        <StatBox label="Totales" value={products.totalProducts} />
        <StatBox label="Activos" value={products.activeProducts} />
        <StatBox label="Sin stock" value={products.outOfStockProducts} />
        <StatBox label="Stock bajo" value={products.lowStockProducts} />
      </div>
    </div>
  )
}
