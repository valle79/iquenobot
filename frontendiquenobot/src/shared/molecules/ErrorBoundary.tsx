import { Component, type ReactNode, type ErrorInfo } from 'react'
import { AlertTriangle, RefreshCw } from 'lucide-react'
import { Button } from '@/shared/atoms/Button/Button'

interface ErrorBoundaryProps {
  children: ReactNode
  fallback?: ReactNode
}

interface ErrorBoundaryState {
  hasError: boolean
  error: Error | null
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('[ErrorBoundary]', error, errorInfo)
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: null })
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback

      return (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-red-50 dark:bg-red-900/20">
            <AlertTriangle className="h-7 w-7 text-red-500" />
          </div>
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
            Algo salió mal
          </h2>
          <p className="mt-1 max-w-md text-sm text-gray-500">
            Ocurrió un error inesperado. Intenta recargar la página o vuelve a intentarlo.
          </p>
          {this.state.error && (
            <p className="mt-2 max-w-md text-xs text-gray-400">
              {this.state.error.message}
            </p>
          )}
          <Button
            variant="primary"
            size="sm"
            onClick={this.handleRetry}
            className="mt-6"
          >
            <RefreshCw className="mr-1.5 h-4 w-4" />
            Reintentar
          </Button>
        </div>
      )
    }

    return this.props.children
  }
}
