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

Pendientes: significado de C/N/A y colores; claves estables de filas/clientes/deudas; reglas de asignación a varias deudas; política de reimportación; cierre con pendientes; reversos y correcciones; alcance y retención de evidencias; recuperación/respaldo; parque Android y minSdk 26; contenido real de Yape/Interbank; conservación de fórmulas, formatos y otras hojas al exportar. No inventar decisiones de estas áreas.

## Privacidad y límites de la fase 0

Usar datos sintéticos en pruebas; Excel real y comprobantes fuera de Git. Crashlytics previsto para pruebas, pendiente de configuración real compatible con `com.pe.cobrorutapp`. No enviar DNI, nombres, montos, comprobantes ni contenido de notificaciones; sin Analytics. No se crean proyectos Firebase remotos.

La fase 0 prepara estructura, DI, preferencias de tema y navegación. No procesa Excel, notificaciones, imágenes, deudas, pagos, gastos o jornadas reales.
