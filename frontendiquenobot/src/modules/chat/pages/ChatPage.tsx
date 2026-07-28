import { useEffect, useState } from 'react'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import { ConversationList } from '@/modules/chat/components/conversation-list/ConversationList'
import { ChatWindow } from '@/modules/chat/components/chat-window/ChatWindow'
import { MessageInput } from '@/modules/chat/components/message-input/MessageInput'
import { ChatToolbar } from '@/modules/chat/components/chat-toolbar/ChatToolbar'
import { ContactInfo } from '@/modules/chat/components/contact-info/ContactInfo'
import { useChatSocket } from '@/modules/chat/hooks/useChatSocket'
import { useCreateConversation } from '@/modules/chat/hooks/useConversations'
import { useBreakpoint } from '@/hooks/useMediaQuery'
import { ArrowLeft, Loader2 } from 'lucide-react'

export default function ChatPage() {
  const { id: conversationId } = useParams<{ id: string }>()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [showInfo, setShowInfo] = useState(false)
  const { isLg } = useBreakpoint()
  const createConversation = useCreateConversation()

  useChatSocket()

  useEffect(() => {
    if (conversationId === 'new') {
      const contactId = searchParams.get('contactId')
      if (!contactId) {
        navigate('/conversations', { replace: true })
        return
      }
      createConversation.mutate(
        { contactId, channel: 'WHATSAPP' },
        {
          onSuccess: (conversation) => {
            navigate(`/conversations/${conversation.id}`, { replace: true })
          },
        },
      )
    }
  }, [])

  if (conversationId === 'new') {
    return (
      <div className="flex h-[calc(100vh-8rem)] -mx-4 sm:-mx-6">
        {isLg && (
          <div className="flex w-80 shrink-0 flex-col border-r border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-950">
            <ConversationList />
          </div>
        )}
        <div className="flex flex-1 items-center justify-center text-gray-500 dark:text-gray-400">
          <Loader2 className="h-8 w-8 animate-spin" />
          <span className="ml-2">Creando conversación...</span>
        </div>
      </div>
    )
  }

  const showContactInfo = showInfo && !!conversationId && isLg
  const showList = !conversationId || isLg
  const showChat = !!conversationId

  return (
    <div className="flex h-[calc(100vh-8rem)] -mx-4 sm:-mx-6">
      {showList && (
        <div className={`flex shrink-0 flex-col border-r border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-950 ${isLg ? 'w-80' : 'w-full'}`}>
          <ConversationList />
        </div>
      )}

      {showChat && (
        <div className={`flex flex-1 flex-col min-h-0 bg-white dark:bg-gray-950 ${!isLg ? 'fixed inset-0 z-30' : ''}`}>
          {!isLg && (
            <div className="flex items-center gap-2 border-b border-gray-200 px-4 py-3 dark:border-gray-700">
              <button
                onClick={() => navigate('/conversations')}
                className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-800"
              >
                <ArrowLeft size={20} />
              </button>
              <span className="font-medium text-gray-900 dark:text-gray-100">Conversación</span>
            </div>
          )}
          <ChatToolbar
            onToggleInfo={() => setShowInfo(!showInfo)}
            showInfo={showInfo}
          />
          <ChatWindow />
          <MessageInput />
        </div>
      )}

      {showContactInfo && conversationId && (
        <ContactInfo
          conversationId={conversationId}
          open={showContactInfo}
          onClose={() => setShowInfo(false)}
        />
      )}
    </div>
  )
}
