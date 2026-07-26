-- Add plan_id FK + new fields to tenants
ALTER TABLE tenants ADD COLUMN plan_id UUID REFERENCES plans(id);
ALTER TABLE tenants ADD COLUMN business_name VARCHAR(200);
ALTER TABLE tenants ADD COLUMN ruc VARCHAR(20);
ALTER TABLE tenants ADD COLUMN primary_color VARCHAR(7) DEFAULT '#6366f1';
ALTER TABLE tenants ADD COLUMN secondary_color VARCHAR(7) DEFAULT '#818cf8';
ALTER TABLE tenants ADD COLUMN language VARCHAR(10) DEFAULT 'es';
ALTER TABLE tenants ADD COLUMN locale VARCHAR(10) DEFAULT 'es-PE';

CREATE INDEX idx_tenants_plan_id ON tenants(plan_id) WHERE is_deleted = FALSE;

-- Seed default plan
INSERT INTO plans (id, name, code, description, monthly_price, yearly_price, max_users, max_conversations, max_contacts, features, is_active, is_public, sort_order)
VALUES
    (gen_random_uuid(), 'Gratuito', 'free', 'Plan gratuito para probar la plataforma', 0, 0, 5, 100, 500, '{"ai": false, "whatsapp": true, "reports": false, "api": false}', TRUE, TRUE, 1),
    (gen_random_uuid(), 'Básico', 'basic', 'Plan básico para pequeñas empresas', 29.99, 299.99, 15, 500, 2000, '{"ai": false, "whatsapp": true, "reports": true, "api": false}', TRUE, TRUE, 2),
    (gen_random_uuid(), 'Profesional', 'pro', 'Plan profesional para empresas en crecimiento', 79.99, 799.99, 50, 2500, 10000, '{"ai": true, "whatsapp": true, "reports": true, "api": true}', TRUE, TRUE, 3),
    (gen_random_uuid(), 'Enterprise', 'enterprise', 'Plan enterprise para grandes organizaciones', 199.99, 1999.99, NULL, NULL, NULL, '{"ai": true, "whatsapp": true, "reports": true, "api": true}', TRUE, TRUE, 4);
