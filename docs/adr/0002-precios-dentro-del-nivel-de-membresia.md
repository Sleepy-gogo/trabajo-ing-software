# Precios dentro del nivel de membresía

Estado: aceptado.

## Contexto

El diseño inicial tenía una entidad `TarifaMembresia` y además un `cargoMensual` en `Membresia`.
Ambos podían representar el mismo importe, aunque todavía no existían mapeos JPA ni migraciones para
este incremento. El único dato que necesita el negocio es el precio del nivel según la relación con
la UNSE.

## Decisión

`NivelMembresia` mantiene un `Map<RelacionUnse, BigDecimal>` llamado `preciosPorRelacion`. No se
crea una entidad, repository ni DTO separado para tarifas.

`Membresia` mantiene la contratación, el nivel, el estado y las fechas. No mantiene importes. Cuando
se genera una `Cuota`, el módulo de pagos resuelve el precio vigente del nivel para la relación del
socio y guarda el importe aplicado en esa cuota. Los pagos conservan el historial.

## Consecuencias

El modelo tiene menos clases y la consulta de un plan no necesita recorrer otra entidad. Un cambio de
precio se aplica al siguiente período. No se congela el precio de una contratación.
