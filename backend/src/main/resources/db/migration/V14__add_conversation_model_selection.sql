ALTER TABLE conversation ADD COLUMN provider_configuration_id UUID REFERENCES provider_configuration (id) ON DELETE SET NULL;
ALTER TABLE conversation ADD COLUMN selected_model VARCHAR(160);
