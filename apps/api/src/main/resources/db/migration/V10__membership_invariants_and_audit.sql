ALTER TABLE membresias ALTER COLUMN proximo_vencimiento DROP NOT NULL;
ALTER TABLE membresias ADD CONSTRAINT ck_membresias_estado CHECK (estado IN ('PENDIENTE_PAGO','ACTIVA','VENCIDA','SUSPENDIDA','CANCELADA'));
ALTER TABLE nivel_membresia_precio_relacion ADD CONSTRAINT ck_precio_membresia_positivo CHECK (precio > 0);
CREATE UNIQUE INDEX uk_nivel_nombre_normalizado ON nivel_membresias (lower(nombre));
CREATE TABLE cambios_socios (
 id UUID PRIMARY KEY, socio_id UUID NOT NULL REFERENCES socios(id),
 responsable_id UUID NOT NULL REFERENCES usuarios(id), motivo VARCHAR(500) NOT NULL,
 detalle VARCHAR(2000) NOT NULL, fecha TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_cambios_socios_fecha ON cambios_socios(socio_id, fecha);

ALTER TABLE membresias ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
