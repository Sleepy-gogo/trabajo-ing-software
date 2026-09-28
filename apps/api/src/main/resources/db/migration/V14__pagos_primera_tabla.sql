CREATE TABLE pagos (
    id UUID PRIMARY KEY,
    concepto VARCHAR(100) NOT NULL,
    usuario_id UUID NOT NULL,
    estado VARCHAR(50) NOT NULL,
    medio_pago VARCHAR(50) NOT NULL,
    monto DECIMAL(12, 2) NOT NULL,
    comprobante VARCHAR(255),
    membresia_id UUID,
    -- reserva_id UUID UNIQUE, -- Descomentar junto con la entidad Reserva
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_pago_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_pago_membresia FOREIGN KEY (membresia_id) REFERENCES membresias(id)
    -- CONSTRAINT fk_pago_reserva FOREIGN KEY (reserva_id) REFERENCES reservas(id)
);
