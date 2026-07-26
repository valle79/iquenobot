export interface SettingDto {
  id: string
  category: string
  key: string
  value: string
  type: string
  description: string
}

export interface UpdateSettingsRequest {
  category: string
  settings: {
    key: string
    value: string
    type?: string
  }[]
}
