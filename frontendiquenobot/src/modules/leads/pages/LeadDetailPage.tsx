import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, User, TrendingUp, Calendar, Tag, FileText, UserPlus } from 'lucide-react'
import { leadService } from '@/services/lead.service'
import { Button } from '@/shared/atoms/Button/Button'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Avatar } from '@/shared/atoms/Avatar/Avatar'
import { Skeleton } from '@/shared/atoms/Skeleton/Skeleton'
import { Select } from '@/shared/atoms/Select/Select'
import { useUpdateLeadStatus, useAssignLead } from '../hooks/useLeads'
import { useUsers } from '@/modules/users/hooks/useUsers'
import { dayjs } from '@/config/dayjs'
import { toast } from 'sonner'
import type { LeadStatus } from '@/types/enums'

const STATUS_OPTIONS = [
  { value: 'NEW', label: 'Nuevo' },
  { value: 'CONTACTED', label: 'Contactado' },
  { value: 'QUALIFIED', label: 'Calificado' },
  { value: 'CONVERTED', label: 'Convertido' },
  { value: 'LOST', label: 'Perdido' },
  { value: 'DISQUALIFIED', label: 'Descalificado' },
]

const STATUS_COLORS: Record<string, 'success' | 'warning' | 'error' | 'info' | 'neutral'> = {
  NEW: 'info',
  CONTACTED: 'warning',
  QUALIFIED: 'success',
  CONVERTED: 'success',
  LOST: 'error',
  DISQUALIFIED: 'neutral',
}

