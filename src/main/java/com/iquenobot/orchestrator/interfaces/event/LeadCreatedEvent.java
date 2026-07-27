package com.iquenobot.orchestrator.interfaces.event;

import com.iquenobot.shared.enums.LeadSource;

public class LeadCreatedEvent extends OrchestratorEvent {

    private final String leadId;
    private final String contactId;
    private final String title;
    private final LeadSource source;

    public LeadCreatedEvent(String tenantId, String conversationId, String leadId,
                            String contactId, String title, LeadSource source) {
        super(tenantId, conversationId);
        this.leadId = leadId;
        this.contactId = contactId;
        this.title = title;
        this.source = source;
    }

    public String getLeadId() { return leadId; }
    public String getContactId() { return contactId; }
    public String getTitle() { return title; }
    public LeadSource getSource() { return source; }

    @Override
    public String getEventType() { return "LEAD_CREATED"; }
}
