import { useEffect, useRef, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  Building2,
  Check,
  CheckCircle2,
  Copy,
  Crown,
  Globe,
  Loader2,
  Lock,
  Mail,
  Phone,
  ShieldCheck,
  Users,
} from 'lucide-react'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { Input } from '@/shared/atoms/Input/Input'
import { Button } from '@/shared/atoms/Button/Button'
import { useCreateTenant } from '../hooks/useAdminTenants'
import { adminService, type PlanDto } from '@/services/admin.service'

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
  try {
    return JSON.parse(featuresStr)
  } catch {
    return {}
  }
}

type UniqueField = 'companyName' | 'subdomain' | 'websiteUrl'

const uniqueErrorMessages: Record<UniqueField, string> = {
  companyName: 'Este nombre ya está en uso por otra empresa',
  subdomain: 'Este subdominio ya está en uso',
  websiteUrl: 'Este sitio web ya está registrado',
}

const companySchema = z
  .object({
    companyName: z.string().min(2, 'Mínimo 2 caracteres').max(200),
    subdomain: z
      .string()
      .min(2, 'Mínimo 2 caracteres')
      .max(50)
      .regex(/^[a-z0-9-]+$/, 'Solo minúsculas, números y guiones'),
    contactEmail: z.string().email('Email inválido'),
    contactPhone: z.string().optional(),
    websiteUrl: z.union([z.string().url('Ingresa una URL válida'), z.literal('')]).optional(),
  })
  .superRefine(async (values, ctx) => {
    if (!values.companyName || !values.subdomain) return
    const result = await adminService.checkTenantUniqueness({
      companyName: values.companyName || undefined,
      subdomain: values.subdomain || undefined,
      websiteUrl: values.websiteUrl || undefined,
    })
    if (!result.companyNameAvailable) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['companyName'],
        message: uniqueErrorMessages.companyName,
      })
    }
    if (!result.subdomainAvailable) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['subdomain'],
        message: uniqueErrorMessages.subdomain,
      })
    }
    if (values.websiteUrl && !result.websiteUrlAvailable) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['websiteUrl'],
        message: uniqueErrorMessages.websiteUrl,
      })
    }
  })

const limitsSchema = z.object({
  maxAgents: z.preprocess(
    (v) => (v === '' || v === undefined ? undefined : Number(v)),
    z
      .number({ invalid_type_error: 'Debe ser un número' })
      .int('Debe ser un número entero')
      .min(0, 'Mínimo 0')
      .max(100000, 'Máximo 100000')
      .optional(),
  ),
  maxSupervisors: z.preprocess(
    (v) => (v === '' || v === undefined ? undefined : Number(v)),
    z
      .number({ invalid_type_error: 'Debe ser un número' })
      .int('Debe ser un número entero')
      .min(0, 'Mínimo 0')
      .max(100000, 'Máximo 100000')
      .optional(),
  ),
})

const adminSchema = z.object({
  adminFirstName: z.string().min(2, 'Mínimo 2 caracteres').max(100),
  adminLastName: z.string().min(2, 'Mínimo 2 caracteres').max(100),
  adminEmail: z.string().email('Email inválido'),
  adminPassword: z
    .string()
    .min(8, 'Mínimo 8 caracteres')
    .regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, 'Debe tener mayúscula, minúscula y número'),
  adminPhone: z.string().optional(),
})

type CompanyForm = z.infer<typeof companySchema>
type LimitsForm = z.infer<typeof limitsSchema>
type AdminForm = z.infer<typeof adminSchema>

interface CreatedCredentials {
  companyName: string
  subdomain: string
  websiteUrl?: string
  email: string
  password: string
}

interface CreateTenantModalProps {
  open: boolean
  onClose: () => void
}

const steps = ['Empresa', 'Plan', 'Límites', 'Administrador']

