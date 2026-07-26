import { type ReactNode } from 'react'
import { ErrorBoundary } from '@/shared/molecules/ErrorBoundary'
import { PageTransition } from '@/shared/molecules/PageTransition'

export function RouteWrapper({ children }: { children: ReactNode }) {
  return (
    <ErrorBoundary>
      <PageTransition>{children}</PageTransition>
    </ErrorBoundary>
  )
}
