import { AlertTriangle, RefreshCw } from 'lucide-react'
import { Button } from '@/shared/atoms/Button/Button'

interface ErrorStateProps {
  title?: string
  message?: string
  onRetry?: () => void
}

export function ErrorState({
  title = 'Ha ocurrido un error',
  message = 'No pudimos cargar los datos. Intenta nuevamente.',
  onRetry,
}: ErrorStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-12 text-center">
      <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-red-100 dark:bg-red-900/20">
        <AlertTriangle className="text-red-600 dark:text-red-400" size={24} />
      </div>
      <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100">{title}</h3>
      <p className="mt-1 max-w-sm text-sm text-gray-500">{message}</p>
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry} className="mt-4">
          <RefreshCw size={16} />
          Reintentar
        </Button>
      )}
    </div>
  )
}
