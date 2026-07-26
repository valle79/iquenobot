import { useState, useCallback } from 'react'
import { Search, X } from 'lucide-react'
import { useDebounce } from '@/hooks/useDebounce'
import { cn } from '@/shared/utils'

interface SearchBarProps {
  placeholder?: string
  onSearch: (query: string) => void
  value?: string
  className?: string
  debounceMs?: number
}

export function SearchBar({
  placeholder = 'Buscar...',
  onSearch,
  value: externalValue,
  className,
  debounceMs = 300,
}: SearchBarProps) {
  const [internalValue, setInternalValue] = useState(externalValue ?? '')
  const value = externalValue ?? internalValue

  const debouncedValue = useDebounce(value, debounceMs)

  const handleChange = useCallback(
    (newValue: string) => {
      setInternalValue(newValue)
      if (debounceMs === 0) {
        onSearch(newValue)
      }
    },
    [debounceMs, onSearch],
  )

  return (
    <div className={cn('relative', className)}>
      <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
      <input
        type="text"
        value={value}
        onChange={(e) => handleChange(e.target.value)}
        placeholder={placeholder}
        className="h-9 w-full rounded-lg border border-gray-200 bg-gray-50 pl-9 pr-8 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100"
      />
      {value && (
        <button
          onClick={() => handleChange('')}
          className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-0.5 text-gray-400 hover:text-gray-600"
        >
          <X size={14} />
        </button>
      )}
    </div>
  )
}
