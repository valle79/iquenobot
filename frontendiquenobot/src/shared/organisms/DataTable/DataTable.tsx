import { useState, useMemo, type ReactNode } from 'react'
import { motion } from 'framer-motion'
import {
  flexRender,
  getCoreRowModel,
  getSortedRowModel,
  getFilteredRowModel,
  getPaginationRowModel,
  useReactTable,
  type ColumnDef,
  type SortingState,
  type ColumnFiltersState,
} from '@tanstack/react-table'
import { ChevronUp, ChevronDown, ChevronsUpDown, ChevronLeft, ChevronRight } from 'lucide-react'
import { cn } from '@/shared/utils'
import { Button } from '@/shared/atoms/Button/Button'
import { SkeletonTable } from '@/shared/atoms/Skeleton/Skeleton'
import { EmptyState } from '@/shared/molecules/EmptyState'

interface DataTableProps<T> {
  columns: ColumnDef<T>[]
  data: T[]
  loading?: boolean
  pageCount?: number
  pageIndex?: number
  onPageChange?: (page: number) => void
  pageSize?: number
  totalRecords?: number
  emptyMessage?: string
  emptyIcon?: ReactNode
}

export function DataTable<T>({
  columns,
  data,
  loading,
  pageCount,
  pageIndex = 0,
  onPageChange,
  pageSize = 20,
  totalRecords,
  emptyMessage = 'No hay datos disponibles',
  emptyIcon,
}: DataTableProps<T>) {
  const [sorting, setSorting] = useState<SortingState>([])
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([])

  const pagination = onPageChange
    ? { pageIndex, pageSize }
    : undefined

  const table = useReactTable({
    data,
    columns,
    state: { sorting, columnFilters, pagination },
    onSortingChange: setSorting,
    onColumnFiltersChange: setColumnFilters,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getPaginationRowModel: !onPageChange ? getPaginationRowModel() : undefined,
    manualPagination: !!onPageChange,
    pageCount,
  })

  return (
    <div className="space-y-4">
      <div className="overflow-hidden rounded-lg border border-gray-200 dark:border-gray-700">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 dark:bg-gray-800">
              {table.getHeaderGroups().map((headerGroup) => (
                <tr key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <th
                      key={header.id}
                      className={cn(
                        'px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400',
                        header.column.getCanSort() && 'cursor-pointer select-none',
                      )}
                      onClick={header.column.getToggleSortingHandler()}
                    >
                      <div className="flex items-center gap-1">
                        {flexRender(header.column.columnDef.header, header.getContext())}
                        {{
                          asc: <ChevronUp size={14} />,
                          desc: <ChevronDown size={14} />,
                        }[header.column.getIsSorted() as string] ?? (
                          header.column.getCanSort() && <ChevronsUpDown size={14} className="opacity-30" />
                        )}
                      </div>
                    </th>
                  ))}
                </tr>
              ))}
            </thead>
            <tbody className="divide-y divide-gray-200 bg-white dark:divide-gray-700 dark:bg-gray-950">
              {loading ? (
                <tr>
                  <td colSpan={columns.length} className="px-4 py-2">
                    <SkeletonTable rows={5} cols={columns.length} />
                  </td>
                </tr>
              ) : table.getRowModel().rows.length === 0 ? (
                <tr>
                  <td colSpan={columns.length}>
                    <EmptyState
                      icon={emptyIcon}
                      title={emptyMessage}
                      description="Comienza agregando un nuevo registro"
                      className="py-12"
                    />
                  </td>
                </tr>
              ) : (
                table.getRowModel().rows.map((row, i) => {
                  const rowTransition = { duration: 0.2, delay: i * 0.03 }
                  return (
                    <motion.tr
                      key={row.id}
                      initial={{ opacity: 0, y: 8 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={rowTransition}
                      className="transition-colors hover:bg-gray-50 dark:hover:bg-gray-800"
                    >
                      {row.getVisibleCells().map((cell) => (
                        <td key={cell.id} className="whitespace-nowrap px-4 py-3 text-sm text-gray-700 dark:text-gray-300">
                          {flexRender(cell.column.columnDef.cell, cell.getContext())}
                        </td>
                      ))}
                    </motion.tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {(onPageChange || table.getPageCount() > 1) && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-gray-500">
            {totalRecords != null && `Total: ${totalRecords} registros`}
          </p>
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onPageChange?.(pageIndex - 1)}
              disabled={pageIndex === 0}
            >
              <ChevronLeft size={16} />
            </Button>
            <span className="text-sm text-gray-600 dark:text-gray-400">
              Página {pageIndex + 1} de {pageCount ?? table.getPageCount()}
            </span>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onPageChange?.((pageIndex ?? 0) + 1)}
              disabled={pageCount != null && pageIndex >= pageCount - 1}
            >
              <ChevronRight size={16} />
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
