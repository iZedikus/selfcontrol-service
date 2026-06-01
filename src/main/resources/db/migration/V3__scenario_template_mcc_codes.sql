ALTER TABLE scenario_templates DROP CONSTRAINT IF EXISTS scenario_templates_scenario_type_code_key;

CREATE TABLE scenario_template_mcc_codes
(
    scenario_id UUID       NOT NULL REFERENCES scenario_templates (scenario_id) ON DELETE CASCADE,
    mcc_code    VARCHAR(4) NOT NULL,
    PRIMARY KEY (scenario_id, mcc_code)
);

CREATE UNIQUE INDEX idx_scenario_template_mcc_codes_code ON scenario_template_mcc_codes (mcc_code);
