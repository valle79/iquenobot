import { useEffect, useState } from 'react'
import { adminService, type PlanDto, type CreatePlanRequest } from '@/services/admin.service'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { Button } from '@/shared/atoms/Button/Button'
import { Input } from '@/shared/atoms/Input/Input'
import { ConfirmDialog } from '@/shared/molecules/ConfirmDialog'
import { Check, Crown, Edit2, Loader2, Save, Trash2, X } from 'lucide-react'

const FEATURE_LABELS: Record<string, string> = {
  whatsapp: 'WhatsApp',
  ai_assistant: 'Asistente IA',
  reports: 'Reportes',
  api_access: 'API Access',
  custom_branding: 'Branding Personalizado',
  multi_agent: 'Multi-Agente',
}

const FEATURE_KEYS = Object.keys(FEATURE_LABELS)

function parseFeatures(featuresStr: string | null | undefined): Record<string, boolean> {
  if (!featuresStr) return {}
  try {
    return JSON.parse(featuresStr)
  } catch {
    return {}
  }
}

function stringifyFeatures(features: Record<string, boolean>): string {
  return JSON.stringify(features)
}

function formatSoles(value: number): string {
  return `S/ ${value.toFixed(2)}`
}

function formatLimit(value: number | null | undefined): string {
  if (value == null) return '—'
  if (value >= 999999) return 'Ilimitados'
  return value.toLocaleString('es-PE')
}

function formatStorage(value: number | null | undefined): string {
  if (value == null) return '—'
  if (value >= 10000) return `${value / 1000} GB`
  return `${value} MB`
}

