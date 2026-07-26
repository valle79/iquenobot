package com.iquenobot.conversation.application;

import com.iquenobot.auth.domain.entity.User;
import com.iquenobot.auth.domain.repository.UserRepository;
import com.iquenobot.contact.application.ContactService;
import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.contact.domain.repository.ContactRepository;
import com.iquenobot.conversation.domain.dto.ConversationDto;
import com.iquenobot.conversation.domain.dto.ConversationMessageDto;
import com.iquenobot.conversation.domain.dto.CreateConversationRequestDto;
import com.iquenobot.conversation.domain.dto.SendMessageRequestDto;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.entity.ConversationMessage;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.conversation.interfaces.mapper.ConversationMapper;
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.ConversationPriority;
import com.iquenobot.shared.enums.ConversationStatus;
import com.iquenobot.shared.enums.MessageDirection;
import com.iquenobot.shared.enums.MessageStatus;
import com.iquenobot.shared.enums.MessageType;
import com.iquenobot.shared.exception.BusinessException;
import com.iquenobot.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final ConversationMapper conversationMapper;
    private final ContactService contactService;

    @Transactional(readOnly = true)
    public ConversationDto getById(UUID id) {
        UUID tenantId = getTenantId();
        Conversation conversation = conversationRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversación no encontrada"));
        
        ConversationDto dto = conversationMapper.toDto(conversation);
        
        // Get last message
        List<ConversationMessage> lastMessages = messageRepository.findByConversationIdOrderBySentAtAsc(id);
        if (!lastMessages.isEmpty()) {
            dto.setLastMessage(conversationMapper.toMessageDto(lastMessages.get(lastMessages.size() - 1)));
        }
        
        return dto;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ConversationDto> getAll(Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Conversation> page = conversationRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ConversationDto> getByStatus(ConversationStatus status, Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Conversation> page = conversationRepository.findByTenantIdAndStatusAndDeletedFalse(
                tenantId, status, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ConversationDto> getActiveConversations(Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Conversation> page = conversationRepository.findActiveConversations(tenantId, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ConversationDto> getUnassignedConversations(Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Conversation> page = conversationRepository.findUnassignedConversations(tenantId, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ConversationDto> getMyConversations(Pageable pageable) {
        UUID tenantId = getTenantId();
        UUID userId = getCurrentUserId();
        Page<Conversation> page = conversationRepository.findByTenantIdAndAssignedUserIdAndDeletedFalse(
                tenantId, userId, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ConversationMessageDto> getMessages(UUID conversationId, Pageable pageable) {
        UUID tenantId = getTenantId();
        
        // Verify conversation exists and belongs to tenant
        conversationRepository.findByIdAndTenantIdAndDeletedFalse(conversationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversación no encontrada"));
        
        Page<ConversationMessage> page = messageRepository.findByConversationIdOrderBySentAtDesc(
                conversationId, pageable);
        
        return PagedResponse.<ConversationMessageDto>builder()
                .content(page.getContent().stream().map(conversationMapper::toMessageDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public ConversationDto create(CreateConversationRequestDto request) {
        UUID tenantId = getTenantId();

        // Validate contact exists
        Contact contact = contactRepository.findByIdAndTenantIdAndDeletedFalse(request.getContactId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contacto no encontrado"));

        if (!contact.canReceiveMessages()) {
            throw new BusinessException("El contacto no puede recibir mensajes");
        }

        // Check if there's an existing active conversation
        Page<Conversation> existingConversations = conversationRepository
                .findByTenantIdAndContactIdAndDeletedFalse(tenantId, request.getContactId(), Pageable.unpaged());
        
        boolean hasActiveConversation = existingConversations.getContent().stream()
                .anyMatch(Conversation::isActive);
        
        if (hasActiveConversation) {
            throw new BusinessException("Ya existe una conversación activa con este contacto");
        }

        // Build conversation
        Conversation conversation = Conversation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .contact(contact)
                .channel(request.getChannel())
                .status(ConversationStatus.OPEN)
                .priority(request.getPriority() != null ? request.getPriority() : ConversationPriority.MEDIUM)
                .subject(request.getSubject())
                .channelConversationId(request.getChannelConversationId())
                .lastMessageAt(LocalDateTime.now())
                .messageCount(0)
                .unreadCount(0)
                .tags(request.getTags())
                .botConversation(false)
                .build();

        // Assign user if provided
        if (request.getAssignedUserId() != null) {
            User assignedUser = userRepository.findById(request.getAssignedUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
            conversation.assignTo(assignedUser);
        }

        conversation = conversationRepository.save(conversation);
        
        // Update contact stats
        contact.incrementConversationCount();
        contactRepository.save(contact);

        // Create initial message if provided
        if (request.getInitialMessage() != null && !request.getInitialMessage().isBlank()) {
            createMessage(conversation, request.getInitialMessage(), MessageDirection.INBOUND);
        }

        log.info("Conversation created: {} for contact: {} in tenant: {}", 
                 conversation.getId(), contact.getId(), tenantId);
        
        return conversationMapper.toDto(conversation);
    }

    @Transactional
    public ConversationMessageDto sendMessage(SendMessageRequestDto request) {
        UUID tenantId = getTenantId();
        UUID userId = getCurrentUserId();

        // Get conversation
        Conversation conversation = conversationRepository
                .findByIdAndTenantIdAndDeletedFalse(request.getConversationId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversación no encontrada"));

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Create message
        ConversationMessage message = ConversationMessage.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .conversation(conversation)
                .user(user)
                .direction(MessageDirection.OUTBOUND)
                .type(request.getType())
                .status(MessageStatus.SENT)
                .content(request.getContent())
                .replyToMessageId(request.getReplyToMessageId())
                .senderName(user.getFullName())
                .senderEmail(user.getEmail())
                .fromBot(false)
                .sentAt(LocalDateTime.now())
                .build();

        message = messageRepository.save(message);

        // Update conversation
        conversation.incrementMessageCount();
        conversation.recordFirstResponse();
        conversationRepository.save(conversation);

        // Update contact
        Contact contact = conversation.getContact();
        contact.incrementMessageCount();
        contactRepository.save(contact);

        log.info("Message sent: {} in conversation: {} by user: {}", 
                 message.getId(), conversation.getId(), userId);

        return conversationMapper.toMessageDto(message);
    }

    @Transactional
    public void assignConversation(UUID conversationId, UUID userId) {
        UUID tenantId = getTenantId();

        Conversation conversation = conversationRepository
                .findByIdAndTenantIdAndDeletedFalse(conversationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversación no encontrada"));

        if (!conversation.canBeAssigned()) {
            throw new BusinessException("La conversación no puede ser asignada en su estado actual");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        conversation.assignTo(user);
        conversationRepository.save(conversation);

        log.info("Conversation {} assigned to user {} in tenant {}", conversationId, userId, tenantId);
    }

    @Transactional
    public void unassignConversation(UUID conversationId) {
        UUID tenantId = getTenantId();

        Conversation conversation = conversationRepository
                .findByIdAndTenantIdAndDeletedFalse(conversationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversación no encontrada"));

        conversation.unassign();
        conversationRepository.save(conversation);

        log.info("Conversation {} unassigned in tenant {}", conversationId, tenantId);
    }

    @Transactional
    public void resolveConversation(UUID conversationId) {
        UUID tenantId = getTenantId();

        Conversation conversation = conversationRepository
                .findByIdAndTenantIdAndDeletedFalse(conversationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversación no encontrada"));

        conversation.markResolved();
        conversationRepository.save(conversation);

        log.info("Conversation {} resolved in tenant {}", conversationId, tenantId);
    }

    @Transactional
    public void closeConversation(UUID conversationId) {
        UUID tenantId = getTenantId();

        Conversation conversation = conversationRepository
                .findByIdAndTenantIdAndDeletedFalse(conversationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversación no encontrada"));

        conversation.markClosed();
        conversationRepository.save(conversation);

        log.info("Conversation {} closed in tenant {}", conversationId, tenantId);
    }

    @Transactional
    public void reopenConversation(UUID conversationId) {
        UUID tenantId = getTenantId();

        Conversation conversation = conversationRepository
                .findByIdAndTenantIdAndDeletedFalse(conversationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversación no encontrada"));

        conversation.reopen();
        conversationRepository.save(conversation);

        log.info("Conversation {} reopened in tenant {}", conversationId, tenantId);
    }

    @Transactional
    public void markAsRead(UUID conversationId) {
        UUID tenantId = getTenantId();

        Conversation conversation = conversationRepository
                .findByIdAndTenantIdAndDeletedFalse(conversationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversación no encontrada"));

        int markedCount = messageRepository.markConversationMessagesAsRead(
                conversationId, LocalDateTime.now());
        
        conversation.resetUnreadCount();
        conversationRepository.save(conversation);

        log.info("Marked {} messages as read in conversation {} for tenant {}", 
                 markedCount, conversationId, tenantId);
    }

    public Conversation findOrCreateByChannelId(String channelConversationId, UUID contactId, ChannelType channel) {
        UUID tenantId = getTenantId();
        
        return conversationRepository.findByChannelConversationIdAndTenantIdAndDeletedFalse(
                channelConversationId, tenantId)
                .orElseGet(() -> {
                    Contact contact = contactRepository.findByIdAndTenantIdAndDeletedFalse(contactId, tenantId)
                            .orElseThrow(() -> new ResourceNotFoundException("Contacto no encontrado"));
                    
                    Conversation newConversation = Conversation.builder()
                            .id(UUID.randomUUID())
                            .tenantId(tenantId)
                            .contact(contact)
                            .channel(channel)
                            .status(ConversationStatus.OPEN)
                            .priority(ConversationPriority.MEDIUM)
                            .channelConversationId(channelConversationId)
                            .lastMessageAt(LocalDateTime.now())
                            .messageCount(0)
                            .unreadCount(0)
                            .botConversation(false)
                            .build();
                    
                    return conversationRepository.save(newConversation);
                });
    }

    private ConversationMessage createMessage(Conversation conversation, String content, MessageDirection direction) {
        ConversationMessage message = ConversationMessage.builder()
                .id(UUID.randomUUID())
                .tenantId(conversation.getTenantId())
                .conversation(conversation)
                .direction(direction)
                .type(MessageType.TEXT)
                .status(MessageStatus.SENT)
                .content(content)
                .senderName(conversation.getContact().getFullName())
                .senderPhone(conversation.getContact().getPhone())
                .fromBot(false)
                .sentAt(LocalDateTime.now())
                .build();

        message = messageRepository.save(message);
        
        conversation.incrementMessageCount();
        if (direction == MessageDirection.INBOUND) {
            conversation.incrementUnreadCount();
        }
        
        return message;
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }

    private UUID getCurrentUserId() {
        String userIdStr = TenantContext.getUserId();
        if (userIdStr == null) {
            throw new BusinessException("Usuario no autenticado");
        }
        return UUID.fromString(userIdStr);
    }

    private PagedResponse<ConversationDto> buildPagedResponse(Page<Conversation> page) {
        return PagedResponse.<ConversationDto>builder()
                .content(page.getContent().stream().map(conversationMapper::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}