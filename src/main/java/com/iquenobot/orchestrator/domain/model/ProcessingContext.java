package com.iquenobot.orchestrator.domain.model;

import com.iquenobot.auth.domain.entity.Tenant;
import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.entity.ConversationMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProcessingContext {

    private IncomingMessage incomingMessage;
    private UUID tenantId;
    private Tenant tenant;
    private Contact contact;
    private Conversation conversation;
    private ConversationMessage persistedMessage;
    private String channelConversationId;
    private BotConfiguration botConfiguration;
    private WorkingHours workingHours;
    private ConversationContext conversationContext;
    private final List<Decision> secondaryDecisions = new ArrayList<>();

    /** true cuando el mensaje lo procesa el scheduler de consolidación
     *  (respuesta automática sobre mensajes agregados), no el webhook síncrono. */
    private boolean scheduledProcessing;

    public IncomingMessage getIncomingMessage() { return incomingMessage; }
    public void setIncomingMessage(IncomingMessage incomingMessage) { this.incomingMessage = incomingMessage; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public Contact getContact() { return contact; }
    public void setContact(Contact contact) { this.contact = contact; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    public ConversationMessage getPersistedMessage() { return persistedMessage; }
    public void setPersistedMessage(ConversationMessage persistedMessage) { this.persistedMessage = persistedMessage; }

    public String getChannelConversationId() { return channelConversationId; }
    public void setChannelConversationId(String channelConversationId) { this.channelConversationId = channelConversationId; }

    public BotConfiguration getBotConfiguration() { return botConfiguration; }
    public void setBotConfiguration(BotConfiguration botConfiguration) { this.botConfiguration = botConfiguration; }

    public WorkingHours getWorkingHours() { return workingHours; }
    public void setWorkingHours(WorkingHours workingHours) { this.workingHours = workingHours; }

    public ConversationContext getConversationContext() { return conversationContext; }
    public void setConversationContext(ConversationContext conversationContext) { this.conversationContext = conversationContext; }

    public void addSecondaryDecision(Decision decision) { this.secondaryDecisions.add(decision); }
    public List<Decision> getSecondaryDecisions() { return secondaryDecisions; }

    public boolean isScheduledProcessing() { return scheduledProcessing; }
    public void setScheduledProcessing(boolean scheduledProcessing) { this.scheduledProcessing = scheduledProcessing; }
}
