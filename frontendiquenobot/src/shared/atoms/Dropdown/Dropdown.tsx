import { type ReactNode, useRef, useEffect, useLayoutEffect, useState, useCallback } from 'react'
import { createPortal } from 'react-dom'
import { cn } from '@/shared/utils'
import type { LucideIcon } from 'lucide-react'

export interface DropdownItem {
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

interface MenuPosition {
  top: number
  left: number
  right: number
}

export function Dropdown({ open, onOpenChange, trigger, items, align = 'end' }: DropdownProps) {
  const triggerRef = useRef<HTMLDivElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)
  const [position, setPosition] = useState<MenuPosition | null>(null)

  const updatePosition = useCallback(() => {
    const el = triggerRef.current
    if (!el) return
    const rect = el.getBoundingClientRect()
    setPosition({ top: rect.bottom + 4, left: rect.left, right: rect.right })
  }, [])

  useLayoutEffect(() => {
    if (!open) {
      setPosition(null)
      return
    }
    updatePosition()
  }, [open, updatePosition])

  useEffect(() => {
    if (!open) return

    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as Node
      if (triggerRef.current?.contains(target) || menuRef.current?.contains(target)) {
        return
      }
      onOpenChange(false)
    }

    const handleReposition = () => updatePosition()

    document.addEventListener('mousedown', handleClickOutside)
    window.addEventListener('resize', handleReposition)
    window.addEventListener('scroll', handleReposition, true)
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      window.removeEventListener('resize', handleReposition)
      window.removeEventListener('scroll', handleReposition, true)
    }
  }, [open, onOpenChange, updatePosition])

  return (
    <div ref={triggerRef} className="relative inline-block">
      <div onClick={() => onOpenChange(!open)}>{trigger}</div>
      {open &&
        position &&
        createPortal(
          <div
            ref={menuRef}
            style={{
              top: position.top,
              left: align === 'start' ? position.left : undefined,
              right: align === 'end' ? window.innerWidth - position.right : undefined,
            }}
            className="fixed z-50 mt-1 min-w-[180px] overflow-hidden rounded-lg border border-gray-200 bg-white py-1 shadow-elevated dark:border-gray-700 dark:bg-gray-900"
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
          </div>,
          document.body,
        )}
    </div>
  )
}
