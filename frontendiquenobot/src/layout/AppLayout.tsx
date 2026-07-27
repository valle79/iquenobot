import { useState, useEffect } from 'react'
import { Outlet } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'
import { useBreakpoint } from '@/hooks/useMediaQuery'
import { useRealtimeEvents } from '@/modules/chat/hooks/useRealtimeEvents'
import { AnimatePresence, motion } from 'framer-motion'
import { X } from 'lucide-react'

export function AppLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const { isLg } = useBreakpoint()

  useRealtimeEvents()

  useEffect(() => {
    if (isLg) setMobileOpen(false)
  }, [isLg])

  return (
    <div className="flex h-screen overflow-hidden">
      {isLg ? (
        <Sidebar collapsed={collapsed} onToggle={() => setCollapsed(!collapsed)} />
      ) : (
        <>
          <AnimatePresence>
            {mobileOpen && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="fixed inset-0 z-40 bg-black/50 lg:hidden"
                onClick={() => setMobileOpen(false)}
              />
            )}
          </AnimatePresence>
          <AnimatePresence>
            {mobileOpen && (
              <motion.div
                initial={{ x: -280 }}
                animate={{ x: 0 }}
                exit={{ x: -280 }}
                transition={{ type: 'spring', damping: 25, stiffness: 250 }}
                className="fixed inset-y-0 left-0 z-50 lg:hidden"
              >
                <Sidebar collapsed={false} onToggle={() => setMobileOpen(false)} />
              </motion.div>
            )}
          </AnimatePresence>
          {mobileOpen && (
            <button
              onClick={() => setMobileOpen(false)}
              className="fixed left-64 top-3 z-50 rounded-lg bg-white p-1.5 text-gray-500 shadow-lg lg:hidden"
            >
              <X size={18} />
            </button>
          )}
        </>
      )}

      <div className="flex flex-1 flex-col overflow-hidden">
        <Topbar onMenuClick={() => setMobileOpen(true)} />
        <main className="flex-1 overflow-auto bg-gray-50 p-4 sm:p-6 dark:bg-gray-900">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
