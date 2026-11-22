UPDATE socios
SET estado_verificacion_unse = 'VERIFICADA',
    updated_at = now()
WHERE relacion_unse = 'EXTERNO'
  AND estado_verificacion_unse = 'PENDIENTE';
