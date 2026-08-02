-- V036: Datos de facturación para cotizaciones formales en el contacto.
-- Campos opcionales (nullable) para no romper la creación de contactos existente.

ALTER TABLE contacts ADD COLUMN document_type VARCHAR(20);
ALTER TABLE contacts ADD COLUMN document_number VARCHAR(11);
ALTER TABLE contacts ADD COLUMN address VARCHAR(255);

COMMENT ON COLUMN contacts.document_type IS 'Tipo de documento del contacto: DNI o RUC';
COMMENT ON COLUMN contacts.document_number IS 'Número de documento: 8 dígitos (DNI) u 11 dígitos (RUC)';
COMMENT ON COLUMN contacts.address IS 'Dirección fiscal o de envío del contacto';
