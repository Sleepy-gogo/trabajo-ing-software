ALTER TABLE membresias ADD COLUMN contratacion_id UUID;
UPDATE membresias SET contratacion_id = id WHERE contratacion_id IS NULL;
ALTER TABLE membresias ALTER COLUMN contratacion_id SET NOT NULL;

ALTER TABLE pagos ADD COLUMN contratacion_id UUID;
UPDATE pagos p SET contratacion_id = m.contratacion_id
FROM membresias m WHERE p.membresia_id = m.id;
ALTER TABLE pagos ADD COLUMN clave_solicitud UUID;
UPDATE pagos SET clave_solicitud = id WHERE clave_solicitud IS NULL;
ALTER TABLE pagos ALTER COLUMN clave_solicitud SET NOT NULL;
ALTER TABLE pagos ADD COLUMN relacion_aplicada VARCHAR(30);
UPDATE pagos SET relacion_aplicada = 'EXTERNO' WHERE relacion_aplicada IS NULL;
ALTER TABLE pagos ALTER COLUMN relacion_aplicada SET NOT NULL;
ALTER TABLE pagos ADD COLUMN nivel_nombre_aplicado VARCHAR(255);
UPDATE pagos p SET nivel_nombre_aplicado = n.nombre
FROM membresias m JOIN nivel_membresias n ON n.id = m.nivel_membresia_id
WHERE p.membresia_id = m.id;
UPDATE pagos SET nivel_nombre_aplicado = 'Membresía' WHERE nivel_nombre_aplicado IS NULL;
ALTER TABLE pagos ALTER COLUMN nivel_nombre_aplicado SET NOT NULL;
ALTER TABLE pagos ADD COLUMN aprobado_en TIMESTAMP WITH TIME ZONE;
ALTER TABLE pagos ADD COLUMN mercado_pago_factura_id BIGINT;
ALTER TABLE pagos ADD COLUMN aplicado_en TIMESTAMP WITH TIME ZONE;
ALTER TABLE pagos ADD COLUMN vencimiento_anterior DATE;
ALTER TABLE pagos ADD COLUMN vencimiento_resultante DATE;
ALTER TABLE pagos ADD COLUMN requiere_revision BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pagos ADD COLUMN motivo_revision VARCHAR(500);
CREATE UNIQUE INDEX ux_pagos_usuario_clave ON pagos (usuario_id, clave_solicitud);
CREATE UNIQUE INDEX ux_pagos_pendiente_membresia
ON pagos (membresia_id, contratacion_id) WHERE estado = 'PENDIENTE';
CREATE INDEX ix_pagos_usuario_fecha ON pagos (usuario_id, created_at DESC);
CREATE INDEX ix_pagos_factura ON pagos (mercado_pago_factura_id);
CREATE UNIQUE INDEX ux_pagos_factura_aplicada ON pagos (mercado_pago_factura_id)
WHERE aplicado_en IS NOT NULL AND mercado_pago_factura_id IS NOT NULL;
ALTER TABLE pagos ADD CONSTRAINT ck_pagos_monto_positivo CHECK (monto > 0);
