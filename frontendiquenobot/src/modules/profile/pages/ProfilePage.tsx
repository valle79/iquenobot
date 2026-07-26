import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/core/auth/auth.store'
import { Avatar } from '@/shared/atoms/Avatar/Avatar'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Button } from '@/shared/atoms/Button/Button'
import { Settings, Mail, Phone, Calendar, Shield, Building2, ChevronRight } from 'lucide-react'
import { dayjs } from '@/config/dayjs'
import { StaggerContainer, StaggerItem } from '@/shared/molecules/StaggerContainer'

function InfoRow({ icon: Icon, label, value }: { icon: typeof Mail; label: string; value: string }) {
  return (
    <div className="flex items-center gap-3 rounded-lg bg-gray-50 px-4 py-3 dark:bg-gray-800/50">
      <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-white dark:bg-gray-800">
        <Icon size={16} className="text-gray-500" />
      </div>
      <div className="flex-1">
        <p className="text-xs text-gray-500">{label}</p>
        <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{value || '-'}</p>
      </div>
    </div>
  )
}

export default function ProfilePage() {
  const navigate = useNavigate()
  const { user, tenant } = useAuthStore()

  if (!user) return null

  return (
    <div className="mx-auto max-w-2xl space-y-8">
      <StaggerContainer>
        <StaggerItem>
          <div className="rounded-2xl border border-gray-200 bg-white p-8 text-center dark:border-gray-700 dark:bg-gray-950">
            <div className="relative mx-auto inline-block">
              <Avatar name={user.fullName} src={user.avatarUrl} size="xl" />
              <div className="absolute -bottom-1 -right-1 h-4 w-4 rounded-full border-2 border-white bg-green-500" />
            </div>
            <h1 className="mt-4 text-2xl font-bold text-gray-900 dark:text-gray-100">{user.fullName}</h1>
            <p className="text-sm text-gray-500">{user.email}</p>
            <div className="mt-3 flex justify-center gap-2">
              <Badge variant={user.status === 'ACTIVE' ? 'success' : 'neutral'} size="sm">
                {user.status === 'ACTIVE' ? 'Activo' : 'Inactivo'}
              </Badge>
              <Badge variant="info" size="sm">
                {user.role === 'TENANT_ADMIN' ? 'Admin' : user.role === 'SUPERVISOR' ? 'Supervisor' : 'Agente'}
              </Badge>
            </div>
          </div>
        </StaggerItem>

        <StaggerItem>
          <div className="rounded-2xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
            <h2 className="mb-4 text-base font-semibold text-gray-900 dark:text-gray-100">Información personal</h2>
            <div className="space-y-2">
              <InfoRow icon={Mail} label="Email" value={user.email} />
              <InfoRow icon={Phone} label="Teléfono" value={user.phone} />
              <InfoRow icon={Calendar} label="Miembro desde" value={dayjs(user.createdAt).format('DD/MM/YYYY')} />
            </div>
          </div>
        </StaggerItem>

        <StaggerItem>
          <div className="rounded-2xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
            <h2 className="mb-4 text-base font-semibold text-gray-900 dark:text-gray-100">Empresa</h2>
            <div className="space-y-2">
              <InfoRow icon={Building2} label="Empresa" value={tenant?.companyName ?? '-'} />
              <InfoRow icon={Shield} label="Rol" value={user.role} />
              <InfoRow icon={Calendar} label="Plan" value={tenant?.subscriptionPlan ?? '-'} />
            </div>
          </div>
        </StaggerItem>

        <StaggerItem className="flex justify-center">
          <Button variant="outline" onClick={() => navigate('/settings')}>
            <Settings size={16} className="mr-2" />
            Ir a configuración
            <ChevronRight size={16} className="ml-1" />
          </Button>
        </StaggerItem>
      </StaggerContainer>
    </div>
  )
}
