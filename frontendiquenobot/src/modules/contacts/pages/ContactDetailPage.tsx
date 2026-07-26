import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { ArrowLeft, Mail, Phone, Building, Globe, Calendar, MessageCircle, Tag, Edit, Ban, CheckCircle } from 'lucide-react'
import { contactService } from '@/services/contact.service'
import { Button } from '@/shared/atoms/Button/Button'
import { Avatar } from '@/shared/atoms/Avatar/Avatar'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Skeleton } from '@/shared/atoms/Skeleton/Skeleton'
import { dayjs } from '@/config/dayjs'
import { STALE_TIMES } from '@/config/constants'
import { useBlockContact, useUnblockContact } from '../hooks/useContacts'
import { useState } from 'react'
import { ConfirmDialog } from '@/shared/molecules/ConfirmDialog'

export default function ContactDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const blockMutation = useBlockContact()
  const unblockMutation = useUnblockContact()
  const [showBlock, setShowBlock] = useState(false)

  const { data: contact, isLoading } = useQuery({
    queryKey: ['contact', id],
    queryFn: () => contactService.getById(id!),
    enabled: !!id,
    staleTime: STALE_TIMES.MEDIUM,
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Button variant="ghost" onClick={() => navigate('/contacts')}>
          <ArrowLeft size={18} />
          Volver
        </Button>
        <div className="space-y-4">
          <Skeleton width={300} height={40} />
          <Skeleton width={200} height={20} />
        </div>
      </div>
    )
  }

  if (!contact) {
    return (
      <div className="space-y-6">
        <Button variant="ghost" onClick={() => navigate('/contacts')}>
          <ArrowLeft size={18} />
          Volver
        </Button>
        <p className="text-gray-500">Contacto no encontrado</p>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <Button variant="ghost" onClick={() => navigate('/contacts')}>
        <ArrowLeft size={18} />
        Volver
      </Button>

      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-4">
            <Avatar name={contact.fullName} src={contact.avatarUrl} size="xl" />
            <div>
              <h1 className="text-xl font-bold text-gray-900 dark:text-gray-100">
                {contact.displayName || contact.fullName}
              </h1>
              <p className="text-sm text-gray-500">{contact.jobTitle || 'Sin cargo'}</p>
              <div className="mt-2 flex gap-2">
                <Badge
                  variant={
                    contact.status === 'ACTIVE' ? 'success'
                    : contact.status === 'BLOCKED' ? 'error'
                    : 'neutral'
                  }
                  size="sm"
                >
                  {contact.status === 'ACTIVE' ? 'Activo'
                   : contact.status === 'BLOCKED' ? 'Bloqueado'
                   : contact.status === 'ARCHIVED' ? 'Archivado'
                   : 'Inactivo'}
                </Badge>
                <Badge variant="info" size="sm">
                  {contact.conversationCount ?? 0} conversaciones
                </Badge>
              </div>
            </div>
          </div>

          <div className="flex gap-2">
            <Button variant="outline" size="sm">
              <Edit size={16} />
              Editar
            </Button>
            {contact.status === 'BLOCKED' ? (
              <Button variant="outline" size="sm" onClick={() => unblockMutation.mutate(contact.id)}>
                <CheckCircle size={16} />
                Desbloquear
              </Button>
            ) : (
              <Button variant="outline" size="sm" onClick={() => setShowBlock(true)}>
                <Ban size={16} />
                Bloquear
              </Button>
            )}
          </div>
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-gray-500">
            Información de contacto
          </h2>
          <div className="space-y-4">
            {[
              { icon: Mail, label: 'Email', value: contact.email },
              { icon: Phone, label: 'Teléfono', value: contact.phone },
              { icon: Phone, label: 'WhatsApp', value: contact.whatsappPhone },
              { icon: Building, label: 'Empresa', value: contact.company },
              { icon: Globe, label: 'Idioma', value: contact.language },
            ].map(({ icon: Icon, label, value }) => (
              <div key={label} className="flex items-center gap-3">
                <Icon size={16} className="text-gray-400" />
                <div>
                  <p className="text-xs text-gray-500">{label}</p>
                  <p className="text-sm text-gray-900 dark:text-gray-100">{value || '—'}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-gray-500">
            Actividad
          </h2>
          <div className="space-y-4">
            {[
              { icon: Calendar, label: 'Creado', value: dayjs(contact.createdAt).format('DD/MM/YYYY HH:mm') },
              { icon: Calendar, label: 'Actualizado', value: dayjs(contact.updatedAt).format('DD/MM/YYYY HH:mm') },
              { icon: MessageCircle, label: 'Último contacto', value: contact.lastContactedAt ? dayjs(contact.lastContactedAt).fromNow() : 'Nunca' },
              { icon: MessageCircle, label: 'Mensajes', value: String(contact.messageCount ?? 0) },
            ].map(({ icon: Icon, label, value }) => (
              <div key={label} className="flex items-center gap-3">
                <Icon size={16} className="text-gray-400" />
                <div>
                  <p className="text-xs text-gray-500">{label}</p>
                  <p className="text-sm text-gray-900 dark:text-gray-100">{value}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {contact.notes && (
        <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
          <h2 className="mb-2 text-sm font-semibold uppercase tracking-wider text-gray-500">Notas</h2>
          <p className="text-sm text-gray-700 dark:text-gray-300">{contact.notes}</p>
        </div>
      )}

      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-gray-500">Etiquetas</h2>
        <div className="flex flex-wrap gap-2">
          {contact.tags ? contact.tags.split(',').map((tag) => (
            <Badge key={tag} variant="neutral">{tag.trim()}</Badge>
          )) : <p className="text-sm text-gray-500">Sin etiquetas</p>}
        </div>
      </div>

      <ConfirmDialog
        open={showBlock}
        onClose={() => setShowBlock(false)}
        onConfirm={() => { blockMutation.mutate({ id: contact.id }); setShowBlock(false) }}
        title="Bloquear contacto"
        message="El contacto no podrá recibir mensajes hasta que sea desbloqueado."
        confirmLabel="Bloquear"
        loading={blockMutation.isPending}
      />
    </div>
  )
}
