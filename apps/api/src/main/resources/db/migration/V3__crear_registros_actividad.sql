-- Historial de actividad de los usuarios. usuario_id no tiene clave foránea para conservar
-- el registro de intentos de inicio de sesión con correos que no pertenecen a ninguna cuenta.
CREATE TABLE registros_actividad (
    id UUID NOT NULL,
    usuario_id UUID,
    email VARCHAR(100),
    tipo VARCHAR(40) NOT NULL,
    detalle VARCHAR(500),
    creado_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT registros_actividad_pkey PRIMARY KEY (id),
    CONSTRAINT ck_registros_actividad_tipo CHECK (tipo IN (
        'INICIO_SESION', 'INICIO_SESION_FALLIDO', 'INICIO_SESION_BLOQUEADO', 'CIERRE_SESION',
        'RESERVA_CREADA', 'RESERVA_CANCELADA', 'VERIFICACION_ACTUALIZADA', 'INFORME_GENERADO'))
);

CREATE INDEX idx_registros_actividad_creado_en ON registros_actividad (creado_en DESC);
CREATE INDEX idx_registros_actividad_usuario ON registros_actividad (usuario_id, creado_en DESC);
