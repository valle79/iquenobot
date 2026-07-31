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
  QuotesPage,
  QuoteDetailPage,
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
      {
        index: true,
        element: (
          <ProtectedRoute roles={['TENANT_ADMIN', 'SUPERVISOR']}>
            <RouteWrapper><DashboardPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'dashboard',
        element: (
          <ProtectedRoute roles={['TENANT_ADMIN', 'SUPERVISOR']}>
            <RouteWrapper><DashboardPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      { path: 'conversations', element: <RouteWrapper><ChatPage /></RouteWrapper> },
      { path: 'conversations/:id', element: <RouteWrapper><ChatPage /></RouteWrapper> },
      { path: 'contacts', element: <RouteWrapper><ContactsPage /></RouteWrapper> },
      { path: 'contacts/:id', element: <RouteWrapper><ContactDetailPage /></RouteWrapper> },
      { path: 'leads', element: <RouteWrapper><LeadsPage /></RouteWrapper> },
      { path: 'leads/:id', element: <RouteWrapper><LeadDetailPage /></RouteWrapper> },
      {
        path: 'quotes',
        element: (
          <ProtectedRoute roles={['TENANT_ADMIN', 'SUPERVISOR']}>
            <RouteWrapper><QuotesPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'quotes/:id',
        element: (
          <ProtectedRoute roles={['TENANT_ADMIN', 'SUPERVISOR']}>
            <RouteWrapper><QuoteDetailPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
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
      {
        path: 'analytics',
        element: (
          <ProtectedRoute roles={['TENANT_ADMIN', 'SUPERVISOR']}>
            <RouteWrapper><AnalyticsPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'roles',
        element: (
          <ProtectedRoute roles={['TENANT_ADMIN']}>
            <RouteWrapper><RolesPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'permissions',
        element: (
          <ProtectedRoute roles={['TENANT_ADMIN']}>
            <RouteWrapper><PermissionsPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'company',
        element: (
          <ProtectedRoute roles={['TENANT_ADMIN']}>
            <RouteWrapper><TenantSettingsPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      { path: 'notifications', element: <RouteWrapper><NotificationsPage /></RouteWrapper> },
      {
        path: 'settings',
        element: (
          <ProtectedRoute roles={['TENANT_ADMIN', 'SUPERVISOR']}>
            <RouteWrapper><SettingsPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'settings/*',
        element: (
          <ProtectedRoute roles={['TENANT_ADMIN', 'SUPERVISOR']}>
            <RouteWrapper><SettingsPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      { path: 'profile', element: <RouteWrapper><ProfilePage /></RouteWrapper> },
      {
        path: 'whatsapp',
        element: (
          <ProtectedRoute roles={['TENANT_ADMIN', 'SUPERVISOR']}>
            <RouteWrapper><WhatsAppPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'channels',
        element: (
          <ProtectedRoute roles={['TENANT_ADMIN', 'SUPERVISOR']}>
            <RouteWrapper><ChannelsPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'categories',
        element: (
          <ProtectedRoute roles={['TENANT_ADMIN', 'SUPERVISOR']}>
            <RouteWrapper><CategoriesPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'chatbot/intents',
        element: (
          <ProtectedRoute roles={['TENANT_ADMIN', 'SUPERVISOR']}>
            <RouteWrapper><ChatbotIntentsPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'chatbot/flows',
        element: (
          <ProtectedRoute roles={['TENANT_ADMIN', 'SUPERVISOR']}>
            <RouteWrapper><ChatbotFlowsPage /></RouteWrapper>
          </ProtectedRoute>
        ),
      },
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
