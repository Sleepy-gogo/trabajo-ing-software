# ADR 0003: socio automático y membresía única

Estado: aceptado por el equipo el 21 de septiembre de 2026.

Cada usuario crea su socio en la misma transacción. El socio empieza con verificación UNSE
pendiente y puede existir sin membresía. Cada socio puede tener como máximo una membresía.

Una contratación empieza pendiente de pago. Solo la aprobación del pago recibida por el webhook
podrá activarla, dentro del incremento de pagos. La cancelación es inmediata y una nueva solicitud
reutiliza la misma membresía. Se conserva una auditoría de los cambios.

Se descarta el modelo de múltiples membresías con vigencia desde/hasta. Las fechas de alta, baja y
próxima cuota no representan ese modelo descartado.

La entrega hasta el incremento 3 no incluye pagos ni webhook. Debe mostrar la solicitud pendiente
sin habilitar beneficios ficticios.
