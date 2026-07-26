import type { RoleType } from '@/types/enums'

type PermissionAction = 'view' | 'create' | 'edit' | 'delete' | 'assign' | 'resolve' | 'close'

type PermissionResource =
  | 'users'
  | 'conversations'
  | 'contacts'
  | 'leads'
  | 'products'
  | 'categories'
  | 'roles'
  | 'permissions'
  | 'notifications'
  | 'settings'
  | 'analytics'
  | 'tenant'
  | 'ai'

const roleHierarchy: Record<RoleType, number> = {
  SUPER_ADMIN: 100,
  TENANT_ADMIN: 80,
  SUPERVISOR: 60,
  AGENT: 40,
  BOT: 20,
}

export const permissionsMap: Record<PermissionResource, Record<PermissionAction, RoleType[]>> = {
  users: {
    view: ['TENANT_ADMIN', 'SUPERVISOR'],
    create: ['TENANT_ADMIN'],
    edit: ['TENANT_ADMIN', 'SUPERVISOR'],
    delete: ['TENANT_ADMIN'],
  },
  conversations: {
    view: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
    create: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
    edit: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
    delete: ['TENANT_ADMIN'],
    assign: ['TENANT_ADMIN', 'SUPERVISOR'],
    resolve: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
    close: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
  },
  contacts: {
    view: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
    create: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
    edit: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
    delete: ['TENANT_ADMIN', 'SUPERVISOR'],
  },
  leads: {
    view: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
    create: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
    edit: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
    delete: ['TENANT_ADMIN'],
    assign: ['TENANT_ADMIN', 'SUPERVISOR'],
  },
  products: {
    view: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
    create: ['TENANT_ADMIN', 'SUPERVISOR'],
    edit: ['TENANT_ADMIN', 'SUPERVISOR'],
    delete: ['TENANT_ADMIN'],
  },
  categories: {
    view: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
    create: ['TENANT_ADMIN', 'SUPERVISOR'],
    edit: ['TENANT_ADMIN', 'SUPERVISOR'],
    delete: ['TENANT_ADMIN'],
  },
  roles: {
    view: ['TENANT_ADMIN'],
    create: ['TENANT_ADMIN'],
    edit: ['TENANT_ADMIN'],
    delete: ['TENANT_ADMIN'],
  },
  permissions: {
    view: ['TENANT_ADMIN'],
    create: ['TENANT_ADMIN'],
    edit: ['TENANT_ADMIN'],
    delete: ['TENANT_ADMIN'],
  },
  notifications: {
    view: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
    create: ['TENANT_ADMIN', 'SUPERVISOR'],
    edit: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
    delete: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
  },
  settings: {
    view: ['TENANT_ADMIN'],
    create: ['TENANT_ADMIN'],
    edit: ['TENANT_ADMIN'],
    delete: ['TENANT_ADMIN'],
  },
  analytics: {
    view: ['TENANT_ADMIN', 'SUPERVISOR'],
    create: ['TENANT_ADMIN'],
    edit: ['TENANT_ADMIN'],
    delete: ['TENANT_ADMIN'],
  },
  tenant: {
    view: ['TENANT_ADMIN'],
    create: ['SUPER_ADMIN'],
    edit: ['TENANT_ADMIN'],
    delete: ['SUPER_ADMIN'],
  },
  ai: {
    view: ['TENANT_ADMIN', 'SUPERVISOR'],
    create: ['TENANT_ADMIN', 'SUPERVISOR'],
    edit: ['TENANT_ADMIN'],
    delete: ['TENANT_ADMIN'],
  },
}

export function can(userRole: RoleType, action: PermissionAction, resource: PermissionResource): boolean {
  const allowedRoles = permissionsMap[resource]?.[action]
  if (!allowedRoles) return false
  return allowedRoles.some((role) => roleHierarchy[userRole] >= roleHierarchy[role])
}

export function useAbility() {
  const { user } = require('@/core/auth/auth.store').useAuthStore()

  return {
    can: (action: PermissionAction, resource: PermissionResource): boolean => {
      if (!user) return false
      return can(user.role, action, resource)
    },
  }
}

export type { PermissionAction, PermissionResource }
