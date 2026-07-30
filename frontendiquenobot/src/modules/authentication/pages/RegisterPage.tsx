import { useNavigate } from 'react-router-dom'
import { ShieldCheck, ArrowLeft } from 'lucide-react'
import { Button } from '@/shared/atoms/Button/Button'

export default function RegisterPage() {
  const navigate = useNavigate()

  return (
    <div className="flex min-h-screen w-full bg-white dark:bg-gray-950">
      {/* Lado izquierdo */}
      <div className="relative hidden w-1/2 overflow-hidden lg:block">
        <img
          src="https://images.unsplash.com/photo-1551434678-e076c223a692?q=80&w=2070&auto=format&fit=crop"
          alt="Plataforma empresarial"
          className="absolute inset-0 h-full w-full object-cover"
        />

        <div className="absolute inset-0 bg-gradient-to-br from-slate-950/85 via-slate-900/75 to-slate-950/90" />

        <div className="relative z-10 flex h-full flex-col justify-between p-12 text-white">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 backdrop-blur-sm ring-1 ring-white/20">
            <ShieldCheck className="h-5 w-5 text-white" />
          </div>

          <div className="max-w-lg">
            <h2 className="text-4xl font-bold leading-tight tracking-tight">
              Acceso gestionado
            </h2>

            <p className="mt-5 text-lg leading-relaxed text-slate-300">
              La activación de nuevas cuentas empresariales se realiza mediante
              un proceso de configuración y validación previa para garantizar
              una correcta implementación de la plataforma.
            </p>
          </div>

          <p className="text-sm text-slate-400">
            © {new Date().getFullYear()} · Todos los derechos reservados
          </p>
        </div>
      </div>

      {/* Lado derecho */}
      <div className="flex w-full flex-col justify-center px-6 py-12 sm:px-12 lg:w-1/2 lg:px-16 xl:px-24">
        <div className="mx-auto w-full max-w-md text-center">
          <div className="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-100 dark:bg-slate-800">
            <ShieldCheck className="h-8 w-8 text-slate-600 dark:text-slate-300" />
          </div>

          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-50">
            Registro no disponible
          </h1>

          <p className="mt-3 text-[15px] leading-relaxed text-slate-500 dark:text-slate-400">
            La creación de nuevas cuentas empresariales no está disponible
            desde esta página. Si deseas utilizar la plataforma en tu
            organización, ponte en contacto con nuestro equipo para solicitar
            acceso y recibir asistencia en el proceso de activación.
          </p>

          <div className="mt-8">
            <Button
              onClick={() => navigate('/login')}
              className="w-full"
              size="lg"
            >
              <ArrowLeft className="h-4 w-4" />
              Ir al inicio de sesión
            </Button>
          </div>

          <p className="mt-8 flex items-center justify-center gap-1.5 text-xs text-slate-400 dark:text-slate-500">
            <ShieldCheck className="h-3.5 w-3.5" />
            Plataforma empresarial · Acceso gestionado
          </p>
        </div>
      </div>
    </div>
  )
}