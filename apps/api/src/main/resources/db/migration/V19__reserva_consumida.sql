ALTER TABLE reservas ADD COLUMN consumida_en TIMESTAMP WITH TIME ZONE;
ALTER TABLE reservas ADD COLUMN consumida_por UUID REFERENCES usuarios(id);
ALTER TABLE reservas ADD CONSTRAINT ck_reserva_consumida CHECK (
  (consumida_en IS NULL AND consumida_por IS NULL) OR
  (consumida_en IS NOT NULL AND consumida_por IS NOT NULL AND estado = 'CONFIRMADA')
);
