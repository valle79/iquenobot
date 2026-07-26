import type { RoleType, UserStatus, TenantStatus } from './enums'

export interface LoginRequest {
  email: string
  password: string
  deviceInfo?: string
  ipAddress?: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresAt: string
  user: UserDto
  tenant: TenantDto
}

export interface UserDto {
  id: string
  email: string
  firstName: string
  lastName: string
  fullName: string
  phone: string
  avatarUrl: string
  status: UserStatus
  role: RoleType
  emailVerified: boolean
  loginAttempts: number
  lastLoginAt: string
  lockedUntil: string
  createdAt: string
  updatedAt: string
  tenantId: string
}

export interface TenantDto {
  id: string
  companyName: string
  subdomain: string
  contactEmail: string
  contactPhone: string
  websiteUrl: string
  logoUrl: string
  address: string
  city: string
  country: string
  timezone: string
  currency: string
  status: TenantStatus
  subscriptionPlan: string
  subscriptionExpiresAt: string
  maxUsers: number
  maxConversations: number
  features: string
  createdAt: string
  updatedAt: string
}

export interface CreateUserRequest {
  email: string
  password: string
  firstName: string
  lastName: string
  phone?: string
  role: RoleType
  avatarUrl?: string
}

export interface UpdateUserRequest {
  firstName?: string
  lastName?: string
  phone?: string
  avatarUrl?: string
  role?: RoleType
  status?: UserStatus
}

export interface CreateTenantRequest {
  companyName: string
  subdomain: string
  contactEmail: string
  contactPhone?: string
  websiteUrl?: string
  address?: string
  city?: string
  country?: string
  timezone?: string
  currency?: string
  subscriptionPlan?: string
  maxUsers?: number
  maxConversations?: number
  adminEmail: string
  adminPassword: string
  adminFirstName: string
  adminLastName: string
  adminPhone?: string
}
