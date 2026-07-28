import { api } from '@/core/api/client'
import { BaseService } from './base.service'
import type { ContactDto, CreateContactRequest } from '@/types/contact'
import type { ApiResponse, PagedResponse } from '@/types/api'

export interface ImportContactsResult {
  total: number
  created: number
  skipped: number
  errors: number
  messages: string[]
}

class ContactService extends BaseService<ContactDto, CreateContactRequest> {
  constructor() {
    super('/contacts')
  }

  async search(query: string, params?: { page?: number; size?: number }): Promise<PagedResponse<ContactDto>> {
    return this.getByPath('search', { ...params, query })
  }

  async getByStatus(status: string, params?: { page?: number; size?: number }): Promise<PagedResponse<ContactDto>> {
    return this.getByPath(`status/${status}`, params)
  }

  async block(id: string, reason?: string): Promise<void> {
    await this.action(`${id}/block`, reason ? { reason } : undefined)
  }

  async unblock(id: string): Promise<void> {
    await this.action(`${id}/unblock`)
  }

  async importContacts(contacts: Record<string, string>[]): Promise<ImportContactsResult> {
    console.log('[ContactService] importContacts called with', contacts.length, 'contacts')
    console.log('[ContactService] first contact sample:', JSON.stringify(contacts[0]).slice(0, 200))
    const url = `${this.endpoint}/import`
    console.log('[ContactService] POST', url)
    try {
      const response = await api.post<ApiResponse<ImportContactsResult>>(url, { contacts }, { timeout: 180000 })
      console.log('[ContactService] response:', response.data)
      return response.data.data
    } catch (err: any) {
      console.error('[ContactService] error:', err)
      console.error('[ContactService] err.response:', err?.response?.data)
      throw err
    }
  }
}

export const contactService = new ContactService()
