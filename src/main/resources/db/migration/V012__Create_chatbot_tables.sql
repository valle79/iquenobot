-- Create chatbot_flows table
CREATE TABLE chatbot_flows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    trigger_type VARCHAR(30) NOT NULL,
    trigger_keywords VARCHAR(1000),
    trigger_pattern VARCHAR(500),
    flow_config TEXT NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    use_ai BOOLEAN NOT NULL DEFAULT FALSE,
    ai_prompt TEXT,
    fallback_message TEXT,
    success_count BIGINT NOT NULL DEFAULT 0,
    failure_count BIGINT NOT NULL DEFAULT 0,
    execution_count BIGINT NOT NULL DEFAULT 0,
    
    -- Base entity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    
    -- Soft delete fields
    deleted_at TIMESTAMP,
    deleted_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- Create chatbot_intents table
CREATE TABLE chatbot_intents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    intent_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    training_phrases TEXT NOT NULL,
    responses TEXT NOT NULL,
    entities TEXT,
    context_required VARCHAR(500),
    context_output VARCHAR(500),
    actions TEXT,
    confidence_threshold DOUBLE PRECISION NOT NULL DEFAULT 0.7,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INTEGER NOT NULL DEFAULT 0,
    matched_count BIGINT NOT NULL DEFAULT 0,
    
    -- Base entity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    
    -- Soft delete fields
    deleted_at TIMESTAMP,
    deleted_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- Create indexes for chatbot_flows
CREATE INDEX idx_chatbot_flows_tenant_id ON chatbot_flows(tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_chatbot_flows_trigger_type ON chatbot_flows(trigger_type) WHERE is_deleted = FALSE;
CREATE INDEX idx_chatbot_flows_is_active ON chatbot_flows(is_active) WHERE is_deleted = FALSE;
CREATE INDEX idx_chatbot_flows_priority ON chatbot_flows(priority) WHERE is_deleted = FALSE;
CREATE INDEX idx_chatbot_flows_tenant_active ON chatbot_flows(tenant_id, is_active) WHERE is_deleted = FALSE;

-- Create indexes for chatbot_intents
CREATE INDEX idx_chatbot_intents_tenant_id ON chatbot_intents(tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_chatbot_intents_intent_name ON chatbot_intents(intent_name) WHERE is_deleted = FALSE;
CREATE INDEX idx_chatbot_intents_is_active ON chatbot_intents(is_active) WHERE is_deleted = FALSE;
CREATE INDEX idx_chatbot_intents_priority ON chatbot_intents(priority) WHERE is_deleted = FALSE;
CREATE INDEX idx_chatbot_intents_tenant_name ON chatbot_intents(tenant_id, intent_name) WHERE is_deleted = FALSE;

-- Add constraints for chatbot_flows
ALTER TABLE chatbot_flows ADD CONSTRAINT check_chatbot_flows_trigger_type 
    CHECK (trigger_type IN ('WELCOME', 'KEYWORD', 'PATTERN', 'INTENT', 'NO_AGENT_AVAILABLE', 
                            'AFTER_HOURS', 'INACTIVITY', 'MENU', 'CUSTOM'));

ALTER TABLE chatbot_flows ADD CONSTRAINT fk_chatbot_flows_tenant_id 
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

-- Add constraints for chatbot_intents
ALTER TABLE chatbot_intents ADD CONSTRAINT check_chatbot_intents_confidence 
    CHECK (confidence_threshold >= 0 AND confidence_threshold <= 1);

ALTER TABLE chatbot_intents ADD CONSTRAINT fk_chatbot_intents_tenant_id 
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

ALTER TABLE chatbot_intents ADD CONSTRAINT uq_chatbot_intents_tenant_name 
    UNIQUE (tenant_id, intent_name);

-- Create triggers for updated_at
CREATE TRIGGER update_chatbot_flows_updated_at BEFORE UPDATE ON chatbot_flows 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_chatbot_intents_updated_at BEFORE UPDATE ON chatbot_intents 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
