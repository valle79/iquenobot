import { type ReactNode } from 'react'
import { cn } from '@/shared/utils'

interface ModalProps {
  open: boolean
  onClose: () => void
  title?: string
  description?: string
  children: ReactNode
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full'
}

const sizeClasses = {
  sm: 'max-w-sm',
  md: 'max-w-md',
  lg: 'max-w-lg',
  xl: 'max-w-xl',
  full: 'max-w-4xl',
}

export function Modal({ open, onClose, title, description, children, size = 'md' }: ModalProps) {
  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} />
      <div
        className={cn(
          'relative w-full max-h-[calc(100vh-2rem)] animate-fade-in overflow-y-auto rounded-xl bg-white p-6 shadow-modal dark:bg-gray-950',
          sizeClasses[size],
        )}
      >
        <div className="mb-6">
          {title && <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{title}</h2>}
          {description && <p className="mt-1 text-sm text-gray-500">{description}</p>}
        </div>
        {children}
      </div>
    </div>
  )
}
