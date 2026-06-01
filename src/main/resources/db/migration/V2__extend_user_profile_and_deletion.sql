ALTER TABLE users
    ADD COLUMN first_name VARCHAR(255),
    ADD COLUMN middle_name VARCHAR(255),
    ADD COLUMN last_name VARCHAR(255),
    ADD COLUMN additional_contact VARCHAR(255),
    ADD COLUMN deleted_at TIMESTAMPTZ;
