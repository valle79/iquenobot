import { useState } from 'react'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { Button } from '@/shared/atoms/Button/Button'
import { Send } from 'lucide-react'

interface ResendToNumberModalProps {
  open: boolean
  quoteNumber: string
  onClose: () => void
  onConfirm: (phone: string) => void
  loading?: boolean
}

export function ResendToNumberModal({
  open,
  quoteNumber,
  onClose,
  onConfirm,
  loading,
}: ResendToNumberModalProps) {
  const [phone, setPhone] = useState('')

  const handleConfirm = () => {
    if (!phone.trim()) return
    onConfirm(phone.trim())
    setPhone('')
  }

  return (
    <Modal open={open} onClose={onClose} title="Enviar a otro número" size="md"
      description={`Reenviar la cotización ${quoteNumber} a un número de WhatsApp específico`}>
      <div className="space-y-4">
        <div>
          <label className="mb-1 block text-xs font-medium text-gray-500">
            Número de WhatsApp (con código de país, ej. 51999888777)
          </label>
          <input
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="51999888777"
            autoFocus
            className="h-10 w-full rounded-lg border border-gray-200 bg-gray-50 px-3 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100"
          />
        </div>
        <div className="flex justify-end gap-3">
          <Button variant="ghost" onClick={onClose} disabled={loading}>
            Cancelar
          </Button>
          <Button onClick={handleConfirm} disabled={!phone.trim()} loading={loading}>
            <Send size={14} />
            Enviar
          </Button>
        </div>
      </div>
    </Modal>
  )
}
