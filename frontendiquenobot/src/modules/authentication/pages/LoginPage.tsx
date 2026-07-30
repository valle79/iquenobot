
import { useTranslation } from 'react-i18next'
import { Button } from '@/shared/atoms/Button/Button'
import { Input } from '@/shared/atoms/Input/Input'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/core/auth/auth.store'
import { toast } from 'sonner'
import { useState } from 'react'
import {
  LogIn,
  Mail,
  Lock,
  Eye,
  EyeOff,
  ShieldCheck,
  ArrowRight,
  Building2,
  X,
  Phone,
  Mail as MailIcon,
  UserCog,
} from 'lucide-react'

const loginSchema = z.object({
  email: z
    .string()
    .min(1, 'El correo es obligatorio')
    .email('Ingresa un correo electrónico válido'),

  password: z
    .string()
    .min(1, 'La contraseña es obligatoria')
    .min(6, 'La contraseña debe tener al menos 6 caracteres'),
})

type LoginForm = z.infer<typeof loginSchema>

export default function LoginPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const login = useAuthStore((state) => state.login)

  const [loading, setLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [rememberMe, setRememberMe] = useState(false)
  const [showForgotModal, setShowForgotModal] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors, isValid },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    mode: 'onChange',
  })

  const onSubmit = async (data: LoginForm) => {
    setLoading(true)

    try {
      await login(data)

      toast.success('¡Bienvenido de nuevo!', {
        description: 'Has iniciado sesión correctamente',
      })

      navigate('/dashboard', { replace: true })
    } catch (error: any) {
      const message =
        error?.response?.data?.message ||
        error?.message ||
        'No fue posible iniciar sesión. Verifica tus credenciales e inténtalo nuevamente.'

      toast.error('Acceso denegado', {
        description: message,
      })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen w-full bg-slate-50 dark:bg-slate-950">
      {/* ==================== PANEL IZQUIERDO ==================== */}
      <div className="relative hidden w-[48%] overflow-hidden lg:block">
        <img
          src="https://images.unsplash.com/photo-1551434678-e076c223a692?q=80&w=2070&auto=format&fit=crop"
          alt="Plataforma empresarial"
          className="absolute inset-0 h-full w-full object-cover"
        />

        <div className="absolute inset-0 bg-gradient-to-br from-slate-950/90 via-slate-900/80 to-indigo-950/70" />

        <div className="absolute -left-20 top-1/4 h-72 w-72 rounded-full bg-indigo-500/20 blur-3xl" />
        <div className="absolute -right-20 bottom-1/4 h-72 w-72 rounded-full bg-blue-500/15 blur-3xl" />

<div className="relative z-10 flex h-full flex-col justify-between p-12 xl:p-16 text-white">
  {/* Logo */}
  <div className="flex items-center gap-3">
    <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-white/10 backdrop-blur-md ring-1 ring-white/20">
      <ShieldCheck className="h-5 w-5" />
    </div>

    <div>
      <p className="text-lg font-semibold tracking-tight">LuKaBot</p>

    </div>
  </div>

  {/* Contenido principal */}
  <div className="max-w-md">
    <div className="mb-6 inline-flex items-center gap-2 rounded-full bg-white/10 px-3.5 py-1.5 text-sm font-medium text-white/90 backdrop-blur-sm ring-1 ring-white/15">
      <Building2 className="h-3.5 w-3.5" />
      Plataforma empresarial inteligente
    </div>

    <h2 className="text-4xl font-bold leading-[1.15] tracking-tight xl:text-[2.75rem]">
      Gestiona tu negocio
      <br />
      con inteligencia
    </h2>

    <p className="mt-5 text-lg leading-relaxed text-slate-300">
      Automatiza conversaciones, centraliza tus canales de atención y
      optimiza la relación con tus clientes desde una plataforma moderna,
      segura y diseñada para mejorar la productividad de tu equipo.
    </p>

    <div className="mt-10 flex flex-wrap items-center gap-6 text-sm text-slate-400">
      <div className="flex items-center gap-2">
        <div className="h-1.5 w-1.5 rounded-full bg-emerald-400" />
        Seguridad avanzada
      </div>

      <div className="flex items-center gap-2">
        <div className="h-1.5 w-1.5 rounded-full bg-emerald-400" />
        Asistencia impulsada por IA
      </div>
    </div>
  </div>

  {/* Footer */}
  <div className="space-y-1 text-sm text-slate-500">
    <p>© {new Date().getFullYear()} LuKaBot</p>
    <p className="text-slate-400">
      Diseñado y desarrollado por
      <span className="font-medium text-white/80"> Luis Valle</span>
    </p>
  </div>
</div>
</div>

      {/* ==================== PANEL DERECHO ==================== */}
      <div className="flex w-full flex-col justify-center px-6 py-12 sm:px-10 lg:w-[52%] lg:px-16 xl:px-24">
        <div className="mx-auto w-full max-w-[420px]">
          <div className="mb-9">
            <h1 className="text-3xl font-bold tracking-tight text-slate-900 dark:text-white">
              {t('auth.login') || 'Iniciar sesión'}
            </h1>

            <p className="mt-2.5 text-[15px] leading-relaxed text-slate-500 dark:text-slate-400">
              Ingresa tus credenciales para acceder a la plataforma
            </p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <Input
              label="Correo electrónico"
              placeholder="nombre@empresa.com"
              type="email"
              autoComplete="email"
              leftIcon={<Mail className="h-4 w-4" />}
              error={errors.email?.message}
              {...register('email')}
            />

            <div className="relative">
              <Input
                label="Contraseña"
                type={showPassword ? 'text' : 'password'}
                placeholder="••••••••"
                autoComplete="current-password"
                leftIcon={<Lock className="h-4 w-4" />}
                error={errors.password?.message}
                {...register('password')}
              />

              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-[38px] text-slate-400 transition hover:text-slate-600 dark:hover:text-slate-300"
                tabIndex={-1}
                aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
              >
                {showPassword ? (
                  <EyeOff className="h-4 w-4" />
                ) : (
                  <Eye className="h-4 w-4" />
                )}
              </button>
            </div>

            <div className="flex items-center justify-between pt-0.5">
              <label className="flex cursor-pointer items-center gap-2.5 select-none">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500 dark:border-slate-600 dark:bg-slate-800"
                />

                <span className="text-sm text-slate-600 dark:text-slate-400">
                  Mantener sesión iniciada
                </span>
              </label>

              <button
                type="button"
                onClick={() => setShowForgotModal(true)}
                className="text-sm font-medium text-brand-600 transition-colors hover:text-brand-700 dark:text-brand-400 dark:hover:text-brand-300"
              >
                ¿Olvidaste tu contraseña?
              </button>
            </div>

            <Button
              type="submit"
              loading={loading}
              disabled={loading || !isValid}
              className="mt-2 w-full gap-2"
              size="lg"
            >
              {loading ? (
                'Verificando...'
              ) : (
                <>
                  <LogIn className="h-4 w-4" />
                  Iniciar sesión
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </Button>
          </form>
          <br />



          <p className="text-center text-sm text-slate-500 dark:text-slate-400">
            ¿Necesitas acceso para tu empresa?{' '}
            <button
              type="button"
              onClick={() => navigate('/register')}
              className="font-semibold text-brand-600 transition-colors hover:text-brand-700 dark:text-brand-400 dark:hover:text-brand-300"
            >
              Solicitar acceso
            </button>
          </p>

          <div className="mt-10 flex items-center justify-center gap-2 rounded-xl bg-slate-100/80 px-4 py-2.5 text-xs text-slate-500 dark:bg-slate-900/60 dark:text-slate-400">
            <ShieldCheck className="h-3.5 w-3.5 text-emerald-500" />
            Conexión segura · Datos protegidos
          </div>
        </div>
      </div>

      {/* ==================== MODAL RECUPERAR CONTRASEÑA ==================== */}
      {showForgotModal && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="forgot-password-title"
        >
          <div
            className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm"
            onClick={() => setShowForgotModal(false)}
          />

          <div className="relative w-full max-w-md overflow-hidden rounded-2xl bg-white shadow-2xl dark:bg-slate-900">
            {/* Header */}
            <div className="flex items-start justify-between border-b border-slate-100 px-6 py-5 dark:border-slate-800">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-50 dark:bg-brand-950/50">
                  <UserCog className="h-5 w-5 text-brand-600 dark:text-brand-400" />
                </div>

                <div>
                  <h3
                    id="forgot-password-title"
                    className="text-lg font-semibold text-slate-900 dark:text-white"
                  >
                    Recuperar contraseña
                  </h3>

                  <p className="text-sm text-slate-500 dark:text-slate-400">
                    Equipo de soporte
                  </p>
                </div>
              </div>

              <button
                type="button"
                onClick={() => setShowForgotModal(false)}
                className="rounded-lg p-1.5 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-800 dark:hover:text-slate-300"
                aria-label="Cerrar"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {/* Body */}
            <div className="px-6 py-6">
              <p className="text-[15px] leading-relaxed text-slate-600 dark:text-slate-300">
                Si no recuerdas tu contraseña o tienes problemas para acceder,
                nuestro equipo puede ayudarte a verificar tu identidad y
                restablecer el acceso de forma segura.
              </p>

              <div className="mt-6 space-y-3">
                <a
                  href="tel:+51999999999"
                  className="flex items-center gap-4 rounded-xl border border-slate-200 bg-slate-50/50 px-4 py-3.5 transition-all hover:border-brand-200 hover:bg-brand-50/50 dark:border-slate-700 dark:bg-slate-800/50 dark:hover:border-brand-800 dark:hover:bg-brand-950/30"
                >
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-white shadow-sm dark:bg-slate-700">
                    <Phone className="h-4.5 w-4.5 text-brand-600 dark:text-brand-400" />
                  </div>

                  <div>
                    <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                      Soporte telefónico
                    </p>

                    <p className="text-[15px] font-semibold text-slate-900 dark:text-white">
                      +51 999 999 999
                    </p>
                  </div>
                </a>

                <a
                  href="mailto:soporte@tuempresa.com"
                  className="flex items-center gap-4 rounded-xl border border-slate-200 bg-slate-50/50 px-4 py-3.5 transition-all hover:border-brand-200 hover:bg-brand-50/50 dark:border-slate-700 dark:bg-slate-800/50 dark:hover:border-brand-800 dark:hover:bg-brand-950/30"
                >
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-white shadow-sm dark:bg-slate-700">
                    <MailIcon className="h-4.5 w-4.5 text-brand-600 dark:text-brand-400" />
                  </div>

                  <div className="min-w-0">
                    <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                      Correo de soporte
                    </p>

                    <p className="truncate text-[15px] font-semibold text-slate-900 dark:text-white">
                      soporte@tuempresa.com
                    </p>
                  </div>
                </a>
              </div>

              <p className="mt-5 text-center text-xs text-slate-400 dark:text-slate-500">
                Tu solicitud será atendida siguiendo nuestros protocolos de seguridad.
              </p>
            </div>

            {/* Footer */}
            <div className="border-t border-slate-100 px-6 py-4 dark:border-slate-800">
              <Button
                type="button"
                variant="secondary"
                className="w-full"
                onClick={() => setShowForgotModal(false)}
              >
                Entendido
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
