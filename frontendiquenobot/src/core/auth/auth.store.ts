import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { UserDto, TenantDto, LoginRequest, AuthResponse } from '@/types/auth'
import { STORAGE_KEYS } from '@/config/constants'
import { authService } from './auth.service'

interface AuthState {
  user: UserDto | null
  tenant: TenantDto | null
  accessToken: string | null
  refreshToken: string | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (credentials: LoginRequest) => Promise<void>
  logout: () => Promise<void>
  refreshAuth: () => Promise<void>
  setTokens: (accessToken: string, refreshToken: string) => void
  setUser: (user: UserDto) => void
  setLoading: (loading: boolean) => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      tenant: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      isLoading: false,

      login: async (credentials: LoginRequest) => {
        set({ isLoading: true })
        try {
          const response: AuthResponse = await authService.login(credentials)
          set({
            user: response.user,
            tenant: response.tenant,
            accessToken: response.accessToken,
            refreshToken: response.refreshToken,
            isAuthenticated: true,
            isLoading: false,
          })
        } catch {
          set({ isLoading: false })
          throw new Error('Credenciales inválidas')
        }
      },

      logout: async () => {
        try {
          await authService.logout()
        } finally {
          set({
            user: null,
            tenant: null,
            accessToken: null,
            refreshToken: null,
            isAuthenticated: false,
            isLoading: false,
          })
        }
      },

      refreshAuth: async () => {
        const { refreshToken: currentToken } = get()
        if (!currentToken) throw new Error('No refresh token')

        try {
          const response: AuthResponse = await authService.refresh(currentToken)
          set({
            user: response.user,
            tenant: response.tenant,
            accessToken: response.accessToken,
            refreshToken: response.refreshToken,
            isAuthenticated: true,
          })
        } catch {
          get().logout()
          throw new Error('Sesión expirada')
        }
      },

      setTokens: (accessToken: string, refreshToken: string) => {
        set({ accessToken, refreshToken })
      },

      setUser: (user: UserDto) => {
        set({ user })
      },

      setLoading: (isLoading: boolean) => {
        set({ isLoading })
      },
    }),
    {
      name: STORAGE_KEYS.REFRESH_TOKEN,
      partialize: (state) => ({
        refreshToken: state.refreshToken,
      }),
    },
  ),
)
