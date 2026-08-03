import { api } from '@/core/api/client'
import type { ApiResponse } from '@/types/api'

export interface UploadResult {
  url: string
}

export async function uploadImage(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)

  const response = await api.post<ApiResponse<UploadResult>>(
    '/upload/image',
    formData,
    {
      headers: { 'Content-Type': null },
    },
  )

  return response.data.data.url
}

export async function uploadAttachment(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)

  const response = await api.post<ApiResponse<UploadResult>>(
    '/upload/attachment',
    formData,
    {
      headers: { 'Content-Type': null },
    },
  )

  return response.data.data.url
}
