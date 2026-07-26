-- Create message_attachments table
CREATE TABLE message_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    message_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    file_name VARCHAR(255),
    file_url VARCHAR(1000) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    thumbnail_url VARCHAR(1000),
    duration_seconds INTEGER,
    width INTEGER,
    height INTEGER,
    caption VARCHAR(1000),
    channel_media_id VARCHAR(100),
    
    -- Base entity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes
CREATE INDEX idx_message_attachments_tenant_id ON message_attachments(tenant_id);
CREATE INDEX idx_message_attachments_message_id ON message_attachments(message_id);
CREATE INDEX idx_message_attachments_type ON message_attachments(type);
CREATE INDEX idx_message_attachments_created_at ON message_attachments(created_at);
CREATE INDEX idx_message_attachments_channel_media_id ON message_attachments(channel_media_id) WHERE channel_media_id IS NOT NULL;

-- Add constraints
ALTER TABLE message_attachments ADD CONSTRAINT check_message_attachments_type 
    CHECK (type IN ('IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT', 'VOICE', 'STICKER', 'LOCATION', 'CONTACT'));

-- Foreign key constraints
ALTER TABLE message_attachments ADD CONSTRAINT fk_message_attachments_tenant_id 
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

ALTER TABLE message_attachments ADD CONSTRAINT fk_message_attachments_message_id 
    FOREIGN KEY (message_id) REFERENCES conversation_messages(id) ON DELETE CASCADE;

-- Create trigger for updated_at
CREATE TRIGGER update_message_attachments_updated_at BEFORE UPDATE ON message_attachments 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();