export function CreateTenantModal({ open, onClose }: CreateTenantModalProps) {
  const [step, setStep] = useState(0)
  const [companyData, setCompanyData] = useState<CompanyForm | null>(null)
  const [availability, setAvailability] = useState<
    Partial<Record<UniqueField, boolean>>
  >({})
  const [checkingField, setCheckingField] = useState<UniqueField | null>(null)
  const [created, setCreated] = useState<CreatedCredentials | null>(null)
  const [copied, setCopied] = useState<string | null>(null)
  const [plans, setPlans] = useState<PlanDto[]>([])
  const [selectedPlan, setSelectedPlan] = useState<PlanDto | null>(null)
  const [loadingPlans, setLoadingPlans] = useState(false)
  const checkSeq = useRef(0)
  const createTenant = useCreateTenant()

  const companyForm = useForm<CompanyForm>({ resolver: zodResolver(companySchema) })
  const limitsForm = useForm<LimitsForm>({ resolver: zodResolver(limitsSchema) })
  const adminForm = useForm<AdminForm>({ resolver: zodResolver(adminSchema) })

  useEffect(() => {
    if (!open) return
    let cancelled = false
    setLoadingPlans(true)
    adminService
      .getPlans({ size: 50 })
      .then((res) => {
        if (cancelled) return
        const active = res.content.filter((p) => p.active)
        setPlans(active)
        setSelectedPlan(
          (prev) =>
            prev ?? active.find((p) => p.code === 'professional') ?? active[0] ?? null,
        )
      })
      .catch(() => {})
      .finally(() => {
        if (!cancelled) setLoadingPlans(false)
      })
    return () => {
      cancelled = true
    }
  }, [open])

  const handleClose = () => {
    setStep(0)
    setCompanyData(null)
    setAvailability({})
    setCheckingField(null)
    setCreated(null)
    setCopied(null)
    setPlans([])
    setSelectedPlan(null)
    checkSeq.current++
    companyForm.reset()
    limitsForm.reset()
    adminForm.reset()
    onClose()
  }

  const checkUniqueField = async (field: UniqueField) => {
    const value = (companyForm.getValues(field) ?? '').trim()
    if (!value) {
      setAvailability((prev) => ({ ...prev, [field]: undefined }))
      return
    }
    const seq = ++checkSeq.current
    setCheckingField(field)
    try {
      const result = await adminService.checkTenantUniqueness({ [field]: value })
      if (seq !== checkSeq.current) return
      setAvailability((prev) => ({ ...prev, [field]: result[`${field}Available`] }))
      if (result[`${field}Available`]) {
        companyForm.clearErrors(field)
      } else {
        companyForm.setError(field, { message: uniqueErrorMessages[field] })
      }
    } catch {
      // Si la verificación falla, la validación del submit la vuelve a intentar
    } finally {
      if (seq === checkSeq.current) setCheckingField(null)
    }
  }

  const availabilityIcon = (field: UniqueField) => {
    if (checkingField === field) return <Loader2 size={16} className="animate-spin text-gray-400" />
    if (availability[field] === true) return <Check size={16} className="text-green-500" />
    if (availability[field] === false) return <AlertCircle size={16} className="text-red-500" />
    return undefined
  }

  const onCompanySubmit = (data: CompanyForm) => {
    setCompanyData(data)
    setStep(1)
  }

  const onAdminSubmit = async (data: AdminForm) => {
    if (!companyData) return
    const limits = limitsForm.getValues()
    try {
      await createTenant.mutateAsync({
        ...companyData,
        contactPhone: companyData.contactPhone || undefined,
        websiteUrl: companyData.websiteUrl || undefined,
        subscriptionPlan: selectedPlan?.code,
        maxAgents: limits.maxAgents,
        maxSupervisors: limits.maxSupervisors,
        ...data,
        adminPhone: data.adminPhone || undefined,
      })
      setCreated({
        companyName: companyData.companyName,
        subdomain: companyData.subdomain,
        websiteUrl: companyData.websiteUrl,
        email: data.adminEmail,
        password: data.adminPassword,
      })
    } catch {
      // handled by mutation
    }
  }

  const copyToClipboard = async (key: string, value: string) => {
    try {
      await navigator.clipboard.writeText(value)
      setCopied(key)
      setTimeout(() => setCopied((current) => (current === key ? null : current)), 2000)
    } catch {
      // portapapeles no disponible
    }
  }

  const renderCopyButton = (value: string, label: string) => (
    <Button
      type="button"
      variant="ghost"
      size="sm"
      icon
      title="Copiar"
      onClick={() => copyToClipboard(label, value)}
    >
      {copied === label ? <Check size={15} className="text-green-500" /> : <Copy size={15} />}
    </Button>
  )

  return (
    <Modal
      open={open}
      onClose={handleClose}
      title={created ? 'Empresa creada' : 'Crear empresa'}
      description={
        created
          ? 'Entrega estas credenciales al administrador de la empresa'
          : 'Registra una nueva empresa en el sistema'
      }
      size="lg"
    >
      {created ? (
        <div className="space-y-5">
          <div className="flex flex-col items-center gap-2 py-2 text-center">
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-green-100 dark:bg-green-900/30">
              <CheckCircle2 size={30} className="text-green-600 dark:text-green-400" />
            </div>
            <p className="text-sm text-gray-600 dark:text-gray-300">
              La empresa <strong>{created.companyName}</strong> fue creada correctamente. Guarda
              las credenciales del administrador en un lugar seguro.
            </p>
          </div>

          <div className="rounded-lg border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-900/60">
            <p className="mb-2 text-xs font-semibold tracking-wider text-gray-500 uppercase">
              Empresa
            </p>
            <div className="space-y-1.5 text-sm">
              <p className="text-gray-900 dark:text-gray-100">{created.companyName}</p>
              <p className="text-gray-500">
                Acceso: <span className="font-mono text-gray-700 dark:text-gray-300">@{created.subdomain}</span>
              </p>
              {created.websiteUrl && (
                <p className="truncate text-gray-500">
                  Sitio web: <span className="text-gray-700 dark:text-gray-300">{created.websiteUrl}</span>
                </p>
              )}
            </div>
          </div>

          <div className="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
            <p className="mb-3 text-xs font-semibold tracking-wider text-gray-500 uppercase">
              Credenciales del administrador
            </p>
            <div className="space-y-3">
              <div className="flex items-center justify-between gap-3">
                <div className="min-w-0">
                  <p className="text-xs text-gray-500">Email</p>
                  <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">
                    {created.email}
                  </p>
                </div>
                {renderCopyButton(created.email, 'email')}
              </div>
              <div className="flex items-center justify-between gap-3">
                <div className="min-w-0">
                  <p className="text-xs text-gray-500">Contraseña</p>
                  <p className="truncate font-mono text-sm font-medium text-gray-900 dark:text-gray-100">
                    {created.password}
                  </p>
                </div>
                {renderCopyButton(created.password, 'password')}
              </div>
            </div>
          </div>

          <div className="rounded-lg bg-amber-50 px-4 py-3 text-xs text-amber-700 dark:bg-amber-900/20 dark:text-amber-400">
            El administrador deberá cambiar su contraseña al iniciar sesión por primera vez.
          </div>

          <Button className="w-full" onClick={handleClose}>
            <Check size={18} />
            Listo
          </Button>
        </div>
      ) : (
        <>
          {/* Steps indicator */}
          <div className="mb-6 flex items-center justify-center gap-2">
            {steps.map((label, index) => (
              <div key={label} className="flex items-center gap-2">
                {index > 0 && (
                  <div className={`h-0.5 w-10 ${step >= index ? 'bg-brand-600' : 'bg-gray-200'}`} />
                )}
                <div className="flex flex-col items-center gap-1">
                  <div
                    className={`flex h-8 w-8 items-center justify-center rounded-full text-sm font-medium ${
                      step >= index ? 'bg-brand-600 text-white' : 'bg-gray-200 text-gray-500'
                    }`}
                  >
                    {step > index ? <Check size={16} /> : index + 1}
                  </div>
                  <span
                    className={`text-[11px] font-medium ${step >= index ? 'text-brand-600' : 'text-gray-400'}`}
                  >
                    {label}
                  </span>
                </div>
              </div>
            ))}
          </div>

          {step === 0 && (
            <form onSubmit={companyForm.handleSubmit(onCompanySubmit)} className="space-y-4">
              <Input
                label="Nombre de la empresa"
                placeholder="Ej: Mi Empresa S.A.S"
                leftIcon={<Building2 size={16} />}
                rightIcon={availabilityIcon('companyName')}
                helperText={availability.companyName === true ? 'Disponible' : undefined}
                error={companyForm.formState.errors.companyName?.message}
                {...companyForm.register('companyName')}
                onBlur={() => checkUniqueField('companyName')}
              />
              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="Subdominio"
                  placeholder="mi-empresa"
                  leftIcon={<Globe size={16} />}
                  rightIcon={availabilityIcon('subdomain')}
                  helperText={availability.subdomain === true ? 'Disponible' : undefined}
                  error={companyForm.formState.errors.subdomain?.message}
                  {...companyForm.register('subdomain')}
                  onBlur={() => checkUniqueField('subdomain')}
                />
                <Input
                  label="Email de contacto"
                  type="email"
                  placeholder="contacto@miempresa.com"
                  leftIcon={<Mail size={16} />}
                  error={companyForm.formState.errors.contactEmail?.message}
                  {...companyForm.register('contactEmail')}
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="Teléfono (opcional)"
                  placeholder="+573001234567"
                  leftIcon={<Phone size={16} />}
                  error={companyForm.formState.errors.contactPhone?.message}
                  {...companyForm.register('contactPhone')}
                />
                <Input
                  label="Sitio web (opcional)"
                  placeholder="https://miempresa.com"
                  leftIcon={<Globe size={16} />}
                  rightIcon={availabilityIcon('websiteUrl')}
                  helperText={availability.websiteUrl === true ? 'Disponible' : undefined}
                  error={companyForm.formState.errors.websiteUrl?.message}
                  {...companyForm.register('websiteUrl')}
                  onBlur={() => checkUniqueField('websiteUrl')}
                />
              </div>
              <div className="flex justify-end gap-3 pt-4">
                <Button type="button" variant="outline" onClick={handleClose}>
                  Cancelar
                </Button>
                <Button type="submit">
                  Siguiente <ArrowRight size={18} />
                </Button>
              </div>
            </form>
          )}

          {step === 1 && (
            <div className="space-y-4">
              <div className="rounded-lg bg-brand-50 px-4 py-3 text-sm text-brand-700 dark:bg-brand-900/20 dark:text-brand-400">
                Creando: <strong>{companyData?.companyName}</strong>
                <span className="ml-2 rounded-md bg-brand-100 px-2 py-0.5 text-xs dark:bg-brand-900/40">
                  @{companyData?.subdomain}
                </span>
              </div>
              <div className="rounded-lg border border-gray-200 bg-gray-50 px-4 py-3 text-xs leading-relaxed text-gray-600 dark:border-gray-700 dark:bg-gray-900/60 dark:text-gray-400">
                Elige el plan que se activará en esta empresa. El administrador podrá gestionar
                sus límites y funcionalidades según el plan seleccionado.
              </div>
              {loadingPlans ? (
                <div className="flex h-48 items-center justify-center">
                  <Loader2 className="h-8 w-8 animate-spin text-brand-600" />
                </div>
              ) : plans.length === 0 ? (
                <div className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700 dark:border-amber-900 dark:bg-amber-900/20 dark:text-amber-400">
                  No hay planes activos disponibles. La empresa se creará sin plan asignado.
                </div>
              ) : (
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                  {plans.map((plan) => {
                    const selected = selectedPlan?.id === plan.id
                    const features = parseFeatures(plan.features)
                    const featured = plan.code === 'professional'
                    return (
                      <button
                        key={plan.id}
                        type="button"
                        onClick={() => setSelectedPlan(plan)}
                        className={`relative rounded-xl border-2 p-4 text-left transition-all ${
                          selected
                            ? 'border-brand-600 bg-brand-50/60 shadow-sm dark:bg-brand-900/20'
                            : 'border-gray-200 hover:border-gray-300 dark:border-gray-700 dark:hover:border-gray-600'
                        }`}
                      >
                        {selected && (
                          <span className="absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full bg-brand-600 text-white shadow">
                            <Check size={14} />
                          </span>
                        )}
                        <div className="flex items-center gap-1.5">
                          {featured && <Crown size={14} className="text-brand-600" />}
                          <p className="font-semibold text-gray-900 dark:text-white">
                            {plan.name}
                          </p>
                        </div>
                        <p className="mt-1 text-lg font-bold text-gray-900 dark:text-white">
                          S/ {Number(plan.monthlyPrice ?? 0).toFixed(2)}
                          <span className="text-xs font-normal text-gray-500 dark:text-gray-400">
                            /mes
                          </span>
                        </p>
                        <div className="mt-3 space-y-1 border-t border-gray-100 pt-2 dark:border-gray-800">
                          {Object.entries(FEATURE_LABELS).map(([key, label]) => (
                            <div key={key} className="flex items-center gap-1.5 text-xs">
                              {features[key] ? (
                                <Check size={12} className="shrink-0 text-green-500" />
                              ) : (
                                <span className="h-3 w-3 shrink-0 rounded-full border border-gray-300 dark:border-gray-600" />
                              )}
                              <span
                                className={
                                  features[key]
                                    ? 'text-gray-700 dark:text-gray-300'
                                    : 'text-gray-400 dark:text-gray-500'
                                }
                              >
                                {label}
                              </span>
                            </div>
                          ))}
                        </div>
                      </button>
                    )
                  })}
                </div>
              )}
              <div className="flex justify-between pt-4">
                <Button type="button" variant="ghost" onClick={() => setStep(0)}>
                  <ArrowLeft size={18} /> Atrás
                </Button>
                <Button
                  onClick={() => setStep(2)}
                  disabled={plans.length > 0 && !selectedPlan}
                >
                  Siguiente <ArrowRight size={18} />
                </Button>
              </div>
            </div>
          )}

          {step === 2 && (
            <form onSubmit={limitsForm.handleSubmit(() => setStep(3))} className="space-y-4">
              <div className="rounded-lg bg-brand-50 px-4 py-3 text-sm text-brand-700 dark:bg-brand-900/20 dark:text-brand-400">
                Creando: <strong>{companyData?.companyName}</strong>
                <span className="ml-2 rounded-md bg-brand-100 px-2 py-0.5 text-xs dark:bg-brand-900/40">
                  @{companyData?.subdomain}
                </span>
                {selectedPlan && (
                  <span className="ml-2 rounded-md bg-brand-100 px-2 py-0.5 text-xs dark:bg-brand-900/40">
                    Plan: {selectedPlan.name}
                  </span>
                )}
              </div>
              <div className="rounded-lg border border-gray-200 bg-gray-50 px-4 py-3 text-xs leading-relaxed text-gray-600 dark:border-gray-700 dark:bg-gray-900/60 dark:text-gray-400">
                Define cuántos agentes y supervisores podrá crear el administrador de esta empresa.
                Deja el campo vacío para no aplicar límite.
              </div>
              <div className="grid grid-cols-2 gap-4">
                <Input
                  type="number"
                  min={0}
                  max={100000}
                  label="Máximo de agentes"
                  placeholder="Sin límite"
                  leftIcon={<Users size={16} />}
                  error={limitsForm.formState.errors.maxAgents?.message}
                  {...limitsForm.register('maxAgents')}
                />
                <Input
                  type="number"
                  min={0}
                  max={100000}
                  label="Máximo de supervisores"
                  placeholder="Sin límite"
                  leftIcon={<ShieldCheck size={16} />}
                  error={limitsForm.formState.errors.maxSupervisors?.message}
                  {...limitsForm.register('maxSupervisors')}
                />
              </div>
              <div className="flex justify-between pt-4">
                <Button type="button" variant="ghost" onClick={() => setStep(1)}>
                  <ArrowLeft size={18} /> Atrás
                </Button>
                <Button type="submit">
                  Siguiente <ArrowRight size={18} />
                </Button>
              </div>
            </form>
          )}

          {step === 3 && (
            <form onSubmit={adminForm.handleSubmit(onAdminSubmit)} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="Nombre"
                  placeholder="Juan"
                  error={adminForm.formState.errors.adminFirstName?.message}
                  {...adminForm.register('adminFirstName')}
                />
                <Input
                  label="Apellido"
                  placeholder="Pérez"
                  error={adminForm.formState.errors.adminLastName?.message}
                  {...adminForm.register('adminLastName')}
                />
              </div>
              <Input
                label="Email del admin"
                type="email"
                placeholder="admin@miempresa.com"
                leftIcon={<Mail size={16} />}
                error={adminForm.formState.errors.adminEmail?.message}
                {...adminForm.register('adminEmail')}
              />
              <Input
                label="Contraseña"
                type="password"
                placeholder="Mín. 8 caracteres"
                leftIcon={<Lock size={16} />}
                error={adminForm.formState.errors.adminPassword?.message}
                {...adminForm.register('adminPassword')}
              />
              <Input
                label="Teléfono del admin (opcional)"
                placeholder="+573001234567"
                leftIcon={<Phone size={16} />}
                error={adminForm.formState.errors.adminPhone?.message}
                {...adminForm.register('adminPhone')}
              />
              <div className="flex justify-between pt-4">
                <Button type="button" variant="ghost" onClick={() => setStep(2)}>
                  <ArrowLeft size={18} /> Atrás
                </Button>
                <Button type="submit" loading={createTenant.isPending}>
                  <Building2 size={18} /> Crear empresa
                </Button>
              </div>
            </form>
          )}
        </>
      )}
    </Modal>
  )
}
