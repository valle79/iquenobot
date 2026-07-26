export interface RoleDto {
  id: string
  name: string
  displayName: string
  description: string
  permissions: string[]
  userCount: number
  system: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateRoleRequest {
  name: string
  displayName: string
  description?: string
  permissions?: string[]
}

export interface UpdateRoleRequest {
  displayName?: string
  description?: string
  permissions?: string[]
}
