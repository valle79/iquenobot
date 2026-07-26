import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Building2, Globe, Mail, Phone, Lock, ArrowLeft, ArrowRight, Check } from 'lucide-react'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { Input } from '@/shared/atoms/Input/Input'
import { Button } from '@/shared/atoms/Button/Button'
import { useCreateTenant } from '../hooks/useAdminTenants'

const companySchema = z.object({
  companyName: z.string().min(2, 'Mínimo 2 caracteres').max(200),
  subdomain: z.string().min(2, 'Mínimo 2 caracteres').max(50).regex(/^[a-z0-9-]+$/, 'Solo minúsculas, números y guiones'),
  contactEmail: z.string().email('Email inválido'),
  contactPhone: z.string().optional(),
  websiteUrl: z.string().optional(),
})

const adminSchema = z.object({
  adminFirstName: z.string().min(2, 'Mínimo 2 caracteres').max(100),
  adminLastName: z.string().min(2, 'Mínimo 2 caracteres').max(100),
  adminEmail: z.string().email('Email inválido'),
  adminPassword: z.string().min(8, 'Mínimo 8 caracteres').regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, 'Debe tener mayúscula, minúscula y número'),
  adminPhone: z.string().optional(),
})

type CompanyForm = z.infer<typeof companySchema>
type AdminForm = z.infer<typeof adminSchema>

interface CreateTenantModalProps {
  open: boolean
  onClose: () => void
}

export function CreateTenantModal({ open, onClose }: CreateTenantModalProps) {
  const [step, setStep] = useState(0)
  const [companyData, setCompanyData] = useState<CompanyForm | null>(null)
  const createTenant = useCreateTenant()

  const companyForm = useForm<CompanyForm>({ resolver: zodResolver(companySchema) })
  const adminForm = useForm<AdminForm>({ resolver: zodResolver(adminSchema) })

  const handleClose = () => {
    setStep(0)
    setCompanyData(null)
    companyForm.reset()
    adminForm.reset()
    onClose()
  }

  const onCompanySubmit = (data: CompanyForm) => {
    setCompanyData(data)
    setStep(1)
  }

  const onAdminSubmit = async (data: AdminForm) => {
    if (!companyData) return
    try {
      await createTenant.mutateAsync({
        ...companyData,
        contactPhone: companyData.contactPhone || undefined,
        websiteUrl: companyData.websiteUrl || undefined,
        ...data,
        adminPhone: data.adminPhone || undefined,
      })
      handleClose()
    } catch {
      // handled by mutation
    }
  }

  return (
    <Modal open={open} onClose={handleClose} title="Crear empresa" description="Registra una nueva empresa en el sistema" size="lg">
      {/* Steps indicator */}
      <div className="mb-6 flex items-center justify-center gap-2">
        <div className={`flex h-8 w-8 items-center justify-center rounded-full text-sm font-medium ${
          step >= 0 ? 'bg-brand-600 text-white' : 'bg-gray-200 text-gray-500'
        }`}>1</div>
        <div className={`h-0.5 w-12 ${step >= 1 ? 'bg-brand-600' : 'bg-gray-200'}`} />
        <div className={`flex h-8 w-8 items-center justify-center rounded-full text-sm font-medium ${
          step >= 1 ? 'bg-brand-600 text-white' : 'bg-gray-200 text-gray-500'
        }`}>2</div>
      </div>

      {step === 0 && (
        <form onSubmit={companyForm.handleSubmit(onCompanySubmit)} className="space-y-4">
          <Input label="Nombre de la empresa" placeholder="Ej: Mi Empresa S.A.S" leftIcon={<Building2 size={16} />}
            error={companyForm.formState.errors.companyName?.message} {...companyForm.register('companyName')} />
          <Input label="Subdominio" placeholder="mi-empresa" leftIcon={<Globe size={16} />}
            error={companyForm.formState.errors.subdomain?.message} {...companyForm.register('subdomain')} />
          <Input label="Email de contacto" type="email" placeholder="contacto@miempresa.com" leftIcon={<Mail size={16} />}
            error={companyForm.formState.errors.contactEmail?.message} {...companyForm.register('contactEmail')} />
          <Input label="Teléfono (opcional)" placeholder="+573001234567" leftIcon={<Phone size={16} />}
            error={companyForm.formState.errors.contactPhone?.message} {...companyForm.register('contactPhone')} />
          <Input label="Sitio web (opcional)" placeholder="https://miempresa.com" leftIcon={<Globe size={16} />}
            error={companyForm.formState.errors.websiteUrl?.message} {...companyForm.register('websiteUrl')} />
          <div className="flex justify-end gap-3 pt-4">
            <Button type="button" variant="outline" onClick={handleClose}>Cancelar</Button>
            <Button type="submit">Siguiente <ArrowRight size={18} /></Button>
          </div>
        </form>
      )}

      {step === 1 && (
        <form onSubmit={adminForm.handleSubmit(onAdminSubmit)} className="space-y-4">
          <div className="rounded-lg bg-brand-50 px-4 py-3 text-sm text-brand-700 dark:bg-brand-900/20 dark:text-brand-400">
            Creando: <strong>{companyData?.companyName}</strong>
            <span className="ml-2 rounded-md bg-brand-100 px-2 py-0.5 text-xs dark:bg-brand-900/40">@{companyData?.subdomain}</span>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input label="Nombre" placeholder="Juan" error={adminForm.formState.errors.adminFirstName?.message} {...adminForm.register('adminFirstName')} />
            <Input label="Apellido" placeholder="Pérez" error={adminForm.formState.errors.adminLastName?.message} {...adminForm.register('adminLastName')} />
          </div>
          <Input label="Email del admin" type="email" placeholder="admin@miempresa.com" leftIcon={<Mail size={16} />}
            error={adminForm.formState.errors.adminEmail?.message} {...adminForm.register('adminEmail')} />
          <Input label="Contraseña" type="password" placeholder="Mín. 8 caracteres" leftIcon={<Lock size={16} />}
            error={adminForm.formState.errors.adminPassword?.message} {...adminForm.register('adminPassword')} />
          <Input label="Teléfono del admin (opcional)" placeholder="+573001234567" leftIcon={<Phone size={16} />}
            error={adminForm.formState.errors.adminPhone?.message} {...adminForm.register('adminPhone')} />
          <div className="flex justify-between pt-4">
            <Button type="button" variant="ghost" onClick={() => setStep(0)}><ArrowLeft size={18} /> Atrás</Button>
            <Button type="submit" loading={createTenant.isPending}><Building2 size={18} /> Crear empresa</Button>
          </div>
        </form>
      )}
    </Modal>
  )
}
