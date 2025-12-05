CREATE SCHEMA IF NOT EXISTS booking;

CREATE TABLE booking.branches
(
    id                                   BIGSERIAL PRIMARY KEY,
    code                                 VARCHAR(10)  NOT NULL UNIQUE,
    name                                 VARCHAR(150) NOT NULL,
    address                              TEXT         NOT NULL,
    phone_number                         VARCHAR(20)  NOT NULL,
    email                                VARCHAR(255),
    opening_time                         TIME         NOT NULL,
    closing_time                         TIME         NOT NULL,
    max_concurrent_appointments_per_slot INT          NOT NULL CHECK (max_concurrent_appointments_per_slot > 0),
    is_active                            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_opening_before_closing CHECK (opening_time < closing_time),
    CONSTRAINT chk_phone_format CHECK (phone_number ~ '^[\+]?[0-9\-\s\(\)]+$')
);

CREATE INDEX idx_branches_code ON booking.branches (code);
CREATE INDEX idx_branches_active ON booking.branches (is_active);
CREATE INDEX idx_branches_name ON booking.branches (name);

CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_branches_updated_at
    BEFORE UPDATE
    ON booking.branches
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();


INSERT INTO booking.branches (code, name, address, phone_number, email, opening_time, closing_time,
                              max_concurrent_appointments_per_slot, is_active)
VALUES ('CPT001', 'Claremont Branch', '33 Main Road, Claremont, Cape Town, 7708',
        '+27-21-555-0101', 'claremont@company.com', '08:00:00', '17:00:00', 5, true),
       ('CPT002', 'V&A Waterfront Branch', 'Shop 209, V&A Waterfront, Cape Town, 8001',
        '+27-21-555-0102', 'waterfront@company.com', '09:00:00', '18:00:00', 4, true),
       ('JHB001', 'Sandton City Branch',
        'Rivonia Road, Sandton City Mall, Sandton, Johannesburg, 2196',
        '+27-11-555-0201', 'sandton@company.com', '08:30:00', '17:30:00', 6, true),
       ('JHB002', 'Rosebank Mall Branch', '50 Bath Avenue, Rosebank Mall, Johannesburg, 2196',
        '+27-11-555-0202', 'rosebank@company.com', '09:00:00', '17:00:00', 4, true),
       ('PTA001', 'Menlyn Park Branch', 'Lois Avenue, Menlyn Park, Pretoria, 0181',
        '+27-12-555-0301', 'menlyn@company.com', '08:00:00', '17:00:00', 5, true),
       ('DBN001', 'Gateway Theatre Branch', '1 Palm Blvd, Gateway Mall, Umhlanga, Durban, 4319',
        '+27-31-555-0401', 'gateway@company.com', '08:00:00', '17:30:00', 4, true),
       ('DBN002', 'Florida Road Branch', '128 Florida Rd, Morningside, Durban, 4001',
        '+27-31-555-0402', 'florida@company.com', '09:00:00', '18:00:00', 3, true),
       ('PE001', 'Walmer Park Branch', '14th Avenue, Walmer Park Centre, Gqeberha, 6070',
        '+27-41-555-0501', 'walmer@company.com', '08:30:00', '17:30:00', 3, true),
       ('BLO001', 'Loch Logan Branch',
        '105 Henry Street, Loch Logan Waterfront, Bloemfontein, 9301',
        '+27-51-555-0601', 'lochlogan@company.com', '08:00:00', '17:00:00', 4, true),
       ('PLK001', 'Savannah Mall Branch', 'Thabo Mbeki Street, Savannah Mall, Polokwane, 0699',
        '+27-15-555-0701', 'polokwane@company.com', '08:00:00', '16:30:00', 3, true),
       ('KIM001', 'Northern Cape Mall Branch', 'Oliver Rd, Kimberley, 8301',
        '+27-53-555-0801', 'kimberley@company.com', '08:00:00', '17:00:00', 3, true),
       ('MTH001', 'Vincent Park Branch', 'Devereux Ave, Vincent Park Centre, East London, 5247',
        '+27-43-555-0901', 'vincent@company.com', '09:00:00', '17:00:00', 3, true),
       ('NEL001', 'Ilanga Mall Branch', 'Flammingo St, Ilanga Mall, Mbombela (Nelspruit), 1201',
        '+27-13-555-1001', 'ilanga@company.com', '08:30:00', '17:30:00', 3, true),
       ('FSH001', 'Foschini Village Branch', 'Church Street, Somerset West, Cape Town, 7130',
        '+27-21-555-1101', 'somerset@company.com', '09:00:00', '18:00:00', 2, true),
       ('CPT003', 'Canal Walk Branch', 'Century Blvd, Canal Walk Shopping Centre, Cape Town, 7441',
        '+27-21-555-1201', 'canalwalk@company.com', '08:00:00', '20:00:00', 6, true),
       ('CPT099', 'Closed Long Street Branch', '123 Long Street, Cape Town, 8001',
        '+27-21-555-0001', 'closed@company.com', '09:00:00', '17:00:00', 2, false),
       ('JHB099', 'Renovation Randburg Branch', '1 Jan Smuts Ave, Randburg, Johannesburg, 2194',
        '+27-11-555-0002', 'renovating@company.com', '08:00:00', '16:00:00', 1, false);

