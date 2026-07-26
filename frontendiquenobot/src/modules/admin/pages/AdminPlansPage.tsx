import { useState, useEffect } from 'react'
import { Plus, Edit2, Trash2, Check, X, Save, Loader } from 'lucide-react'
import { adminService, type PlanDto, type CreatePlanRequest } from '@/services/admin.service'

const FEATURE_LABELS: Record<string, string> = {
  whatsapp: 'WhatsApp',
  ai_assistant: 'Asistente IA',
  reports: 'Reportes',
  api_access: 'API Access',
  custom_branding: 'Branding Personalizado',
  multi_agent: 'Multi-Agente',
}

function parseFeatures(featuresStr: string | null | undefined): Record<string, boolean> {
  if (!featuresStr) return {}
  try { return JSON.parse(featuresStr) } catch { return {} }
}

function stringifyFeatures(features: Record<string, boolean>): string {
  return JSON.stringify(features)
}

export default function AdminPlansPage() {
  const [plans, setPlans] = useState<PlanDto[]>([])
  const [loading, setLoading] = useState(true)
  const [showModal, setShowModal] = useState(false)
  const [editingPlan, setEditingPlan] = useState<PlanDto | null>(null)

  useEffect(() => { loadPlans() }, [])

  const loadPlans = async () => {
    try {
      const res = await adminService.getPlans({ size: 50 })
      setPlans(res.content)
    } catch (e) { console.error(e) }
    finally { setLoading(false) }
  }

  const handleEdit = (plan: PlanDto) => { setEditingPlan(plan); setShowModal(true) }
  const handleCreate = () => { setEditingPlan(null); setShowModal(true) }

  const handleDelete = async (plan: PlanDto) => {
    if (!confirm(`¿Eliminar el plan "${plan.name}"?`)) return
    try {
      await adminService.deletePlan(plan.id)
      await loadPlans()
    } catch (e) { console.error(e) }
  }

  if (loading) return (
    <div className="flex h-64 items-center justify-center">
      <Loader className="h-8 w-8 animate-spin text-brand-600" />
    </div>
  )

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Planes</h1>
        <button onClick={handleCreate}
          className="flex items-center gap-2 rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">
          <Plus size={18} /> Nuevo Plan
        </button>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2 xl:grid-cols-4">
        {plans.map((plan) => {
          const features = parseFeatures(plan.features)
          return (
            <div key={plan.id} className="relative rounded-lg border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-950">
              {!plan.active && (
                <span className="absolute right-3 top-3 rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600 dark:bg-gray-800 dark:text-gray-400">Inactivo</span>
              )}
              <h3 className="text-lg font-bold text-gray-900 dark:text-white">{plan.name}</h3>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">{plan.description}</p>
              <div className="mt-4">
                <span className="text-3xl font-bold text-gray-900 dark:text-white">${plan.monthlyPrice ?? 0}</span>
                <span className="text-sm text-gray-500 dark:text-gray-400">/mes</span>
              </div>
              {(plan.yearlyPrice ?? 0) > 0 && (
                <p className="mt-1 text-xs text-gray-500">${plan.yearlyPrice}/año (ahorra ${((plan.monthlyPrice ?? 0) * 12 - (plan.yearlyPrice ?? 0)).toFixed(2)})</p>
              )}
              <div className="mt-4 space-y-2">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-gray-500">Usuarios</span>
                  <span className="font-medium text-gray-900 dark:text-white">{plan.maxUsers != null && plan.maxUsers >= 999999 ? 'Ilimitados' : plan.maxUsers ?? '-'}</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-gray-500">Conversaciones</span>
                  <span className="font-medium text-gray-900 dark:text-white">{plan.maxConversations != null && plan.maxConversations >= 999999 ? 'Ilimitadas' : plan.maxConversations?.toLocaleString() ?? '-'}</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-gray-500">Contactos</span>
                  <span className="font-medium text-gray-900 dark:text-white">{plan.maxContacts != null && plan.maxContacts >= 999999 ? 'Ilimitados' : plan.maxContacts?.toLocaleString() ?? '-'}</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-gray-500">Storage</span>
                  <span className="font-medium text-gray-900 dark:text-white">{plan.maxStorageMb != null && plan.maxStorageMb >= 10000 ? `${plan.maxStorageMb / 1000} GB` : plan.maxStorageMb != null ? `${plan.maxStorageMb} MB` : '-'}</span>
                </div>
              </div>
              <div className="mt-4 space-y-1.5">
                {Object.entries(FEATURE_LABELS).map(([key, label]) => (
                  <div key={key} className="flex items-center gap-2 text-sm">
                    {features[key] ? <Check size={14} className="text-green-500" /> : <X size={14} className="text-gray-300 dark:text-gray-600" />}
                    <span className={features[key] ? 'text-gray-700 dark:text-gray-300' : 'text-gray-400 dark:text-gray-500'}>{label}</span>
                  </div>
                ))}
              </div>
              <div className="mt-6 flex gap-2">
                <button onClick={() => handleEdit(plan)}
                  className="flex flex-1 items-center justify-center gap-2 rounded-lg border border-gray-200 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-800">
                  <Edit2 size={14} /> Editar
                </button>
                <button onClick={() => handleDelete(plan)}
                  className="flex items-center justify-center rounded-lg border border-red-200 px-3 py-2 text-sm font-medium text-red-600 hover:bg-red-50 dark:border-red-900 dark:text-red-400 dark:hover:bg-red-900/20">
                  <Trash2 size={14} />
                </button>
              </div>
            </div>
          )
        })}
      </div>

      {showModal && <PlanModal plan={editingPlan} onClose={() => { setShowModal(false); setEditingPlan(null) }} onSaved={loadPlans} />}
    </div>
  )
}