export default function AdminPlansPage() {
  const [plans, setPlans] = useState<PlanDto[]>([])
  const [loading, setLoading] = useState(true)
  const [showModal, setShowModal] = useState(false)
  const [editingPlan, setEditingPlan] = useState<PlanDto | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<PlanDto | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  useEffect(() => {
    void loadPlans()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const loadPlans = async () => {
    try {
      const res = await adminService.getPlans({ size: 50 })
      setPlans(
        res.content.filter((p) => p.active).sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)),
      )
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async (plan: PlanDto) => {
    setDeleting(true)
    setDeleteError(null)
    try {
      await adminService.deletePlan(plan.id)

      // Actualiza la lista localmente sin recargar toda la página
      setPlans((prev) => prev.filter((p) => p.id !== plan.id))
      setDeleteTarget(null)
    } catch (e) {
      console.error('Error eliminando plan:', e)
      setDeleteError('No se pudo eliminar el plan. Asegúrate de que ninguna empresa lo tenga asignado.')
    } finally {
      setDeleting(false)
    }
  }

  const handleEdit = (plan: PlanDto) => {
    setEditingPlan(plan)
    setShowModal(true)
  }

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Loader2 className="text-brand-600 h-8 w-8 animate-spin" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Planes</h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Administra los planes disponibles. Puedes editar cada uno de ellos, incluidos precios,
          límites y funcionalidades.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {plans.map((plan) => {
          const features = parseFeatures(plan.features)
          const featured = plan.code === 'professional'
          const monthly = plan.monthlyPrice ?? 0
          const yearly = plan.yearlyPrice ?? 0
          const annualSaving = monthly > 0 ? monthly * 12 - yearly : 0
          return (
            <div
              key={plan.id}
              className={`relative flex flex-col rounded-2xl border bg-white p-6 shadow-sm transition-shadow hover:shadow-md dark:bg-gray-950 ${
                featured
                  ? 'border-brand-500/60 ring-brand-500/30 ring-1'
                  : 'border-gray-200 dark:border-gray-800'
              }`}
            >
              {featured && (
                <span className="bg-brand-600 absolute -top-3 left-1/2 flex -translate-x-1/2 items-center gap-1 rounded-full px-3 py-1 text-xs font-semibold text-white">
                  <Crown size={12} /> Recomendado
                </span>
              )}

              <h3 className="text-lg font-bold text-gray-900 dark:text-white">{plan.name}</h3>
              <p className="mt-1 min-h-10 text-sm text-gray-500 dark:text-gray-400">
                {plan.description}
              </p>

              <div className="mt-4 flex items-baseline gap-1">
                <span className="text-3xl font-extrabold tracking-tight text-gray-900 dark:text-white">
                  {formatSoles(monthly)}
                </span>
                <span className="text-sm text-gray-500 dark:text-gray-400">/mes</span>
              </div>
              {yearly > 0 && (
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  {formatSoles(yearly)}/año
                  {annualSaving > 0 && (
                    <span className="ml-1 font-medium text-green-600 dark:text-green-400">
                      (ahorra {formatSoles(annualSaving)})
                    </span>
                  )}
                </p>
              )}

              <div className="mt-5 space-y-2.5 border-t border-gray-100 pt-4 text-sm dark:border-gray-800">
                <div className="flex items-center justify-between">
                  <span className="text-gray-500 dark:text-gray-400">Usuarios</span>
                  <span className="font-medium text-gray-900 dark:text-white">
                    {formatLimit(plan.maxUsers)}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-gray-500 dark:text-gray-400">Conversaciones</span>
                  <span className="font-medium text-gray-900 dark:text-white">
                    {formatLimit(plan.maxConversations)}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-gray-500 dark:text-gray-400">Contactos</span>
                  <span className="font-medium text-gray-900 dark:text-white">
                    {formatLimit(plan.maxContacts)}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-gray-500 dark:text-gray-400">Almacenamiento</span>
                  <span className="font-medium text-gray-900 dark:text-white">
                    {formatStorage(plan.maxStorageMb)}
                  </span>
                </div>
              </div>

              <div className="mt-5 grid grid-cols-1 gap-1.5 border-t border-gray-100 pt-4 sm:grid-cols-2 dark:border-gray-800">
                {FEATURE_KEYS.map((key) => (
                  <div key={key} className="flex items-center gap-1.5 text-sm">
                    {features[key] ? (
                      <Check size={14} className="shrink-0 text-green-500" />
                    ) : (
                      <X size={14} className="shrink-0 text-gray-300 dark:text-gray-600" />
                    )}
                    <span
                      className={
                        features[key]
                          ? 'text-gray-700 dark:text-gray-300'
                          : 'text-gray-400 dark:text-gray-500'
                      }
                    >
                      {FEATURE_LABELS[key]}
                    </span>
                  </div>
                ))}
              </div>

              <div className="mt-6 flex flex-1 items-end">
                <div className="mt-6 flex flex-1 items-end gap-2">
                  <Button variant="outline" className="flex-1" onClick={() => handleEdit(plan)}>
                    <Edit2 size={15} />
                    Editar
                  </Button>

                  <Button
                    variant="outline"
                    className="border-red-200 text-red-600 hover:border-red-300 hover:bg-red-50 hover:text-red-700 dark:border-red-800 dark:text-red-400 dark:hover:bg-red-950/30"
                    onClick={() => setDeleteTarget(plan)}
                  >
                    <Trash2 size={15} />
                  </Button>
                </div>
              </div>
            </div>
          )
        })}
      </div>

      {showModal && (
        <PlanModal
          plan={editingPlan}
          onClose={() => {
            setShowModal(false)
            setEditingPlan(null)
          }}
          onSaved={() => {
            setShowModal(false)
            setEditingPlan(null)
            void loadPlans()
          }}
        />
      )}

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => {
          if (!deleting) setDeleteTarget(null)
        }}
        onConfirm={() => {
          if (deleteTarget) void handleDelete(deleteTarget)
        }}
        title="Eliminar plan"
        message={
          deleteError ??
          `¿Estás seguro de eliminar el plan "${deleteTarget?.name}"? Esta acción no se puede deshacer.`
        }
        confirmLabel="Eliminar"
        loading={deleting}
      />
    </div>
  )
}

