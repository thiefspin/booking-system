CREATE TABLE booking.appointments
(
    id                    BIGSERIAL PRIMARY KEY,
    booking_reference     VARCHAR(20)  NOT NULL UNIQUE,
    branch_id            BIGINT       NOT NULL REFERENCES booking.branches(id),
    customer_first_name  VARCHAR(100) NOT NULL,
    customer_last_name   VARCHAR(100) NOT NULL,
    customer_email       VARCHAR(255) NOT NULL,
    customer_phone       VARCHAR(20)  NOT NULL,
    appointment_date_time TIMESTAMP   NOT NULL,
    duration_minutes     INT          NOT NULL CHECK (duration_minutes >= 15),
    purpose              VARCHAR(500),
    notes                TEXT,
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at         TIMESTAMP,
    cancellation_reason  TEXT,

    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),
    CONSTRAINT chk_email_format CHECK (customer_email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_phone_format CHECK (customer_phone ~ '^[\+]?[0-9\-\s\(\)]+$'),
    CONSTRAINT chk_future_appointment CHECK (appointment_date_time > created_at)
);

CREATE INDEX idx_appointments_booking_reference ON booking.appointments(booking_reference);
CREATE INDEX idx_appointments_branch_id ON booking.appointments(branch_id);
CREATE INDEX idx_appointments_customer_email ON booking.appointments(customer_email);
CREATE INDEX idx_appointments_appointment_date_time ON booking.appointments(appointment_date_time);
CREATE INDEX idx_appointments_status ON booking.appointments(status);
CREATE INDEX idx_appointments_created_at ON booking.appointments(created_at);

CREATE INDEX idx_appointments_branch_datetime_status ON booking.appointments(branch_id, appointment_date_time, status);

CREATE TRIGGER update_appointments_updated_at
    BEFORE UPDATE ON booking.appointments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

