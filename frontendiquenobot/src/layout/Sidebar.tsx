import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard,
  MessageCircle,
  Users,
  Target,
  Package,
  FileText,
  UserCircle,
  BarChart3,
  Bell,
  Settings,
  ChevronLeft,
  ChevronRight,
  Building2,
  Building,
  User,
  MessageSquare,
  Tags,
  Brain,
  GitBranch,
  FlaskConical,
  Shield,
  Activity,
  CreditCard,
  Database,
  type LucideIcon,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { motion, AnimatePresence } from 'framer-motion'
import { useAuthStore } from '@/core/auth/auth.store'
import type { RoleType } from '@/types/enums'

interface NavItem {
  label: string
  path: string
  icon: LucideIcon
  roles?: RoleType[]
}

const tenantNavItems: NavItem[] = [
  { label: 'navigation.dashboard', path: '/dashboard', icon: LayoutDashboard, roles: ['TENANT_ADMIN', 'SUPERVISOR'] },
  { label: 'navigation.conversations', path: '/conversations', icon: MessageCircle },
  { label: 'navigation.contacts', path: '/contacts', icon: Users },
  { label: 'navigation.leads', path: '/leads', icon: Target },
  { label: 'navigation.quotes', path: '/quotes', icon: FileText, roles: ['TENANT_ADMIN', 'SUPERVISOR'] },
  { label: 'navigation.products', path: '/products', icon: Package },
  { label: 'Categorías', path: '/categories', icon: Tags, roles: ['TENANT_ADMIN', 'SUPERVISOR'] },
  { label: 'navigation.users', path: '/users', icon: UserCircle, roles: ['TENANT_ADMIN', 'SUPERVISOR'] },
  { label: 'Intenciones', path: '/chatbot/intents', icon: Brain, roles: ['TENANT_ADMIN', 'SUPERVISOR'] },
  { label: 'Flujos', path: '/chatbot/flows', icon: GitBranch, roles: ['TENANT_ADMIN', 'SUPERVISOR'] },
  { label: 'Probar Chatbot', path: '/chatbot/test', icon: FlaskConical, roles: ['TENANT_ADMIN', 'SUPERVISOR'] },
  { label: 'navigation.analytics', path: '/analytics', icon: BarChart3, roles: ['TENANT_ADMIN', 'SUPERVISOR'] },
  { label: 'navigation.notifications', path: '/notifications', icon: Bell },
  { label: 'Mi Perfil', path: '/profile', icon: User },
  { label: 'Permisos', path: '/permissions', icon: Shield, roles: ['TENANT_ADMIN'] },
  { label: 'Mi Empresa', path: '/company', icon: Building2, roles: ['TENANT_ADMIN'] },
  { label: 'Canales', path: '/channels', icon: MessageSquare, roles: ['TENANT_ADMIN', 'SUPERVISOR'] },
  { label: 'navigation.settings', path: '/settings', icon: Settings, roles: ['TENANT_ADMIN', 'SUPERVISOR'] },
]

const adminNavItems: NavItem[] = [
  { label: 'Panel', path: '/admin', icon: LayoutDashboard },
  { label: 'Empresas', path: '/admin/tenants', icon: Building },
  { label: 'Planes', path: '/admin/plans', icon: CreditCard },
  { label: 'Analíticas', path: '/admin/analytics', icon: Activity },
  { label: 'Base de Datos', path: '/admin/database', icon: Database },
  { label: 'Mi Perfil', path: '/profile', icon: User },
  { label: 'Configuración', path: '/admin/settings', icon: Settings },
]

interface SidebarProps {
  collapsed: boolean
  onToggle: () => void
}

export function Sidebar({ collapsed, onToggle }: SidebarProps) {
  const { t } = useTranslation()
  const { user, tenant } = useAuthStore()

  const isSuperAdmin = user?.role === 'SUPER_ADMIN'
  const navItems = isSuperAdmin ? adminNavItems : tenantNavItems

  const visibleItems = navItems.filter(
    (item) => !item.roles || (user && item.roles.includes(user.role))
  )

  return (
    <aside
      className={`relative flex flex-col border-r border-gray-200 bg-white transition-all duration-300 dark:border-gray-800 dark:bg-gray-950 ${
        collapsed ? 'w-16' : 'w-64'
      }`}
    >
      <div className="flex h-16 items-center justify-between px-4">
        <AnimatePresence>
          {!collapsed ? (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="flex flex-col"
            >
              <span className="text-lg font-bold text-brand-600 dark:text-brand-400">
                LuKaBot
              </span>
              {isSuperAdmin ? (
                <span className="text-xs text-gray-500 dark:text-gray-400">Super Admin</span>
              ) : (
                <span className="flex items-center gap-1 text-xs text-gray-500 dark:text-gray-400">
                  <Building2 size={12} />
                  {tenant?.companyName ?? ''}
                </span>
              )}
            </motion.div>
          ) : (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="flex w-full justify-center"
            >
              <span className="text-lg font-bold text-brand-600 dark:text-brand-400">
                {isSuperAdmin ? 'SA' : 'IB'}
              </span>
            </motion.div>
          )}
        </AnimatePresence>
        <button
          onClick={onToggle}
          className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-800"
        >
          {collapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
        </button>
      </div>

      <nav className="flex-1 space-y-1 px-2 py-4">
        {visibleItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-brand-50 text-brand-700 dark:bg-brand-900/20 dark:text-brand-400'
                  : 'text-gray-600 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800'
              } ${collapsed ? 'justify-center' : ''}`
            }
          >
            <item.icon size={20} />
            {!collapsed && <span>{t(item.label)}</span>}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
