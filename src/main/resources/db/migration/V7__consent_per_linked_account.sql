-- Один consent (acceptance) на linked_account_id по REST_КОНТРАКТ.yaml

ALTER TABLE acceptances
    ADD COLUMN linked_account_id UUID;

UPDATE acceptances a
SET linked_account_id = (
    SELECT la.linked_account_id
    FROM linked_accounts la
    WHERE la.acceptance_id = a.acceptance_id
    ORDER BY la.linked_at NULLS LAST
    LIMIT 1
)
WHERE linked_account_id IS NULL;

DELETE FROM acceptances
WHERE linked_account_id IS NULL;

ALTER TABLE acceptances
    ALTER COLUMN linked_account_id SET NOT NULL;

ALTER TABLE acceptances
    ADD CONSTRAINT fk_acceptances_linked_account
        FOREIGN KEY (linked_account_id) REFERENCES linked_accounts (linked_account_id);

ALTER TABLE acceptances
    ADD CONSTRAINT uq_acceptances_linked_account UNIQUE (linked_account_id);

ALTER TABLE linked_accounts
    DROP COLUMN IF EXISTS acceptance_id;

-- Статусы execution по контракту IS
UPDATE scenario_executions
SET status = 'Completed'
WHERE status = 'DebitCompleted';

UPDATE scenario_executions
SET status = 'Failed'
WHERE status = 'DebitFailed';

-- Статусы debit_operations — банковские enum Simulacrum
UPDATE debit_operations
SET status = 'AcceptedSettlementCompleted'
WHERE status = 'DebitCompleted';

UPDATE debit_operations
SET status = 'Rejected'
WHERE status = 'DebitFailed';

UPDATE debit_operations
SET status = 'Pending'
WHERE status = 'DebitInitiated';
