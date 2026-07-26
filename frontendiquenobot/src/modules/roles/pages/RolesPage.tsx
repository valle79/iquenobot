import { Shield, Users, UserCheck, Eye, Edit, Trash2 } from 'lucide-react'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Button } from '@/shared/atoms/Button/Button'

interface RoleCardProps {
  name: string
  description: string
  userCount: number
  permissions: string[]
  color: 'info' | 'success' | 'warning' | 'neutral'
}

function RoleCard({ name, description, userCount, permissions, color }: RoleCardProps) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 transition-all hover:shadow-elevated dark:border-gray-700 dark:bg-gray-950">
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-3">
          <div className={`rounded-lg bg-${color === 'info' ? 'blue' : color === 'success' ? 'green' : color === 'warning' ? 'yellow' : 'gray'}-100 p-3 dark:bg-${color === 'info' ? 'blue' : color === 'success' ? 'green' : color === 'warning' ? 'yellow' : 'gray'}-900/20`}>
            <Shield size={20} className={`text-${color === 'info' ? 'blue' : color === 'success' ? 'green' : color === 'warning' ? 'yellow' : 'gray'}-600 dark:text-${color === 'info' ? 'blue' : color === 'success' ? 'green' : color === 'warning' ? 'yellow' : 'gray'}-400`} />
          </div>
          <div>
            <h3 className="font-semibold text-gray-900 dark:text-gray-100">{name}</h3>
            <p className="text-sm text-gray-500">{userCount} usuarios</p>
          </div>
        </div>
      </div>
      <p className="mt-4 text-sm text-gray-600 dark:text-gray-400">{description}</p>
      <div className="mt-4 flex flex-wrap gap-1.5">
        {permissions.map((p) => (
          <Badge key={p} variant="neutral" size="sm">{p}</Badge>
        ))}
      </div>
      <div className="mt-4 flex gap-2">
        <Button variant="outline" size="sm"><Eye size={14} />Ver</Button>
        <Button variant="outline" size="sm"><Edit size={14} />Editar</Button>
      </div>
    </div>
  )
}

export default function RolesPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Roles y Permisos</h1>
        <p className="mt-1 text-sm text-gray-500">Gestiona los roles y permisos del sistema</p>
      </div>

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        <RoleCard
          name="Super Admin"
          description="Acceso completo al sistema. Puede gestionar tenants, usuarios y configuración global."
          userCount={1}
          permissions={['Todos los permisos']}
          color="info"
        />
        <RoleCard
          name="Admin"
          description="Administración del tenant. Puede gestionar usuarios, roles, configuraciones y ver reportes."
          userCount={3}
          permissions={['Usuarios', 'Roles', 'Configuración', 'Reportes']}
          color="success"
        />
        <RoleCard
          name="Supervisor"
          description="Supervisión de agentes. Puede asignar conversaciones, ver estadísticas y gestionar contactos."
          userCount={5}
          permissions={['Asignar', 'Ver stats', 'Contactos', 'Productos']}
          color="warning"
        />
        <RoleCard
          name="Agente"
          description="Atención al cliente. Puede gestionar conversaciones, contactos y ver productos."
          userCount={12}
          permissions={['Conversaciones', 'Contactos', 'Productos']}
          color="neutral"
        />
        <RoleCard
          name="Bot"
          description="Usuario automatizado para el chatbot. Solo puede enviar y recibir mensajes."
          userCount={2}
          permissions={['Mensajes']}
          color="neutral"
        />
      </div>
    </div>
  )
}
