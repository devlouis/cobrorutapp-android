# Contexto del producto

## Objetivo y actores

CobroRutApp ayuda a cobradores Android que trabajan por rutas y reciben efectivo, Yape y Plin de Interbank. Reduce transcripción al Excel, errores y duplicados, y conserva historial. Cada cobrador maneja su ruta y confirma sus cobros. El supervisor entrega el Excel diario y recibe el cierre; no aprueba pagos dentro de la app.

Primera versión local y sin conexión para las operaciones de cobranza. Sin backend, sincronización entre dispositivos, portal administrativo ni integración bancaria directa. Los ingresos digitales llegan a la cuenta asignada a la ruta.

## Flujo acordado

1. Supervisor envía Excel del día; cobrador importa clientes, deudas y saldos para iniciar jornada.
2. Efectivo se registra manualmente. En digital, el cobrador verifica ingreso por notificación o entrando a Yape/Interbank.
3. Se propone detectar notificaciones del mismo Android y guardar ingresos pendientes de asociación; contenido y viabilidad requieren dispositivo real.
4. Opcionalmente comparte una captura recibida por WhatsApp: OCR sugiere datos, nunca confirma ingreso por sí solo.
5. Cobrador revisa cliente, deuda y monto, y confirma. Solo entonces se aplica el abono.
6. Registra gastos, revisa pendientes/cierre y exporta el Excel actualizado para enviarlo al supervisor.

## Invariantes para próximas fases

Room será fuente de verdad de la jornada; Excel es entrada/salida. Partir del saldo importado y aplicar únicamente nuevos abonos para no descontar historial dos veces. Dinero con `Long` en céntimos y moneda explícita.

Separar remitente del pago de cliente deudor: un tercero puede pagar. Coincidencia por nombre solo sugiere; incluso una única coincidencia exige confirmación humana. Ingreso sin identificar permanece persistido y no modifica saldos.

Notificación/captura/manual pueden referirse al mismo pago: conservar evidencias sin duplicar aplicaciones. Nombre + monto no es clave única: dos pagos iguales pueden ser legítimos. Duplicados técnicos e indicios de duplicidad deben tratarse de forma distinta.

Confirmar pago y aplicar a deuda dentro de una transacción idempotente. Correcciones de confirmados conservan historial y motivo. Antes de habilitar reimportación hay que diseñar y probar una política que no borre ni duplique cobros. Los pendientes sobreviven cierre e inicio de otro día.

Captura independiente de pantallas/ViewModel. No prometer recepción infalible con pantalla bloqueada: verificar restricciones por dispositivo. Avisos tocables, sin apertura automática de pantallas. Recoger únicamente notificaciones de fuentes/tipos necesarios; solicitar permisos solo con implementación y explicación.

## Archivo de referencia y dudas

`2SJL17ABRIL26.xlsx` no estaba en el repositorio al inspeccionarlo en fase 0; **no fue inspeccionado**. Según el contexto aportado, la hoja principal se llama Plan1 y contiene clientes, operaciones, cuotas, saldos, abonos por fecha, gastos y caja. Esto requiere validación contra el archivo real. Durante el cierre de fase 0 el usuario proporcionó el archivo en una ubicación local externa; se comprobó su existencia, no su contenido. Está disponible para un encargo de fase 1 y no se copió a Git.

Pendientes: aplicación exacta de C/N/A y colores en el archivo; claves estables de filas/clientes/deudas; reglas de asignación a varias deudas; política de reimportación; cierre con pendientes; reversos y correcciones; alcance y retención de evidencias; recuperación/respaldo; parque Android y minSdk 26; contenido real de Yape y formatos alternativos de Interbank; conservación de fórmulas, formatos y otras hojas al exportar. No inventar decisiones de estas áreas.

## Privacidad y límites de la fase 0

Usar datos sintéticos en pruebas; Excel real y comprobantes fuera de Git. Crashlytics previsto para pruebas, pendiente de configuración real compatible con `com.pe.cobrorutapp`. No enviar DNI, nombres, montos, comprobantes ni contenido de notificaciones; sin Analytics. No se crean proyectos Firebase remotos.

La fase 0 prepara estructura, DI, preferencias de tema y navegación. No procesa Excel, notificaciones, imágenes, deudas, pagos, gastos o jornadas reales.

## Entrevista al cobrador — 2026-09-15

Fuente: apuntes compartidos por el usuario. Son definiciones reportadas por el trabajador; no provienen de inspección del Excel ni constituyen reglas de cálculo implementadas.

| Marca | Significado reportado |
| --- | --- |
| C | No pagó la cuota del día |
| N | Nada, no pagó la cuota |
| A | Adelanto |
| Amarillo | Restante de deuda |
| Azul | Todo lo abonado hasta la fecha |
| Plomo | Cuota correspondiente al pago del día |
| Verde | Cuotas canceladas |

El usuario confirmó que C y N significan lo mismo: no pagó la cuota del día. Son marcas equivalentes de negocio; conservar su representación original al importar/exportar hasta definir la política de conservación del Excel. Adelanto no define todavía cómo distribuir el importe entre cuotas. Cuotas canceladas se registra tal como fue expresado; validar su representación en el Excel, sin asumir columnas o códigos RGB.

Un cliente puede tener hasta tres préstamos activos simultáneamente, confirmado por el usuario. Este límite no restringe ni elimina el historial de préstamos pagados.

Interés confirmado: 20 % aplicado una sola vez sobre el capital por todo el crédito; la cuota diaria ya incluye capital e interés. Ejemplo validado por el usuario: capital S/400, interés S/80, total S/480, 20 cuotas de S/24. Falta definir redondeo y ajuste de cuotas cuando la división no sea exacta, y verificar cómo representa el Excel estos importes. No sumar nuevamente intereses al saldo importado ni reconstruirlo descontando dos veces el historial. Esto documenta una regla del producto, no habilita todavía originación de créditos o cálculos en la app.

Plazos confirmados por el usuario: capital hasta S/400 inclusive, 20 días; capital mayor que S/400, 24 días. Esta aclaración sustituye el umbral preliminar de S/500 y resuelve el intervalo antes pendiente: S/400 corresponde a 20 días; S/450 y S/500 corresponden a 24 días. Calendario preliminar indicado: cobranza de lunes a sábado, incluyendo feriados salvo feriados nacionales. El usuario pidió definir esta información después: no cerrar aún cómputo del plazo, excepciones, fecha de primera cuota o ajuste del vencimiento. Tampoco está definido si los tres días de anticipación del aviso son calendario o de cobranza.

Avisar al cobrador tres días antes del vencimiento final de cada crédito del cliente, confirmado por el usuario; el aviso se trata por préstamo cuando existan varios simultáneos. No se refiere al vencimiento de cada cuota. Faltan hora, repetición y cómputo de esos tres días. No hay notificaciones ni permisos implementados para esta función.

Siguientes definiciones: aplicación de un pago a varios préstamos, pagos parciales/adelantos, redondeo, reimportación y correcciones. Retomar calendario y detalles de avisos más adelante, según lo solicitado por el usuario.

## Seguimiento de definiciones

El [checklist de reglas de negocio](BUSINESS_RULES_CHECKLIST.md) distingue reglas confirmadas y pendientes. La distribución de un pago entre varios préstamos se consultó al cobrador y está a la espera de respuesta; no bloquea la POC técnica de notificaciones autorizada en fase 1.
