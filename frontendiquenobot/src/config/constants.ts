export const API_BASE_URL = import.meta.env.VITE_API_URL ?? '/api/v1'

export const WS_URL = import.meta.env.VITE_WS_URL ?? ''

export const APP_NAME = import.meta.env.VITE_APP_NAME ?? 'IquenoBot CRM'

export const PAGINATION = {
  DEFAULT_PAGE: 0,
  DEFAULT_SIZE: 20,
  CHAT_SIZE: 50,
} as const

export const STALE_TIMES = {
  SHORT: 1000 * 30,
  MEDIUM: 1000 * 60 * 2,
  LONG: 1000 * 60 * 10,
  INFINITY: Infinity,
} as const

export const STORAGE_KEYS = {
  ACCESS_TOKEN: 'iq_access_token',
  REFRESH_TOKEN: 'iq_refresh_token',
  THEME: 'iq_theme',
  LANGUAGE: 'iq_language',
  USER: 'iq_user',
} as const
