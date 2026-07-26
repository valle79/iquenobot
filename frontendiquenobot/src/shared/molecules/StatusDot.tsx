import { cn } from '@/shared/utils'

type StatusType = 'online' | 'away' | 'busy' | 'offline' | 'active' | 'inactive' | 'pending' | 'resolved'

interface StatusDotProps {
  status: StatusType
  size?: 'sm' | 'md' | 'lg'
  label?: string
  className?: string
}

const statusColors: Record<StatusType, string> = {
  online: 'bg-green-500',
  away: 'bg-yellow-500',
  busy: 'bg-red-500',
  offline: 'bg-gray-400',
  active: 'bg-green-500',
  inactive: 'bg-gray-400',
  pending: 'bg-yellow-500',
  resolved: 'bg-blue-500',
}

const sizeClasses = {
  sm: 'h-1.5 w-1.5',
  md: 'h-2 w-2',
  lg: 'h-2.5 w-2.5',
}

export function StatusDot({ status, size = 'md', label, className }: StatusDotProps) {
  return (
    <div className={cn('inline-flex items-center gap-2', className)}>
      <span className={cn('inline-block rounded-full', statusColors[status], sizeClasses[size])} />
      {label && <span className="text-sm text-gray-600 dark:text-gray-400">{label}</span>}
    </div>
  )
}
