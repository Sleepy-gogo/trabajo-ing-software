# ADR 0004: Pago único y renovación prepaga

Estado: aceptado para el incremento de pagos del 27 de septiembre de 2026.

Cada cobro de membresía es una fila de `Pago`. Su importe y contexto tarifario se fijan al
iniciarlo. Un pago aprobado y verificado extiende una sola vez `Membresia.proximoVencimiento`.
Si se paga antes del vencimiento, el mes se suma a esa fecha; si se paga después, se cuenta
desde la fecha efectiva del cobro. Los beneficios requieren un vencimiento posterior al día
actual de Buenos Aires. El día del vencimiento ya no están vigentes.

No se emiten cuotas ni se acumula una deuda por el mero paso del tiempo. La falta de renovación
deja la membresía `VENCIDA`; `SUSPENDIDA` queda reservada para una decisión administrativa y un
cobro recibido durante esa suspensión requiere revisión sin reactivar beneficios. Un aviso
repetido del proveedor no extiende la fecha de nuevo.

La contratación conserva un identificador propio aunque se reutilice la fila de membresía
después de cancelarla. Cada pago guarda ese identificador, una clave de solicitud y, cuando
corresponde, el identificador del cobro externo. Un cobro tardío de una contratación anterior
queda registrado para revisión. El backend verifica los cobros de Mercado Pago mediante su API;
el retorno del navegador no constituye una aprobación.

Esta decisión sustituye las referencias a «próxima cuota», deuda acumulada y suspensión
automática por impago del ADR 0003 y de los documentos anteriores. El modelo de socio
automático y membresía única del ADR 0003 continúa vigente.
