-- Create categories table
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    icon_url VARCHAR(500),
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
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

-- Create products table
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    sku VARCHAR(100),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    short_description VARCHAR(500),
    price DECIMAL(10, 2) NOT NULL,
    compare_at_price DECIMAL(10, 2),
    cost_price DECIMAL(10, 2),
    category_id UUID,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    low_stock_threshold INTEGER DEFAULT 10,
    status VARCHAR(20) NOT NULL,
    image_url VARCHAR(1000),
    images TEXT,
    weight DECIMAL(10, 2),
    width DECIMAL(10, 2),
    height DECIMAL(10, 2),
    length DECIMAL(10, 2),
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    tags VARCHAR(500),
    metadata TEXT,
    
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

-- Create indexes for categories
CREATE INDEX idx_categories_tenant_id ON categories(tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_categories_name ON categories(name) WHERE is_deleted = FALSE;
CREATE INDEX idx_categories_display_order ON categories(display_order) WHERE is_deleted = FALSE;
CREATE INDEX idx_categories_is_active ON categories(is_active) WHERE is_deleted = FALSE;
CREATE INDEX idx_categories_tenant_name ON categories(tenant_id, name) WHERE is_deleted = FALSE;

-- Create indexes for products
CREATE INDEX idx_products_tenant_id ON products(tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_products_sku ON products(sku) WHERE is_deleted = FALSE AND sku IS NOT NULL;
CREATE INDEX idx_products_name ON products(name) WHERE is_deleted = FALSE;
CREATE INDEX idx_products_status ON products(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_products_category_id ON products(category_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_products_featured ON products(is_featured) WHERE is_deleted = FALSE AND is_featured = TRUE;
CREATE INDEX idx_products_stock ON products(stock_quantity) WHERE is_deleted = FALSE;
CREATE INDEX idx_products_price ON products(price) WHERE is_deleted = FALSE;
CREATE INDEX idx_products_created_at ON products(created_at);
CREATE INDEX idx_products_is_deleted ON products(is_deleted);

-- Create composite indexes for common queries
CREATE INDEX idx_products_tenant_status ON products(tenant_id, status) WHERE is_deleted = FALSE;
CREATE INDEX idx_products_tenant_sku ON products(tenant_id, sku) WHERE is_deleted = FALSE AND sku IS NOT NULL;
CREATE INDEX idx_products_tenant_category ON products(tenant_id, category_id) WHERE is_deleted = FALSE;

-- Add constraints for categories
ALTER TABLE categories ADD CONSTRAINT fk_categories_tenant_id 
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

ALTER TABLE categories ADD CONSTRAINT uq_categories_tenant_name 
    UNIQUE (tenant_id, name);

-- Add constraints for products
ALTER TABLE products ADD CONSTRAINT check_products_status 
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'OUT_OF_STOCK', 'DISCONTINUED'));

ALTER TABLE products ADD CONSTRAINT check_products_price 
    CHECK (price >= 0);

ALTER TABLE products ADD CONSTRAINT check_products_stock 
    CHECK (stock_quantity >= 0);

ALTER TABLE products ADD CONSTRAINT fk_products_tenant_id 
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

ALTER TABLE products ADD CONSTRAINT fk_products_category_id 
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;

-- Create triggers for updated_at
CREATE TRIGGER update_categories_updated_at BEFORE UPDATE ON categories 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_products_updated_at BEFORE UPDATE ON products 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
