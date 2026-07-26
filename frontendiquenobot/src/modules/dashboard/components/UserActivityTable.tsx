import { useTranslation } from 'react-i18next'
import { MessageCircle, CheckCheck, Target, UserPlus } from 'lucide-react'
import type { UserActivity } from '@/types/dashboard'

interface UserActivityTableProps {
  data: UserActivity[]
}

export function UserActivityTable({ data }: UserActivityTableProps) {
  const { t } = useTranslation()

  if (data.length === 0) {
    return (
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
          Actividad de usuarios
        </h3>
        <p className="text-sm text-gray-400">{t('common.noData')}</p>
      </div>
    )
  }

  return (
    <div className="rounded-xl border border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-950">
      <div className="border-b border-gray-100 px-6 py-4 dark:border-gray-800">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
          Actividad de usuarios
        </h3>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-gray-100 text-left text-xs font-medium uppercase text-gray-500 dark:border-gray-800">
              <th className="px-6 py-3">Usuario</th>
              <th className="px-6 py-3">
                <span className="flex items-center gap-1"><MessageCircle size={12} /> Conversaciones</span>
              </th>
              <th className="px-6 py-3">
                <span className="flex items-center gap-1"><CheckCheck size={12} /> Cerradas</span>
              </th>
              <th className="px-6 py-3">
                <span className="flex items-center gap-1"><Target size={12} /> Leads</span>
              </th>
              <th className="px-6 py-3">
                <span className="flex items-center gap-1"><UserPlus size={12} /> Convertidos</span>
              </th>
              <th className="px-6 py-3">
                <span className="flex items-center gap-1"><MessageCircle size={12} /> Mensajes</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {data.map((user) => (
              <tr key={user.userId} className="border-b border-gray-50 last:border-b-0 hover:bg-gray-50 dark:border-gray-800 dark:hover:bg-gray-800/50">
                <td className="px-6 py-3">
                  <div>
                    <p className="font-medium text-gray-900 dark:text-gray-100">{user.userName}</p>
                    <p className="text-xs text-gray-400">{user.userEmail}</p>
                  </div>
                </td>
                <td className="px-6 py-3 text-gray-900 dark:text-gray-100">{user.assignedConversations}</td>
                <td className="px-6 py-3 text-gray-900 dark:text-gray-100">{user.closedConversations}</td>
                <td className="px-6 py-3 text-gray-900 dark:text-gray-100">{user.assignedLeads}</td>
                <td className="px-6 py-3 text-gray-900 dark:text-gray-100">{user.convertedLeads}</td>
                <td className="px-6 py-3 text-gray-900 dark:text-gray-100">{user.sentMessages}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
