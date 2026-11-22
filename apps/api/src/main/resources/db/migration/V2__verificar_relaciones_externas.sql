-- Quien se declara externo no tiene una relación con la UNSE que verificar.
-- Las cuentas externas que quedaron pendientes pasan a verificadas para que puedan ingresar.
UPDATE socios
SET estado_verificacion_unse = 'VERIFICADA',
    updated_at = now()
WHERE relacion_unse = 'EXTERNO'
  AND estado_verificacion_unse = 'PENDIENTE';
