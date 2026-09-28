CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE TABLE reservas (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    espacio_id UUID NOT NULL REFERENCES espacios(id),
    fecha DATE NOT NULL,
    desde TIME NOT NULL,
    hasta TIME NOT NULL,
    personas INTEGER NOT NULL CHECK (personas > 0),
    tarifa_hora NUMERIC(12,2) NOT NULL CHECK (tarifa_hora >= 0),
    relacion_aplicada VARCHAR(30) NOT NULL,
    total NUMERIC(12,2) NOT NULL CHECK (total >= 0),
    estado VARCHAR(30) NOT NULL CHECK (estado IN ('PENDIENTE_PAGO', 'CONFIRMADA', 'CANCELADA', 'VENCIDA')),
    codigo VARCHAR(50) UNIQUE,
    vence_en TIMESTAMP WITH TIME ZONE NOT NULL,
    creada_en TIMESTAMP WITH TIME ZONE NOT NULL,
    ticket_origen_id UUID REFERENCES reservas(id),
    credito_aplicado NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (credito_aplicado >= 0 AND credito_aplicado <= total),
    saldo_ticket NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (saldo_ticket >= 0),
    clave_solicitud UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (usuario_id, clave_solicitud),
    CHECK (desde < hasta),
    CONSTRAINT reservas_sin_superposicion EXCLUDE USING gist (
      espacio_id WITH =, tsrange(fecha + desde, fecha + hasta, '[)') WITH &&
    ) WHERE (estado IN ('PENDIENTE_PAGO', 'CONFIRMADA'))
);
CREATE INDEX ix_reservas_usuario_fecha ON reservas(usuario_id, fecha DESC);
ALTER TABLE pagos ADD COLUMN reserva_id UUID UNIQUE REFERENCES reservas(id);
ALTER TABLE pagos ADD COLUMN checkout_url TEXT;
ALTER TABLE pagos ADD CONSTRAINT ck_pago_reserva CHECK (
    reserva_id IS NULL OR (membresia_id IS NULL AND concepto IN ('RESERVA', 'DIFERENCIA_TICKET'))
);
