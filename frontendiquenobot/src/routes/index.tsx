import { type RouteObject } from 'react-router-dom'
import { AppLayout } from '@/layout/AppLayout'
import { AuthLayout } from '@/layout/AuthLayout'
import { ProtectedRoute } from './protected-route'
import { RouteWrapper } from './route-wrapper'
import {
  LoginPage,
  RegisterPage,
  DashboardPage,
  ChatPage,
  ContactsPage,
  ContactDetailPage,
  ProductsPage,
  ProductDetailPage,
  UsersPage,
  SettingsPage,
  AnalyticsPage,
  NotificationsPage,
  LeadsPage,
  LeadDetailPage,
  RolesPage,
  AdminTenantsPage,
  AdminDashboardPage,
  AdminPlansPage,
  AdminAnalyticsPage,
  AdminDatabasePage,
  AdminSettingsPage,
  ProfilePage,
  WhatsAppPage,
  ChannelsPage,
  CategoriesPage,
  ChatbotIntentsPage,
  ChatbotFlowsPage,
  PermissionsPage,
  TenantSettingsPage,
} from './lazy-routes'

export const routes: RouteObject[] = [
  {
    element: <AuthLayout />,
    children: [
      { path: '/login', element: <RouteWrapper><LoginPage /></RouteWrapper> },
      { path: '/register', element: <RouteWrapper><RegisterPage /></RouteWrapper> },
    ],
  },
  {
    element: (
      <ProtectedRoute>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <RouteWrapper><DashboardPage /></RouteWrapper> },
      { path: 'dashboard', element: <RouteWrapper><DashboardPage /></RouteWrapper> },
      { path: 'conversations', element: <RouteWrapper><ChatPage /></RouteWrapper> },
      { path: 'conversations/:id', element: <RouteWrapper><ChatPage /></RouteWrapper> },
      { path: 'contacts', element: <RouteWrapper><ContactsPage /></RouteWrapper> },
      { path: 'contacts/:id', element: <RouteWrapper><ContactDetailPage /></RouteWrapper> },
      { path: 'leads', element: <RouteWrapper><LeadsPage /></RouteWrapper> },
      { path: 'leads/:id', element: <RouteWrapper><LeadDetailPage /></RouteWrapper> },
      { path: 'products', element: <RouteWrapper><ProductsPage /></RouteWrapper> },
      { path: 'products/:id', element: <RouteWrapper><ProductDetailPage /></RouteWrapper> },
      {
        path: 'users',
        element: (
          <ProtectedRoute roles={['TENANT_ADMIN', 'SUPERVISOR']}>
            <RouteWrapper><UsersPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      { path: 'analytics', element: <RouteWrapper><AnalyticsPage /></RouteWrapper> },
      { path: 'roles', element: <RouteWrapper><RolesPage /></RouteWrapper> },
      { path: 'permissions', element: <RouteWrapper><PermissionsPage /></RouteWrapper> },
      { path: 'company', element: <RouteWrapper><TenantSettingsPage /></RouteWrapper> },
      { path: 'notifications', element: <RouteWrapper><NotificationsPage /></RouteWrapper> },
      { path: 'settings', element: <RouteWrapper><SettingsPage /></RouteWrapper> },
      { path: 'settings/*', element: <RouteWrapper><SettingsPage /></RouteWrapper> },
      { path: 'profile', element: <RouteWrapper><ProfilePage /></RouteWrapper> },
      { path: 'whatsapp', element: <RouteWrapper><WhatsAppPage /></RouteWrapper> },
      { path: 'channels', element: <RouteWrapper><ChannelsPage /></RouteWrapper> },
      { path: 'categories', element: <RouteWrapper><CategoriesPage /></RouteWrapper> },
      { path: 'chatbot/intents', element: <RouteWrapper><ChatbotIntentsPage /></RouteWrapper> },
      { path: 'chatbot/flows', element: <RouteWrapper><ChatbotFlowsPage /></RouteWrapper> },
      {
        path: 'admin',
        element: (
          <ProtectedRoute roles={['SUPER_ADMIN']}>
            <RouteWrapper><AdminDashboardPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/tenants',
        element: (
          <ProtectedRoute roles={['SUPER_ADMIN']}>
            <RouteWrapper><AdminTenantsPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/plans',
        element: (
          <ProtectedRoute roles={['SUPER_ADMIN']}>
            <RouteWrapper><AdminPlansPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/analytics',
        element: (
          <ProtectedRoute roles={['SUPER_ADMIN']}>
            <RouteWrapper><AdminAnalyticsPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/database',
        element: (
          <ProtectedRoute roles={['SUPER_ADMIN']}>
            <RouteWrapper><AdminDatabasePage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/settings',
        element: (
          <ProtectedRoute roles={['SUPER_ADMIN']}>
            <RouteWrapper><AdminSettingsPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
    ],
  },
]
