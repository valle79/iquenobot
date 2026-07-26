import { type ReactNode, useRef, useEffect } from 'react'
import { cn } from '@/shared/utils'
import type { LucideIcon } from 'lucide-react'

interface DropdownItem {
  label?: string
  icon?: LucideIcon
  onClick?: () => void
  danger?: boolean
  type?: 'separator'
}

interface DropdownProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  trigger: ReactNode
  items: DropdownItem[]
  align?: 'start' | 'end'
}

export function Dropdown({ open, onOpenChange, trigger, items, align = 'end' }: DropdownProps) {
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return

    const handleClickOutside = (event: MouseEvent) => {
      if (ref.current && !ref.current.contains(event.target as Node)) {
        onOpenChange(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [open, onOpenChange])

  return (
    <div ref={ref} className="relative inline-block">
      <div onClick={() => onOpenChange(!open)}>{trigger}</div>
      {open && (
        <div
          className={cn(
            'absolute top-full z-50 mt-1 min-w-[180px] overflow-hidden rounded-lg border border-gray-200 bg-white py-1 shadow-elevated dark:border-gray-700 dark:bg-gray-900',
            align === 'end' ? 'right-0' : 'left-0',
          )}
        >
          {items.map((item, index) => {
            if (item.type === 'separator') {
              return <div key={index} className="my-1 border-t border-gray-200 dark:border-gray-700" />
            }
            return (
              <button
                key={index}
                onClick={() => {
                  item.onClick?.()
                  onOpenChange(false)
                }}
                className={cn(
                  'flex w-full items-center gap-2 px-3 py-2 text-sm transition-colors',
                  item.danger
                    ? 'text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-900/20'
                    : 'text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-800',
                )}
              >
                {item.icon && <item.icon size={16} />}
                {item.label}
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}
