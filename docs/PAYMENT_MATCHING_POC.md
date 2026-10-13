# POC de asociación y registro de ingresos

## Objetivo

Validar cómo convertir una evidencia de Plin o un ingreso manual en un borrador que el cobrador pueda revisar, asignar y confirmar. La POC no aplica pagos, no modifica saldos y no define todavía el esquema Room de cobranza.

## Decisión de producto propuesta

Usar una sola bandeja para ingresos digitales pendientes. Cada elemento conserva su origen y permanece pendiente hasta que el cobrador confirme cliente, préstamo y monto. El registro manual sigue un flujo distinto: después de confirmar explícitamente todos sus datos, se aplica de inmediato.

- Plin con coincidencia: mostrar una o varias sugerencias, sin asignar automáticamente.
- Plin sin coincidencia: mostrar `Sin coincidencia` y permitir buscar cliente o dejarlo pendiente.
- Efectivo o ingreso manual: el asistente permite una captura rápida como `pablito1 20` o `juan mototaxi 150.50`, crea un borrador pendiente y muestra lo interpretado al revisarlo. No exige escribir el signo de moneda. Al confirmar explícitamente durante la revisión, el pago se aplica de inmediato.
- Un nombre coincidente nunca confirma un pago. El remitente puede ser un tercero distinto del cliente deudor.
- La POC conserva la evidencia original y la sugerencia por separado. Cambiar una sugerencia no altera la evidencia.

## Evidencia del Excel

Inspección local y de solo lectura de `Plan1`, sin copiar datos reales al repositorio:

- Encabezados relevantes: `NOMBRE`, `DNI`, `FECHA`, `VENTA`, `SERVICIO`, `TOTAL`, `V.CUOTA`, `SALDO` y `ABONO`.
- 3,909 filas contienen nombre y producen 2,047 nombres distintos después de normalizar mayúsculas, tildes, puntuación y espacios.
- 696 nombres normalizados aparecen en más de una fila; 2,558 filas pertenecen a esos grupos y un nombre llega a 22 filas.
- Solo 53 filas contienen DNI.
- `SALDO` y `ABONO` contienen numerosas fórmulas, por lo que la importación no puede tratar el libro como una lista plana ni sobrescribirlo sin validar round-trip.

Estos conteos no demuestran que las filas repetidas sean personas duplicadas: pueden representar operaciones, créditos u otra estructura todavía no definida. Sí demuestran que el texto del nombre no es una clave estable para cliente o deuda.

## Flujo de Plin

1. El listener conserva una evidencia técnica del aviso permitido.
2. Un parser específico extrae remitente visible y monto en céntimos. Si el formato no se reconoce, la evidencia queda pendiente sin inventar campos.
3. El matcher consulta el directorio importado y devuelve cero, una o varias sugerencias.
4. La bandeja muestra evidencia, sugerencias y estado pendiente.
5. El cobrador selecciona o busca al cliente deudor.
6. Si el cliente tiene varios préstamos activos, el cobrador selecciona el préstamo o la distribución definida posteriormente.
7. Una pantalla de revisión muestra remitente, cliente, préstamo y monto antes de confirmar.
8. La aplicación del abono queda fuera de esta POC y requerirá una transacción idempotente en fase 2.

## Política inicial de sugerencias

Orden recomendado:

1. Nombre completo normalizado exactamente igual.
2. Alias confirmado y vinculado explícitamente a un cliente.
3. Coincidencia por todos los términos del nombre, solo como lista de candidatos.
4. Sin coincidencia cuando faltan términos o existen resultados demasiado amplios.

Normalizar solo para buscar: mayúsculas/minúsculas, tildes, signos y espacios. Conservar siempre el texto original para mostrarlo. No usar monto, hora o cercanía ortográfica como identidad. No elegir automáticamente al primer resultado. Una única sugerencia también requiere confirmación.

Los alias no se deducen del Excel ni del texto bancario. El cobrador debe vincularlos de forma explícita y poder corregirlos; falta definir si un alias puede pertenecer a más de un cliente.

## Asistente manual tipo chat