export default function LeadDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const updateStatus = useUpdateLeadStatus()
  const assignLead = useAssignLead()
  const { users } = useUsers()
  const [assignUserId, setAssignUserId] = useState('')

  const { data: lead, isLoading } = useQuery({
    queryKey: ['lead', id],
    queryFn: () => leadService.getById(id!),
    enabled: !!id,
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton width={200} height={28} />
        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2 space-y-4">
            <Skeleton width="100%" height={200} />
            <Skeleton width="100%" height={150} />
          </div>
          <Skeleton width="100%" height={300} />
        </div>
      </div>
    )
  }

  if (!lead) {
    return (
      <div className="flex flex-col items-center py-20">
        <p className="text-gray-500">Lead no encontrado</p>
        <Button variant="outline" className="mt-4" onClick={() => navigate('/leads')}>
          Volver a leads
        </Button>
      </div>
    )
  }

  const handleStatusChange = (newStatus: string) => {
    updateStatus.mutate({ id: lead.id, action: newStatus.toLowerCase() })
  }

  const handleAssign = () => {
    if (!assignUserId) return
    assignLead.mutate({ leadId: lead.id, userId: assignUserId }, {
      onSuccess: () => setAssignUserId(''),
    })
  }

  const score = lead.score ?? 0

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="sm" onClick={() => navigate('/leads')}>
          <ArrowLeft size={16} />
        </Button>
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{lead.title}</h1>
          <p className="mt-1 text-sm text-gray-500">
            Creado {dayjs(lead.createdAt).format('DD/MM/YYYY')} &middot; {lead.daysSinceCreated} días
          </p>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
            <h3 className="mb-4 text-base font-semibold text-gray-900 dark:text-gray-100">Información del lead</h3>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <p className="text-xs font-medium text-gray-500">Estado</p>
                <Select
                  options={STATUS_OPTIONS}
                  value={lead.status}
                  onChange={(e) => handleStatusChange(e.target.value)}
                  className="mt-1"
                />
              </div>
              <div>
                <p className="text-xs font-medium text-gray-500">Fuente</p>
                <p className="mt-1 text-sm text-gray-900 dark:text-gray-100">{lead.source}</p>
              </div>
              <div>
                <p className="text-xs font-medium text-gray-500">Valor estimado</p>
                <p className="mt-1 text-sm font-semibold text-gray-900 dark:text-gray-100">
                  ${lead.estimatedValue?.toLocaleString() ?? '0'}
                </p>
              </div>
              <div>
                <p className="text-xs font-medium text-gray-500">Probabilidad</p>
                <p className="mt-1 text-sm text-gray-900 dark:text-gray-100">{lead.probability ?? 0}%</p>
              </div>
              {lead.expectedCloseDate && (
                <div>
                  <p className="text-xs font-medium text-gray-500">Fecha esperada de cierre</p>
                  <p className="mt-1 text-sm text-gray-900 dark:text-gray-100">
                    {dayjs(lead.expectedCloseDate).format('DD/MM/YYYY')}
                  </p>
                </div>
              )}
              {lead.lastContactAt && (
                <div>
                  <p className="text-xs font-medium text-gray-500">Último contacto</p>
                  <p className="mt-1 text-sm text-gray-900 dark:text-gray-100">
                    {dayjs.utc(lead.lastContactAt).fromNow()}
                  </p>
                </div>
              )}
            </div>
            {lead.description && (
              <div className="mt-4">
                <p className="text-xs font-medium text-gray-500">Descripción</p>
                <p className="mt-1 text-sm text-gray-700 dark:text-gray-300">{lead.description}</p>
              </div>
            )}
            {lead.lostReason && (
              <div className="mt-4">
                <p className="text-xs font-medium text-gray-500">Razón de pérdida</p>
                <p className="mt-1 text-sm text-red-600">{lead.lostReason}</p>
              </div>
            )}
          </div>

          {lead.notes && (
            <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
              <div className="flex items-center gap-2 mb-3">
                <FileText size={16} className="text-gray-400" />
                <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100">Notas</h3>
              </div>
              <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{lead.notes}</p>
            </div>
          )}

          {lead.tags && (
            <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
              <div className="flex items-center gap-2 mb-3">
                <Tag size={16} className="text-gray-400" />
                <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100">Etiquetas</h3>
              </div>
              <div className="flex flex-wrap gap-2">
                {lead.tags.split(',').map((tag, i) => (
                  <Badge key={i} variant="info" size="sm">{tag.trim()}</Badge>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="space-y-6">
          <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
            <h3 className="mb-4 text-base font-semibold text-gray-900 dark:text-gray-100">Score</h3>
            <div className="flex items-center gap-3">
              <div className="relative h-16 w-16">
                <svg className="h-16 w-16 -rotate-90" viewBox="0 0 36 36">
                  <circle cx="18" cy="18" r="16" fill="none" stroke="currentColor" strokeWidth="2" className="text-gray-200 dark:text-gray-700" />
                  <circle
                    cx="18" cy="18" r="16" fill="none" strokeWidth="2"
                    strokeDasharray={`${score} ${100 - score}`}
                    strokeLinecap="round"
                    className={score >= 70 ? 'stroke-green-500' : score >= 40 ? 'stroke-yellow-500' : 'stroke-red-500'}
                  />
                </svg>
                <span className="absolute inset-0 flex items-center justify-center text-sm font-bold text-gray-900 dark:text-gray-100">
                  {score}
                </span>
              </div>
              <div>
                <p className="text-sm text-gray-500">Puntuación del lead</p>
                <p className="text-xs text-gray-400">
                  {score >= 70 ? 'Alta probabilidad' : score >= 40 ? 'Media probabilidad' : 'Baja probabilidad'}
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
            <h3 className="mb-4 text-base font-semibold text-gray-900 dark:text-gray-100">Contacto</h3>
            {lead.contact ? (
              <div
                className="flex items-center gap-3 cursor-pointer rounded-lg p-2 hover:bg-gray-50 dark:hover:bg-gray-800"
                onClick={() => navigate(`/contacts/${lead.contact.id}`)}
              >
                <Avatar name={lead.contact.fullName} src={lead.contact.avatarUrl} size="md" />
                <div>
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{lead.contact.fullName}</p>
                  <p className="text-xs text-gray-500">{lead.contact.email || lead.contact.phone || 'Sin contacto'}</p>
                </div>
              </div>
            ) : (
              <p className="text-sm text-gray-500">Sin contacto asociado</p>
            )}
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
            <h3 className="mb-4 text-base font-semibold text-gray-900 dark:text-gray-100">Asignación</h3>
            {lead.assignedTo ? (
              <div className="flex items-center gap-3">
                <Avatar name={lead.assignedTo.fullName} src={lead.assignedTo.avatarUrl} size="sm" />
                <div>
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{lead.assignedTo.fullName}</p>
                  <p className="text-xs text-gray-500">
                    Asignado {lead.assignedAt ? dayjs.utc(lead.assignedAt).fromNow() : ''}
                  </p>
                </div>
              </div>
            ) : (
              <p className="text-sm text-gray-500 mb-3">Sin asignar</p>
            )}
            <div className="mt-3 flex gap-2">
              <Select
                options={users.map((u) => ({ value: u.id, label: u.fullName }))}
                value={assignUserId}
                onChange={(e) => setAssignUserId(e.target.value)}
                placeholder="Seleccionar agente"
                className="flex-1"
              />
              <Button
                size="sm"
                onClick={handleAssign}
                disabled={!assignUserId}
                loading={assignLead.isPending}
              >
                <UserPlus size={14} />
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
