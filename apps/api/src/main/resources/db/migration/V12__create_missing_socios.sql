-- Every account has one socio; membership remains optional.
INSERT INTO socios(id,usuario_id,relacion_unse,estado_verificacion_unse,created_at,updated_at)
SELECT gen_random_uuid(),u.id,'EXTERNO','PENDIENTE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM usuarios u WHERE NOT EXISTS(SELECT 1 FROM socios s WHERE s.usuario_id=u.id);
