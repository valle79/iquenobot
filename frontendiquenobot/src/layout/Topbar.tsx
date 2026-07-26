import { Bell, Moon, Sun, LogOut, Settings, User, Menu } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { useTheme } from '@/providers'
import { useAuthStore } from '@/core/auth/auth.store'
import { Avatar } from '@/shared/atoms/Avatar/Avatar'
import { Dropdown } from '@/shared/atoms/Dropdown/Dropdown'
import { useState } from 'react'

interface TopbarProps {
  onMenuClick?: () => void
}

export function Topbar({ onMenuClick }: TopbarProps) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { theme, toggleTheme } = useTheme()
  const { user, logout } = useAuthStore()
  const [userMenuOpen, setUserMenuOpen] = useState(false)

  return (
    <header className="flex h-16 items-center justify-between border-b border-gray-200 bg-white px-4 sm:px-6 dark:border-gray-800 dark:bg-gray-950">
      <div className="flex items-center gap-3">
        <button
          onClick={onMenuClick}
          className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 lg:hidden dark:hover:bg-gray-800"
        >
          <Menu size={20} />
        </button>
        <h1 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
          {t('navigation.dashboard')}
        </h1>
      </div>

      <div className="flex items-center gap-3">
        <button
          onClick={toggleTheme}
          className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-800"
          title={theme === 'light' ? t('common.darkMode') : t('common.lightMode')}
        >
          {theme === 'light' ? <Moon size={18} /> : <Sun size={18} />}
        </button>

        <button
          onClick={() => navigate('/notifications')}
          className="relative rounded-lg p-2 text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-800"
        >
          <Bell size={18} />
          <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-red-500" />
        </button>

        <Dropdown
          open={userMenuOpen}
          onOpenChange={setUserMenuOpen}
          trigger={
            <button className="flex items-center gap-2 rounded-lg p-1 hover:bg-gray-100 dark:hover:bg-gray-800">
              <Avatar name={user?.fullName ?? ''} src={user?.avatarUrl} size="sm" />
            </button>
          }
          items={[
            {
              label: t('common.profile'),
              icon: User,
              onClick: () => navigate('/profile'),
            },
            {
              label: t('navigation.settings'),
              icon: Settings,
              onClick: () => navigate('/settings'),
            },
            { type: 'separator' },
            {
              label: t('auth.logout'),
              icon: LogOut,
              onClick: () => void logout(),
              danger: true,
            },
          ]}
        />
      </div>
    </header>
  )
}
