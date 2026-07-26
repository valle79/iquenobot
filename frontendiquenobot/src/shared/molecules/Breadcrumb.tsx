import { ChevronRight, type LucideIcon } from 'lucide-react'
import { Link } from 'react-router-dom'
import { cn } from '@/shared/utils'

interface BreadcrumbItem {
  label: string
  href?: string
  icon?: LucideIcon
}

interface BreadcrumbProps {
  items: BreadcrumbItem[]
  className?: string
}

export function Breadcrumb({ items, className }: BreadcrumbProps) {
  return (
    <nav className={cn('flex items-center gap-1.5 text-sm', className)}>
      {items.map((item, index) => {
        const isLast = index === items.length - 1

        return (
          <div key={index} className="flex items-center gap-1.5">
            {index > 0 && <ChevronRight size={14} className="text-gray-400" />}
            {isLast ? (
              <span className="flex items-center gap-1.5 font-medium text-gray-900 dark:text-gray-100">
                {item.icon && <item.icon size={14} />}
                {item.label}
              </span>
            ) : item.href ? (
              <Link
                to={item.href}
                className="flex items-center gap-1.5 text-gray-500 transition-colors hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
              >
                {item.icon && <item.icon size={14} />}
                {item.label}
              </Link>
            ) : (
              <span className="flex items-center gap-1.5 text-gray-500 dark:text-gray-400">
                {item.icon && <item.icon size={14} />}
                {item.label}
              </span>
            )}
          </div>
        )
      })}
    </nav>
  )
}
