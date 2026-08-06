import { WhatsAppConnectSection } from '@/modules/channels/components/WhatsAppConnectSection'
import { StaggerContainer, StaggerItem } from '@/shared/molecules/StaggerContainer'

export default function WhatsAppPage() {
  return (
    <StaggerContainer className="mx-auto max-w-2xl space-y-6">
      <StaggerItem>
        <WhatsAppConnectSection compact />
      </StaggerItem>
    </StaggerContainer>
  )
}
