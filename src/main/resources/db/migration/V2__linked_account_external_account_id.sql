ALTER TABLE linked_accounts
    ADD COLUMN external_account_id VARCHAR(255),
    ADD COLUMN bank_name VARCHAR(255);

CREATE UNIQUE INDEX ux_linked_accounts_user_external_account
    ON linked_accounts (user_id, external_account_id)
    WHERE external_account_id IS NOT NULL;
