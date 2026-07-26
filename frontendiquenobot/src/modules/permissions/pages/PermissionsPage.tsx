import { Shield, User, Eye, Pencil, Plus, Trash2, Check, X } from 'lucide-react'
import { permissionsMap, type PermissionResource, type PermissionAction } from '@/core/rbac/permissions'
import { Badge } from '@/shared/atoms/Badge/Badge'

const resources: { key: PermissionResource; label: string }[] = [
  { key: 'users', label: 'Usuarios' },
  { key: 'conversations', label: 'Conversaciones' },
  { key: 'contacts', label: 'Contactos' },
  { key: 'leads', label: 'Oportunidades' },
  { key: 'products', label: 'Productos' },
  { key: 'categories', label: 'Categorías' },
  { key: 'roles', label: 'Roles' },
  { key: 'permissions', label: 'Permisos' },
  { key: 'notifications', label: 'Notificaciones' },
  { key: 'settings', label: 'Configuración' },
  { key: 'analytics', label: 'Analíticas' },
  { key: 'tenant', label: 'Empresa' },
  { key: 'ai', label: 'IA' },
]

const actions: { key: PermissionAction; label: string; icon: typeof Eye }[] = [
  { key: 'view', label: 'Ver', icon: Eye },
  { key: 'create', label: 'Crear', icon: Plus },
  { key: 'edit', label: 'Editar', icon: Pencil },
  { key: 'delete', label: 'Eliminar', icon: Trash2 },
]

const allRoles = ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'] as const

export default function PermissionsPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Shield size={24} className="text-brand-500" />
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Permisos</h1>
          <p className="mt-1 text-sm text-gray-500">Matriz de permisos por rol y recurso</p>
        </div>
      </div>

      <div className="overflow-x-auto rounded-xl border border-gray-200 dark:border-gray-700">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-gray-200 bg-gray-50 dark:border-gray-700 dark:bg-gray-900">
              <th className="px-4 py-3 text-left font-medium text-gray-600 dark:text-gray-400">Recurso</th>
              {actions.map((action) => (
                <th key={action.key} className="px-4 py-3 text-center font-medium text-gray-600 dark:text-gray-400">
                  <div className="flex items-center justify-center gap-1">
                    <action.icon size={14} />
                    <span>{action.label}</span>
                  </div>
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
            {resources.map((resource) => {
              const perm = permissionsMap[resource.key]
              return (
                <tr key={resource.key} className="hover:bg-gray-50 dark:hover:bg-gray-900/50">
                  <td className="px-4 py-3 font-medium text-gray-900 dark:text-gray-100">{resource.label}</td>
                  {actions.map((action) => {
                    const allowed = perm?.[action.key] ?? []
                    return (
                      <td key={action.key} className="px-4 py-3 text-center">
                        <div className="flex items-center justify-center gap-1">
                          {allRoles.map((role) => (
                            <Badge
                              key={role}
                              variant={allowed.includes(role as any) ? 'success' : 'neutral'}
                              size="sm"
                            >
                              {role === 'TENANT_ADMIN' ? 'Admin'
                                : role === 'SUPERVISOR' ? 'Sup.'
                                : 'Agent'}
                            </Badge>
                          ))}
                        </div>
                      </td>
                    )
                  })}
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}
