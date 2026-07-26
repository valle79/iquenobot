import { cn } from '@/shared/utils'

interface SkeletonProps {
  variant?: 'text' | 'circular' | 'rectangular'
  width?: string | number
  height?: string | number
  className?: string
}

export function Skeleton({ variant = 'text', width, height, className }: SkeletonProps) {
  return (
    <div
      className={cn(
        'animate-pulse bg-gray-200 dark:bg-gray-700',
        variant === 'circular' && 'rounded-full',
        variant === 'text' && 'h-4 rounded',
        variant === 'rectangular' && 'rounded-lg',
        className,
      )}
      style={{ width, height }}
    />
  )
}

export function SkeletonCard({ className }: { className?: string }) {
  return (
    <div className={cn('rounded-lg border border-gray-200 p-4 dark:border-gray-700', className)}>
      <div className="mb-4 flex items-center gap-3">
        <Skeleton variant="circular" width={40} height={40} />
        <div className="flex-1 space-y-2">
          <Skeleton width="60%" />
          <Skeleton width="40%" />
        </div>
      </div>
      <div className="space-y-2">
        <Skeleton width="100%" />
        <Skeleton width="80%" />
      </div>
    </div>
  )
}

export function SkeletonTable({ rows = 5, cols = 4 }: { rows?: number; cols?: number }) {
  return (
    <div className="rounded-lg border border-gray-200 dark:border-gray-700">
      <div className="flex gap-4 border-b border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-800/50">
        {Array.from({ length: cols }).map((_, i) => (
          <Skeleton key={i} className="flex-1" />
        ))}
      </div>
      {Array.from({ length: rows }).map((_, r) => (
        <div key={r} className="flex gap-4 border-b border-gray-100 p-4 last:border-0 dark:border-gray-800">
          {Array.from({ length: cols }).map((_, c) => (
            <Skeleton key={c} className="flex-1" width={c === 0 ? '40%' : '100%'} />
          ))}
        </div>
      ))}
    </div>
  )
}

export function SkeletonChat({ messages = 4 }: { messages?: number }) {
  return (
    <div className="flex h-full flex-col gap-4 p-4">
      {Array.from({ length: messages }).map((_, i) => {
        const isMine = i % 2 === 0
        return (
          <div key={i} className={cn('flex gap-3', isMine ? 'flex-row-reverse' : '')}>
            <Skeleton variant="circular" width={32} height={32} />
            <div className={cn('space-y-2', isMine ? 'items-end' : '')}>
              <Skeleton width={isMine ? 120 : 200} height={32} className="rounded-2xl" />
              <Skeleton width={isMine ? 80 : 160} height={20} className="rounded-2xl" />
            </div>
          </div>
        )
      })}
    </div>
  )
}

export function SkeletonStats({ cards = 4 }: { cards?: number }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {Array.from({ length: cards }).map((_, i) => (
        <div key={i} className="rounded-xl border border-gray-200 p-5 dark:border-gray-700">
          <div className="mb-3 flex items-center justify-between">
            <Skeleton width="50%" />
            <Skeleton variant="circular" width={32} height={32} />
          </div>
          <Skeleton width="70%" height={28} className="mb-2" />
          <Skeleton width="40%" />
        </div>
      ))}
    </div>
  )
}
