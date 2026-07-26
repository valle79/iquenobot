import { BaseService } from './base.service'
import type { ContactDto, CreateContactRequest } from '@/types/contact'
import type { PagedResponse } from '@/types/api'

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
}

export const contactService = new ContactService()
