import { type ReactNode, useState } from 'react'
import { cn } from '@/shared/utils'
import { motion } from 'framer-motion'

interface Tab {
  id: string
  label: string
  icon?: ReactNode
  badge?: number
}

interface TabsProps {
  tabs: Tab[]
  activeTab?: string
  onChange: (tabId: string) => void
  variant?: 'underline' | 'pills'
  className?: string
}

export function Tabs({ tabs, activeTab, onChange, variant = 'underline', className }: TabsProps) {
  const [active, setActive] = useState(activeTab ?? tabs[0]?.id ?? '')

  const selected = activeTab ?? active

  const handleChange = (tabId: string) => {
    setActive(tabId)
    onChange(tabId)
  }

  if (variant === 'pills') {
    return (
      <div className={cn('inline-flex rounded-lg bg-gray-100 p-1 dark:bg-gray-800', className)}>
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => handleChange(tab.id)}
            className={cn(
              'relative flex items-center gap-2 rounded-md px-4 py-2 text-sm font-medium transition-colors',
              selected === tab.id
                ? 'bg-white text-gray-900 shadow-sm dark:bg-gray-950 dark:text-gray-100'
                : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300',
            )}
          >
            {tab.icon}
            {tab.label}
            {tab.badge != null && (
              <span className="rounded-full bg-gray-200 px-1.5 py-0.5 text-xs dark:bg-gray-700">
                {tab.badge}
              </span>
            )}
          </button>
        ))}
      </div>
    )
  }

  return (
    <div className={cn('border-b border-gray-200 dark:border-gray-700', className)}>
      <div className="flex gap-0">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => handleChange(tab.id)}
            className={cn(
              'relative flex items-center gap-2 px-4 py-3 text-sm font-medium transition-colors',
              selected === tab.id
                ? 'text-brand-600 dark:text-brand-400'
                : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300',
            )}
          >
            {tab.label}
            {tab.badge != null && (
              <span className="rounded-full bg-brand-100 px-1.5 py-0.5 text-xs text-brand-700 dark:bg-brand-900/30 dark:text-brand-400">
                {tab.badge}
              </span>
            )}
            {selected === tab.id && (
              <motion.div
                layoutId="tab-indicator"
                className="absolute bottom-0 left-0 right-0 h-0.5 bg-brand-600 dark:bg-brand-400"
              />
            )}
          </button>
        ))}
      </div>
    </div>
  )
}
