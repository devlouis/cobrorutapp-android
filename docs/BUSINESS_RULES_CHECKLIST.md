# Checklist de reglas de negocio

Este documento separa las reglas ya confirmadas de las definiciones pendientes. Sirve para continuar las conversaciones de producto mientras se desarrolla la POC de notificaciones; no autoriza implementar cobranza ni cierra las decisiones pendientes.

## Créditos y cuotas

- [x] Un cliente puede tener hasta tres préstamos activos simultáneamente. El límite no elimina ni restringe el historial de préstamos pagados.
- [x] El interés es 20 % del capital, aplicado una sola vez por todo el crédito.
- [x] Las cuotas ya incluyen capital e interés; no se vuelve a sumar interés al saldo importado.
- [x] Para capital menor o igual a S/400, el plazo es de 20 días.
- [x] Para capital mayor a S/400, el plazo es de 24 días.
- [ ] Definir cómo se redondean las cuotas y cómo se ajusta la última cuota cuando la división no es exacta.
- [ ] Definir cómo se aplican pagos parciales y adelantos.
- [ ] Definir qué ocurre cuando un pago supera el saldo del préstamo o del cliente.
- [ ] Definir el tratamiento de un archivo importado que contenga más de tres préstamos activos de un cliente, sin descartar deudas silenciosamente.
- [ ] Definir cómo se distribuye un pago cuando el cliente tiene varios préstamos activos. La consulta fue enviada al cobrador y se espera su respuesta.

## Calendario y avisos

- [x] El cobrador recibe un aviso tres días antes del vencimiento final de cada crédito.
- [x] Si un cliente tiene varios créditos activos, el aviso corresponde a cada crédito por separado.
- [x] El aviso se refiere al vencimiento final del crédito, no al vencimiento de cada cuota.
- [ ] Definir el calendario de cobranza. La conversación sobre lunes a sábado y feriados quedó pospuesta y no constituye una regla cerrada.
- [ ] Definir la fecha de la primera cuota, las excepciones y el ajuste del vencimiento.
- [ ] Definir si los tres días de anticipación se cuentan como días calendario o días de cobranza.
- [ ] Definir la hora y la repetición del aviso.

## Registro y confirmación de pagos

- [x] El remitente del pago y el cliente deudor se identifican por separado; pueden ser la misma persona o un tercero que paga por el cliente.
- [x] Una coincidencia de nombre solo genera una sugerencia. El cobrador siempre confirma cliente, deuda y monto, incluso cuando existe una sola coincidencia.
- [x] Un ingreso pendiente no modifica el saldo.
- [x] Los pendientes persisten al cerrar la app y al cambiar de día.
- [x] La confirmación del pago y su aplicación a la deuda deben ser idempotentes y ocurrir en una misma transacción.
- [x] Las correcciones de pagos confirmados conservan el historial y el motivo.
- [ ] Definir el flujo exacto de reversos y correcciones.
- [ ] Definir qué ocurre con los pendientes al cerrar una jornada.

## Duplicados y evidencias

- [x] Una notificación, una captura y un registro manual pueden ser evidencias del mismo pago.
- [x] Se conservan las evidencias sin duplicar el abono aplicado.
- [x] Nombre y monto no identifican de forma única un pago; dos pagos iguales pueden ser legítimos.
- [x] Un duplicado técnico y una sospecha revisable reciben tratamientos distintos.
- [x] Una imagen o el resultado de OCR no confirma por sí solo un ingreso bancario.
- [ ] Definir el alcance y el tiempo de retención de las evidencias.
- [ ] Definir los criterios concretos para clasificar un duplicado técnico y una sospecha revisable.

## Saldos, importación y Excel

- [x] El saldo importado es la base y solo se descuentan los nuevos abonos; el historial no se descuenta nuevamente.
- [x] El dinero se representa en céntimos con `Long` y moneda explícita, nunca con `Double`.
- [x] C y N son marcas equivalentes: la cuota del día no fue pagada.
- [x] Al importar y exportar se conserva la representación original de C o N mientras no exista otra política acordada.
- [x] A significa adelanto; esta marca todavía no determina cómo distribuir el importe entre cuotas.
- [x] Según la entrevista, amarillo representa el restante de la deuda, azul todo lo abonado hasta la fecha, plomo la cuota del pago del día y verde las cuotas canceladas.
- [ ] Validar C, N, A, los colores y su representación contra el Excel real antes de convertirlos en reglas del archivo.
- [ ] Definir claves estables para filas, clientes y deudas, incluyendo clientes homónimos.
- [ ] Confirmar monedas admitidas y manejo de archivos con moneda ausente o distinta.
- [ ] Diseñar y probar la política de reimportación antes de habilitarla, sin borrar ni duplicar cobros.
- [ ] Validar la conservación de fórmulas, formatos y otras hojas al exportar.

## Jornada, gastos y recuperación

- [x] El cobrador confirma sus cobros; el supervisor no aprueba pagos dentro de la app.
- [ ] Definir las reglas de gastos y caja para el cierre de jornada.
- [ ] Definir la recuperación y el respaldo de la información local.
