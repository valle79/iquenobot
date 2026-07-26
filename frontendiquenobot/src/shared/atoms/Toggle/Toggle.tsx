import { type ReactNode } from 'react'
import { cn } from '@/shared/utils'
import { AnimatePresence, motion } from 'framer-motion'

interface ToggleProps {
  checked: boolean
  onChange: (checked: boolean) => void
  label?: string
  disabled?: boolean
  size?: 'sm' | 'md'
}

const sizeClasses = {
  sm: 'h-5 w-9',
  md: 'h-6 w-11',
}

const thumbSize = {
  sm: 'h-3.5 w-3.5',
  md: 'h-5 w-5',
}

export function Toggle({ checked, onChange, label, disabled, size = 'md' }: ToggleProps) {
  return (
    <label className={cn('inline-flex items-center gap-3', disabled && 'cursor-not-allowed opacity-50')}>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        disabled={disabled}
        onClick={() => onChange(!checked)}
        className={cn(
          'relative inline-flex shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2',
          checked ? 'bg-brand-600' : 'bg-gray-300 dark:bg-gray-600',
          sizeClasses[size],
        )}
      >
        <AnimatePresence>
          <motion.span
            initial={false}
            animate={{ x: checked ? (size === 'sm' ? 16 : 22) : 0 }}
            transition={{ type: 'spring', stiffness: 500, damping: 30 }}
            className={cn(
              'inline-block rounded-full bg-white shadow-sm',
              thumbSize[size],
            )}
          />
        </AnimatePresence>
      </button>
      {label && <span className="text-sm text-gray-700 dark:text-gray-300">{label}</span>}
    </label>
  )
}
