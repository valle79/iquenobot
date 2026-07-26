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
import { LogIn, Mail, Lock, Eye, EyeOff, ShieldCheck } from 'lucide-react'

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
        'Credenciales incorrectas. Verifica tu correo y contraseña.'

      toast.error('No se pudo iniciar sesión', {
        description: message,
      })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen w-full bg-white dark:bg-gray-950">
      {/* ========== LADO IZQUIERDO ========== */}
      <div className="relative hidden w-1/2 overflow-hidden lg:block">
        <img
          src="https://images.unsplash.com/photo-1551434678-e076c223a692?q=80&w=2070&auto=format&fit=crop"
          alt="Plataforma empresarial"
          className="absolute inset-0 h-full w-full object-cover"
        />

        {/* Overlay profesional */}
        <div className="absolute inset-0 bg-gradient-to-br from-slate-950/85 via-slate-900/75 to-slate-950/90" />

        {/* Contenido */}
        <div className="relative z-10 flex h-full flex-col justify-between p-12 text-white">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 backdrop-blur-sm ring-1 ring-white/20">
            <ShieldCheck className="h-5 w-5 text-white" />
          </div>

          <div className="max-w-lg">
            <h2 className="text-4xl font-bold leading-tight tracking-tight">
              Gestiona tu empresa<br />
              con inteligencia
            </h2>
            <p className="mt-5 text-lg leading-relaxed text-slate-300">
              Plataforma multi-empresa diseñada para escalar. Control total,
              seguridad avanzada y herramientas pensadas para equipos modernos.
            </p>
          </div>

          <p className="text-sm text-slate-400">
            © {new Date().getFullYear()} · Todos los derechos reservados
          </p>
        </div>
      </div>

      {/* ========== LADO DERECHO (LOGIN) ========== */}
      <div className="flex w-full flex-col justify-center px-6 py-12 sm:px-12 lg:w-1/2 lg:px-16 xl:px-24">
        <div className="mx-auto w-full max-w-md">
          {/* Header */}
          <div className="mb-10">
            <h1 className="text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-50">
              {t('auth.login') || 'Iniciar sesión'}
            </h1>
            <p className="mt-2.5 text-[15px] text-slate-500 dark:text-slate-400">
              Ingresa tus credenciales para acceder a la plataforma
            </p>
          </div>

          {/* Formulario */}
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
              >
                {showPassword ? (
                  <EyeOff className="h-4 w-4" />
                ) : (
                  <Eye className="h-4 w-4" />
                )}
              </button>
            </div>

            <Button
              type="submit"
              loading={loading}
              disabled={loading || !isValid}
              className="mt-3 w-full"
              size="lg"
            >
              <LogIn className="h-4 w-4" />
              {loading ? 'Verificando...' : 'Iniciar sesión'}
            </Button>
          </form>

          {/* Footer */}
          <p className="mt-10 text-center text-sm text-slate-500 dark:text-slate-400">
            ¿No tienes una empresa registrada?{' '}
            <button
              type="button"
              onClick={() => navigate('/register')}
              className="font-semibold text-brand-600 transition hover:text-brand-700 dark:text-brand-400 dark:hover:text-brand-300"
            >
              Crear cuenta
            </button>
          </p>

          <p className="mt-8 flex items-center justify-center gap-1.5 text-xs text-slate-400 dark:text-slate-500">
            <ShieldCheck className="h-3.5 w-3.5" />
            Conexión segura · Datos encriptados
          </p>
        </div>
      </div>
    </div>
  )
}