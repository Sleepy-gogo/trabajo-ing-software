ALTER TABLE espacios ADD COLUMN estado VARCHAR(30) NOT NULL DEFAULT 'HABILITADO';
ALTER TABLE espacios ADD CONSTRAINT ck_espacio_estado CHECK (estado IN ('HABILITADO','MANTENIMIENTO','INUTILIZABLE','EN_USO'));
CREATE TABLE espacio_tarifas (
 espacio_id UUID NOT NULL REFERENCES espacios(id), relacion VARCHAR(30) NOT NULL,
 importe NUMERIC(12,2) NOT NULL CHECK(importe >= 0), PRIMARY KEY(espacio_id,relacion)
);
CREATE TABLE bloqueos_espacios (
 id UUID PRIMARY KEY, espacio_id UUID NOT NULL REFERENCES espacios(id),
 fecha DATE NOT NULL, desde TIME NOT NULL, hasta TIME NOT NULL, motivo VARCHAR(500) NOT NULL,
 CONSTRAINT ck_bloqueo_horario CHECK(desde < hasta)
);
CREATE INDEX ix_bloqueos_espacio_fecha ON bloqueos_espacios(espacio_id,fecha);
CREATE INDEX ix_disponibilidades_espacio ON disponibilidades(id_espacio);