La primera versión será un formulario conversacional determinista, no un modelo generativo. Está optimizado para que el cobrador capture pagos con pocos caracteres mientras recorre la ruta. Ejemplos sintéticos:

- `pablito1 20` → búsqueda `pablito1`, S/20.00
- `juan mototaxi 150.50` → búsqueda `juan mototaxi`, S/150.50

El último elemento numérico se interpreta como monto y todo el texto anterior como nombre o alias de búsqueda. El signo `S/` es opcional y la moneda predeterminada es PEN. El importe admite soles enteros o uno/dos decimales y se convierte a céntimos sin `Double`. El medio se toma del contexto seleccionado en la pantalla, por ejemplo efectivo, para no exigir otra palabra en cada registro.

Al enviar el texto, el asistente guarda un borrador pendiente y permite continuar de inmediato con el siguiente cobro. No aplica el abono ni obliga a resolver coincidencias durante la ruta. Al terminar, el cobrador abre la lista de revisión; si falta el monto, hay varias coincidencias o existen varios préstamos, completa esos datos mediante opciones visibles. Solo después de confirmar la tarjeta completa se aplica el pago.

Flujo recomendado:

1. Seleccionar el medio para la captura rápida, por ejemplo efectivo.
2. Escribir nombre registrado o alias y monto, sin necesidad de `S/`.
3. Guardar el borrador pendiente y permitir capturar el siguiente pago.
4. Al finalizar la ruta, revisar coincidencias y elegir cliente y préstamo.
5. Revisar medio, remitente si corresponde, cliente, préstamo, moneda y monto.
6. Confirmar explícitamente para aplicar el pago de inmediato. Mientras falte o sea ambiguo cualquier dato, la confirmación permanece deshabilitada.

Para pagos de terceros se necesita un flujo posterior que permita expresar por separado quién entregó el dinero y a qué cliente corresponde.

## Modelo conceptual para la POC

- `PaymentEvidence`: fuente, identificador técnico, texto original acotado y fecha.
- `PaymentDraft`: moneda, monto opcional, remitente opcional, cliente seleccionado opcional y estado de revisión.
- `CustomerCandidate`: referencia importada, nombre mostrado y motivo de sugerencia.
- `DebtCandidate`: referencia de deuda y datos mínimos para que el cobrador la distinga.
- `CustomerAlias`: alias confirmado y referencia estable del cliente.

Las referencias estables de cliente y deuda siguen sin definirse. El número de fila del Excel no es suficiente para reimportación porque puede cambiar entre archivos.

## Entregas técnicas de la POC

1. Parser Plin para el formato real ya observado, con fixtures sintéticos y monto en céntimos.
2. Matcher puro de nombres y alias con resultados cero/uno/varios.
3. Directorio sintético para probar homónimos, terceros, tildes, orden de nombres y alias.
4. Bandeja debug con estados `Sugerencia`, `Varias coincidencias` y `Sin coincidencia`.
5. Asistente manual determinista con tarjeta de revisión.
6. Prueba read-only del directorio importado desde el Excel real, sin aplicar pagos ni exportar.

## Criterios de aceptación

- Ningún candidato modifica saldo antes de la confirmación humana.
- Una coincidencia única sigue pendiente.
- Cero y varias coincidencias permanecen visibles y recuperables.
- La captura manual `consulta monto` se guarda como borrador sin exigir el signo de moneda ni interrumpir la ruta para desambiguar.
- El cobrador puede corregir cliente y préstamo sin modificar la evidencia.
- Los importes usan `Long` en céntimos y moneda explícita.
- Un remitente distinto del cliente puede asignarse correctamente.
- Tests y repositorio usan únicamente datos sintéticos.
- Cerrar la app o cambiar de día no elimina pendientes cuando se implemente persistencia de fase 2.

## Definiciones pendientes

- Clave estable de cliente y deuda entre importaciones.
- Política de alias, homónimos y corrección de alias equivocados.
- Distribución del pago cuando existen varios préstamos activos.
- Pagos parciales, adelantos, excedentes y redondeo.
- Flujo exacto para efectivo entregado por un tercero.
