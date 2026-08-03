
import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type {
  UserDto,
  TenantDto,
  LoginRequest,
  AuthResponse
} from '@/types/auth'
import { STORAGE_KEYS } from '@/config/constants'
import { authService } from './auth.service'
import { useChatStore } from '@/modules/chat/stores/chat.store'
import { useSocketStore } from '@/core/websocket/socket.store'
import { queryClient } from '@/providers/query-client'

const AUTH_PERSIST_KEY = 'iquenobot-auth'
const LEGACY_PERSIST_KEY = STORAGE_KEYS.REFRESH_TOKEN

// Migración one-time: la sesión activa guardada bajo la clave antigua
// (iq_refresh_token) se traslada a la nueva clave (iquenobot-auth).
try {
  const legacyRaw = localStorage.getItem(LEGACY_PERSIST_KEY)
  if (legacyRaw && !localStorage.getItem(AUTH_PERSIST_KEY)) {
    localStorage.setItem(AUTH_PERSIST_KEY, legacyRaw)
  }
  localStorage.removeItem(LEGACY_PERSIST_KEY)
} catch {
  // ignore storage errors
}

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

  setTokens: (
    accessToken: string,
    refreshToken: string
  ) => void

  setUser: (user: UserDto) => void
  setLoading: (loading: boolean) => void

  clearState: () => void
}

const initialState = {
  user: null,
  tenant: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,
  isLoading: false,
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({

      ...initialState,

      login: async (credentials: LoginRequest) => {

        // LIMPIAR COMPLETAMENTE EL ESTADO ANTERIOR
        // (auth + chat + socket + cache React Query)
        get().clearState()

        set({ isLoading: true })

        try {

          const response: AuthResponse =
            await authService.login(credentials)

          set({
            user: response.user,
            tenant: response.tenant,
            accessToken: response.accessToken,
            refreshToken: response.refreshToken,
            isAuthenticated: true,
            isLoading: false,
          })

        } catch (error) {

          set({
            ...initialState,
            isLoading: false,
          })

          throw error
        }
      },

      logout: async () => {

        const token = get().refreshToken

        // LIMPIAR PRIMERO LA UI (nunca mostrar datos del tenant anterior)
        get().clearState()

        try {

          if (token) {
            await authService.logout()
          }

        } catch {
          // ignorar errores de logout
        }
      },

      refreshAuth: async () => {

        const currentToken = get().refreshToken

        if (!currentToken) {
          get().clearState()
          return
        }

        try {

          const response: AuthResponse =
            await authService.refresh(currentToken)

          set({
            user: response.user,
            tenant: response.tenant,
            accessToken: response.accessToken,
            refreshToken: response.refreshToken,
            isAuthenticated: true,
          })

        } catch {

          // Refresh fallido → sesión inválida: limpiar todo
          get().clearState()

          throw new Error('Sesión expirada')
        }
      },

      setTokens: (
        accessToken: string,
        refreshToken: string
      ) => {
        set({ accessToken, refreshToken })
      },

      setUser: (user: UserDto) => {
        set({ user })
      },

      setLoading: (isLoading: boolean) => {
        set({ isLoading })
      },

      clearState: () => {

        set(initialState)

        // limpiar el persist de auth
        useAuthStore.persist.clearStorage()
        localStorage.removeItem(LEGACY_PERSIST_KEY)

        // limpiar stores multitenant en memoria
        useChatStore.getState().clear()
        useSocketStore.getState().clear()

        // limpiar el cache de React Query (contacts, notifications,
        // dashboard, stats, etc.) que no incluye tenantId en sus keys
        queryClient.clear()
      },
    }),
    {
      name: AUTH_PERSIST_KEY,

      partialize: (state) => ({
        refreshToken: state.refreshToken,
      }),
    },
  ),
)