function PlanModal({
  plan,
  onClose,
  onSaved,
}: {
  plan: PlanDto | null
  onClose: () => void
  onSaved: () => void
}) {
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState({
    name: plan?.name ?? '',
    code: plan?.code ?? '',
    description: plan?.description ?? '',
    monthlyPrice: plan?.monthlyPrice ?? 0,
    yearlyPrice: plan?.yearlyPrice ?? 0,
    maxUsers: plan?.maxUsers ?? null,
    maxConversations: plan?.maxConversations ?? null,
    maxContacts: plan?.maxContacts ?? null,
    maxStorageMb: plan?.maxStorageMb ?? null,
    active: plan?.active ?? true,
    publicPlan: plan?.publicPlan ?? true,
    sortOrder: plan?.sortOrder ?? 1,
  })
  const [features, setFeatures] = useState<Record<string, boolean>>(() => {
    const parsed = parseFeatures(plan?.features)
    return Object.fromEntries(FEATURE_KEYS.map((key) => [key, parsed[key] ?? false]))
  })

  const update = (key: string, value: string | number | boolean | null) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  const updateNumber = (key: string, value: string) =>
    update(key, value === '' ? null : Number(value))

  const handleSave = async () => {
    setSaving(true)
    try {
      const payload: CreatePlanRequest = {
        name: form.name,
        code: form.code,
        description: form.description,
        monthlyPrice: form.monthlyPrice ?? 0,
        yearlyPrice: form.yearlyPrice ?? 0,
        maxUsers: form.maxUsers ?? -1,
        maxConversations: form.maxConversations ?? -1,
        maxContacts: form.maxContacts ?? -1,
        maxStorageMb: form.maxStorageMb ?? -1,
        features: stringifyFeatures(features),
        active: form.active,
        publicPlan: form.publicPlan,
        sortOrder: form.sortOrder ?? 1,
      }
      if (plan) await adminService.updatePlan(plan.id, payload)
      else await adminService.createPlan(payload)
      onSaved()
    } catch (e) {
      console.error(e)
    } finally {
      setSaving(false)
    }
  }

  const limitField = (
    label: string,
    key: 'maxUsers' | 'maxConversations' | 'maxContacts' | 'maxStorageMb',
  ) => (
    <Input
      label={label}
      type="number"
      min={0}
      placeholder="Sin límite"
      value={form[key] ?? ''}
      onChange={(e) => updateNumber(key, e.target.value)}
      helperText="Déjalo vacío para no aplicar límite"
    />
  )

  return (
    <Modal
      open
      onClose={onClose}
      title={plan ? `Editar plan ${plan.name}` : 'Nuevo Plan'}
      description="Modifica los datos del plan y guarda los cambios"
      size="lg"
    >
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Input
          label="Nombre"
          placeholder="Ej: Básico"
          value={form.name}
          onChange={(e) => update('name', e.target.value)}
        />
        <Input
          label="Código"
          placeholder="Ej: basic"
          value={form.code}
          onChange={(e) => update('code', e.target.value)}
        />
        <div className="sm:col-span-2">
          <label className="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Descripción
          </label>
          <textarea
            value={form.description}
            onChange={(e) => update('description', e.target.value)}
            rows={2}
            className="focus:border-brand-500 focus:ring-brand-500 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 placeholder-gray-400 transition-colors focus:ring-1 focus:outline-none dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100 dark:placeholder-gray-500"
          />
        </div>
        <Input
          label="Precio mensual (S/)"
          type="number"
          min={0}
          step="0.01"
          value={form.monthlyPrice}
          onChange={(e) => update('monthlyPrice', Number(e.target.value))}
        />
        <Input
          label="Precio anual (S/)"
          type="number"
          min={0}
          step="0.01"
          value={form.yearlyPrice}
          onChange={(e) => update('yearlyPrice', Number(e.target.value))}
        />
        {limitField('Máx. usuarios', 'maxUsers')}
        {limitField('Máx. conversaciones', 'maxConversations')}
        {limitField('Máx. contactos', 'maxContacts')}
        {limitField('Máx. almacenamiento (MB)', 'maxStorageMb')}
        <Input
          label="Orden de visualización"
          type="number"
          min={1}
          value={form.sortOrder ?? 1}
          onChange={(e) => update('sortOrder', Number(e.target.value))}
        />
      </div>

      <div className="mt-5">
        <p className="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">
          Funcionalidades incluidas
        </p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          {FEATURE_KEYS.map((key) => {
            const enabled = features[key]
            return (
              <label
                key={key}
                className={`flex cursor-pointer items-center gap-2 rounded-lg border px-3 py-2 text-sm transition-colors ${
                  enabled
                    ? 'border-brand-500/50 bg-brand-50 text-brand-700 dark:border-brand-500/40 dark:bg-brand-900/20 dark:text-brand-300'
                    : 'border-gray-200 text-gray-500 hover:border-gray-300 dark:border-gray-700 dark:text-gray-400'
                }`}
              >
                <input
                  type="checkbox"
                  checked={enabled}
                  onChange={(e) => setFeatures((prev) => ({ ...prev, [key]: e.target.checked }))}
                  className="accent-brand-600"
                />
                {FEATURE_LABELS[key]}
              </label>
            )
          })}
        </div>
      </div>

      <div className="mt-5 flex flex-wrap items-center gap-x-6 gap-y-2">
        <label className="flex cursor-pointer items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
          <input
            type="checkbox"
            checked={form.active}
            onChange={(e) => update('active', e.target.checked)}
            className="accent-brand-600"
          />
          Plan activo
        </label>
        <label className="flex cursor-pointer items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
          <input
            type="checkbox"
            checked={form.publicPlan}
            onChange={(e) => update('publicPlan', e.target.checked)}
            className="accent-brand-600"
          />
          Plan público
        </label>
      </div>

      <div className="mt-6 flex justify-end gap-3 border-t border-gray-100 pt-4 dark:border-gray-800">
        <Button variant="outline" onClick={onClose}>
          Cancelar
        </Button>
        <Button onClick={handleSave} loading={saving}>
          <Save size={16} /> {plan ? 'Guardar cambios' : 'Crear plan'}
        </Button>
      </div>
    </Modal>
  )
}
