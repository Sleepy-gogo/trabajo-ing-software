CREATE TABLE suscripciones_mercado_pago (
    id UUID PRIMARY KEY,
    membresia_id UUID NOT NULL REFERENCES membresias(id),
    pago_inicial_id UUID NOT NULL UNIQUE REFERENCES pagos(id),
    preapproval_id VARCHAR(100) UNIQUE,
    estado VARCHAR(30) NOT NULL,
    monto DECIMAL(12, 2) NOT NULL,
    checkout_url VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_suscripciones_mp_membresia ON suscripciones_mercado_pago (membresia_id);

ALTER TABLE pagos ADD COLUMN mercado_pago_payment_id VARCHAR(100) UNIQUE;