function PlanModal({ plan, onClose, onSaved }: { plan: PlanDto | null; onClose: () => void; onSaved: () => void }) {
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState<CreatePlanRequest>({
    name: plan?.name ?? '',
    code: plan?.code ?? '',
    description: plan?.description ?? '',
    monthlyPrice: plan?.monthlyPrice ?? 0,
    yearlyPrice: plan?.yearlyPrice ?? 0,
    maxUsers: plan?.maxUsers ?? 1,
    maxConversations: plan?.maxConversations ?? 100,
    maxContacts: plan?.maxContacts ?? 100,
    maxStorageMb: plan?.maxStorageMb ?? 50,
    features: plan?.features ?? stringifyFeatures(FEATURE_LABELS),
    active: plan?.active ?? true,
    publicPlan: plan?.publicPlan ?? true,
    sortOrder: plan?.sortOrder ?? 1,
  })

  const handleSave = async () => {
    setSaving(true)
    try {
      if (plan) await adminService.updatePlan(plan.id, form)
      else await adminService.createPlan(form)
      onSaved()
      onClose()
    } catch (e) { console.error(e) }
    finally { setSaving(false) }
  }

  const update = (key: string, value: string | number | boolean) => setForm({ ...form, [key]: value })

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={onClose}>
      <div className="w-full max-w-2xl rounded-lg bg-white p-6 shadow-xl dark:bg-gray-950 max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h2 className="text-lg font-bold text-gray-900 dark:text-white">{plan ? 'Editar Plan' : 'Nuevo Plan'}</h2>

        <div className="mt-6 grid grid-cols-2 gap-4">
          <Field label="Nombre" value={form.name} onChange={(v) => update('name', v)} />
          <Field label="Código" value={form.code} onChange={(v) => update('code', v)} />
          <div className="col-span-2">
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Descripción</label>
            <textarea value={form.description} onChange={(e) => update('description', e.target.value)}
              className="mt-1 w-full rounded-lg border border-gray-200 px-3 py-2 text-sm dark:border-gray-700 dark:bg-gray-900 dark:text-white" rows={2} />
          </div>
          <Field label="Precio mensual ($)" type="number" value={form.monthlyPrice} onChange={(v) => update('monthlyPrice', Number(v))} />
          <Field label="Precio anual ($)" type="number" value={form.yearlyPrice} onChange={(v) => update('yearlyPrice', Number(v))} />
          <Field label="Max usuarios" type="number" value={form.maxUsers} onChange={(v) => update('maxUsers', Number(v))} />
          <Field label="Max conversaciones" type="number" value={form.maxConversations} onChange={(v) => update('maxConversations', Number(v))} />
          <Field label="Max contactos" type="number" value={form.maxContacts} onChange={(v) => update('maxContacts', Number(v))} />
          <Field label="Max storage (MB)" type="number" value={form.maxStorageMb} onChange={(v) => update('maxStorageMb', Number(v))} />
          <Field label="Orden" type="number" value={form.sortOrder ?? 1} onChange={(v) => update('sortOrder', Number(v))} />
          <div className="flex items-center gap-4">
            <label className="flex items-center gap-2">
              <input type="checkbox" checked={form.active} onChange={(e) => update('active', e.target.checked)} className="rounded" />
              <span className="text-sm text-gray-700 dark:text-gray-300">Activo</span>
            </label>
            <label className="flex items-center gap-2">
              <input type="checkbox" checked={form.publicPlan} onChange={(e) => update('publicPlan', e.target.checked)} className="rounded" />
              <span className="text-sm text-gray-700 dark:text-gray-300">Plan público</span>
            </label>
          </div>
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <button onClick={onClose} className="rounded-lg border border-gray-200 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300">Cancelar</button>
          <button onClick={handleSave} disabled={saving}
            className="flex items-center gap-2 rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-50">
            {saving ? <Loader className="h-4 w-4 animate-spin" /> : <Save size={16} />}
            {plan ? 'Actualizar' : 'Crear'}
          </button>
        </div>
      </div>
    </div>
  )
}

function Field({ label, value, onChange, type = 'text' }: { label: string; value: string | number; onChange: (v: string | number) => void; type?: string }) {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">{label}</label>
      <input type={type} value={value} onChange={(e) => onChange(type === 'number' ? Number(e.target.value) : e.target.value)}
        className="mt-1 w-full rounded-lg border border-gray-200 px-3 py-2 text-sm dark:border-gray-700 dark:bg-gray-900 dark:text-white" />
    </div>
  )
}
