# Propuesta de POC — Excel, directorio, asistente y notificaciones

## Objetivo

Validar en un dispositivo el flujo:

```text
Seleccionar Excel → validar/importar filas → consultar directorio
                                      ↓
           captura manual o aviso Plin → sugerir coincidencias
                                      ↓
                     bandeja pendiente → revisión humana
```

La primera entrega será una prueba técnica de lectura y asociación. No actualizará el Excel, no aplicará pagos reales, no exportará un cierre y no modificará saldos importados.

## Navegación y menú lateral

Mantener las tres pestañas como flujo principal:

- Jornada.
- Cobranza.
- Ingresos.

Un `ModalNavigationDrawer` contendrá funciones transversales que no deben ocupar una cuarta pestaña:

- `Importar archivo`.
- `Clientes`.
- `Configuración` para tema y futuras preferencias.

La barra superior tendrá botón de menú. Las pantallas abiertas desde el drawer ocultarán la barra inferior y el FAB del asistente; Atrás regresará a la última pestaña activa. El drawer seguirá disponible desde sus pantallas raíz. El diagnóstico de notificaciones continuará en Ingresos durante la POC y no se convertirá en una opción general del drawer.

Navigation 3 administrará tres back stacks de trabajo y destinos globales de utilidad. El estado recordará la pestaña activa al entrar y salir de Importar o Clientes.

## Pantalla Importar archivo

Flujo propuesto:

1. Elegir `.xlsx` mediante el selector de documentos de Android (`ACTION_OPEN_DOCUMENT`), sin permiso general de almacenamiento.
2. Mostrar nombre del archivo y advertir que contiene datos personales.
3. Analizar en segundo plano sin bloquear la UI.
4. Validar hoja, encabezados, tipos, fórmulas y límites.
5. Mostrar un resumen antes de aceptar: filas leídas, filas utilizables, advertencias y errores.
6. Pulsar `Importar para prueba` para construir el directorio local de la sesión.

Regla temporal aprobada: una fila solo es elegible si contiene `NOMBRE` y una celda `DNI` no vacía. El filtro comprueba presencia, no validez oficial del DNI. Las filas excluidas se contabilizan y se muestran en el resumen, pero no entran al directorio ni al matcher.

No se evaluarán fórmulas dentro de la app. Se leerá el valor calculado guardado por Excel; si una fórmula no tiene resultado disponible, la fila mostrará una advertencia en lugar de inventar el valor.

La POC rechazará archivos cifrados, formatos distintos de `.xlsx`, hojas sin `Plan1` y estructuras incompatibles. Debe limitar tamaño y cantidad de filas antes de cargar todo en memoria, ignorar enlaces externos y no ejecutar macros ni contenido activo.

## Campos de importación

Inspección read-only del archivo de referencia en `Plan1!C1:M4062`:

| Columna | Modelo propuesto | Observación real del archivo |
| --- | --- | --- |
| `DIRECCION` | texto opcional | Encabezado presente; 4,061 filas vacías |
| `NOMBRE` | texto original requerido para una fila utilizable | 3,909 valores; existen nombres repetidos |
| `DNI` | texto obligatorio para esta POC; formato aún no validado | Solo 53 valores; 52 provienen de celdas numéricas y uno de texto |
| `FECHA` | fecha importada opcional con advertencia si es inválida | 3,876 valores no vacíos |
| `VENTA` | dinero PEN en céntimos | 3,874 valores no vacíos |
| `SERVICIO` | valor fuente opcional, sin significado asumido | Existe en el archivo aunque no estaba en la lista solicitada |
| `TOTAL` | dinero PEN en céntimos | 4,008 valores no vacíos |
| `V.CUOTA` | dinero PEN en céntimos | 4,008 valores no vacíos |
| `SALDO` | dinero PEN en céntimos; base importada | 4,006 valores; 1,189 fórmulas |
| `ABONO` | dinero PEN en céntimos; dato importado informativo | 4,005 valores; 1,292 fórmulas |
| `No CUOTA` | valor original y número opcional si es entero válido | 4,005 valores; 1,175 fórmulas y 133 valores textuales |

Se recomienda conservar `SERVICIO` como campo fuente opcional. No se le asignará una regla de negocio hasta confirmarla, pero descartarlo impediría explicar o validar algunas filas y dificultaría el round-trip posterior.

El filtro temporal importaría 53 filas con nombre y DNI, y excluiría 3,856 filas que sí tienen nombre pero no DNI. Las 53 celdas no vacías son únicas en este archivo, pero sus valores crudos tienen entre uno y tres caracteres; ninguna tiene ocho dígitos y no existe un formato numérico de celda que complete ceros. Por tanto, se mostrarán como `DNI presente — formato no validado` y no se afirmará que sean DNIs peruanos válidos.

El DNI debe conservarse como texto exactamente para identificación visual; nunca como `Double`. La futura validación estricta de ocho dígitos queda separada de este filtro de presencia. Las fechas deben interpretarse usando el tipo/estilo de celda. Todo campo monetario se convierte a `Long` en céntimos con moneda PEN.

`ABONO` representa el dato que ya venía en el archivo. No debe convertirse en un nuevo `Payment` ni volver a descontarse del saldo. Los pagos capturados por la app son movimientos separados posteriores a la importación.

`No CUOTA` no se usará como identificador de crédito. Se mostrará como posición de cuota importada; los valores textuales o de fórmula requieren tolerancia y validación.

## Modelo seguro para la POC

No conviene convertir cada nombre único directamente en un cliente. El archivo contiene nombres repetidos y pocos DNI, por lo que nombre y fila no son identificadores estables.

Modelos conceptuales:

- `ImportBatch`: archivo, hoja, instante de importación, conteos y estado.
- `ImportedCreditRow`: referencia de lote + fila fuente y todos los campos importados.
- `CustomerSuggestion`: agrupación o candidato propuesto, nunca identidad confirmada.
- `PaymentDraft`: captura manual o digital pendiente de revisión.
- `PaymentEvidence`: texto técnico de notificación u origen manual.

Para esta POC, `ImportBatchId + sourceRowNumber` sirve como referencia interna de una fila dentro de una importación. No es una identidad estable para reimportar otro archivo. Cada fila se conserva aunque comparta nombre con otras.

Cuando existe DNI se puede presentar como dato de desambiguación, pero la POC no fusionará silenciosamente filas. En el archivo actual sus valores no tienen formato de DNI peruano completo, así que tampoco se usarán como identidad estable. Una política productiva de cliente/crédito requiere definir claves estables y reimportación antes de crear el esquema Room final.

## Pantalla Clientes

El elemento del drawer se llamará `Clientes`; la pantalla mostrará el título `Directorio importado` para dejar claro que proviene del archivo.

Contenido:

- resumen del lote activo y botón para ir a Importar si no existe uno;
- búsqueda por nombre, alias confirmado o DNI;
- lista con `LazyColumn`, preparada para aproximadamente cuatro mil filas;
- nombre, dirección cuando exista, DNI parcialmente oculto, fecha, venta, total, cuota, saldo, abono y número de cuota;
- indicador cuando el mismo nombre aparece en varias filas;
- detalle de fila/crédito al pulsar una tarjeta.

El DNI se mostrará parcialmente oculto por defecto. La dirección no ocupará espacio cuando esté ausente. No se registrarán consultas, nombres ni DNI en logs.

El encabezado de la pantalla informará que el directorio está filtrado por presencia de DNI y mostrará `53 importados · 3,856 con nombre excluidos` para el archivo de referencia. Esto evita que una prueba parezca representar toda la cartera.

La lista debe permitir distinguir varias operaciones del mismo nombre mediante fecha, total, saldo, valor/número de cuota y referencia de fila. No presentará todas las filas repetidas como si fueran automáticamente un solo cliente con varios préstamos.

## Integración con el asistente

El asistente puede capturar aunque todavía no exista un Excel importado. El borrador queda `Sin directorio`. Después de importar, el matching puede recalcularse sin modificar el texto ni el monto original.

Con un lote activo:

1. El comando rápido produce consulta y monto.
2. El matcher busca contra nombres y alias confirmados.
3. Una coincidencia de nombre puede devolver varias filas/créditos.
4. El cobrador sigue capturando sin resolverlas durante la ruta.
5. En la revisión elige cliente/fila de crédito y confirma los datos.

Para una sola fila sugerida también se mantiene la confirmación humana.

## Integración con Plin y Yape

El mismo directorio alimentará las sugerencias de las notificaciones:

1. conservar evidencia de la notificación permitida;
2. extraer remitente y monto cuando el formato sea reconocido;
3. buscar candidatos en el directorio importado;
4. crear un borrador digital pendiente;
5. revisar remitente, cliente, crédito y monto.

Una notificación nunca aplica un pago automáticamente. El remitente puede ser un tercero. Yape permanece pendiente de obtener y validar un formato real; no se reutilizará el parser Plin por suposición.

## Persistencia y alcance de las pruebas de campo

Hay dos niveles distintos:

### POC visual/técnica

- lectura read-only del Excel;
- directorio temporal;
- fixtures o archivo real controlado;
- matching y navegación;
- estado conservado durante navegación y recreación de actividad;
- ninguna aplicación a saldos.

Puede ejecutarse sin Room, pero perderá la sesión si Android termina el proceso. Sirve para validar formato, rendimiento y ergonomía, no para registrar una ruta real.

### Simulación de campo confiable

Para caminar una ruta y revisar al final sin riesgo de perder información se necesita antes una base Room mínima con:

- lote de importación;
- filas importadas;
- borradores manuales/digitales;
- evidencias;
- aliases o vínculos confirmados cuando se definan.

Esta base debe probar reinicio, transacciones, idempotencia y migraciones. Es el inicio técnico de fase 2. DataStore no es apropiado para estos datos. Aun con Room, la simulación puede mantener deshabilitada la modificación de saldos y exportación hasta cerrar las reglas pendientes.

## Secuencia recomendada

1. Migrar la navegación a Navigation 3 e incorporar drawer y FAB.
2. Crear la pantalla Importar y un lector read-only en `:integration:excel`.
3. Validar el archivo real en emulador y Z2474: memoria, tiempo, fórmulas y errores.
4. Mostrar Directorio importado sin agrupar silenciosamente filas.
5. Conectar el matcher y el asistente a ese directorio.
6. Conectar evidencias Plin al parser y a la bandeja pendiente.
7. Evaluar la UI y el matching en una POC controlada.
8. Diseñar Room y política de reimportación antes de una prueba de ruta que deba sobrevivir cierre de proceso.
9. Validar Yape cuando exista un dispositivo/cuenta disponible.
10. Diseñar escritura y round-trip del Excel en una entrega separada.

## Criterios de aceptación de la primera POC

- Drawer accesible sin sustituir las tres pestañas.
- Importar y Clientes aparecen en el drawer.
- El selector acepta el archivo real sin solicitar permiso general de almacenamiento.
- La app muestra un resumen de validación antes de importar.
- Solo entran filas con nombre y DNI no vacío; el resumen muestra cuántas se excluyeron por falta de DNI.
- Las filas inválidas se informan sin abortar silenciosamente ni inventar valores.
- Clientes muestra el directorio de forma fluida y permite buscar.
- Dirección ausente, DNI escaso, fórmulas y `No CUOTA` textual no provocan fallos.
- El asistente y Plin obtienen cero, una o varias sugerencias desde el lote activo.
- Ninguna coincidencia confirma automáticamente una persona, crédito o pago.
- El saldo importado y el abono previo no se modifican ni se descuentan otra vez.
- No se exporta ni sobrescribe el archivo original.

## Decisiones aprobadas — 2026-09-24

1. Usar `ModalNavigationDrawer`; ocultar barra inferior y FAB en Importar, Clientes y Configuración.
2. Mostrar `Clientes` en el menú y `Directorio importado` como título interno.
3. Importar `SERVICIO` como dato fuente opcional aunque todavía no tenga regla confirmada.
4. Tratar `ABONO` como base informativa importada, no como pago nuevo.
5. No fusionar filas automáticamente por nombre; mostrar duplicados para selección humana.
6. Ejecutar primero POC read-only sin persistencia de proceso; diseñar Room antes de una simulación de ruta confiable.
7. Mantener actualización/exportación del Excel fuera de esta entrega.
8. Considerar `DNI presente` como filtro temporal de existencia, sin exigir ocho dígitos ni usarlo aún como identidad estable.

La regla productiva de jornada se trabajará después: importar un Excel diario será obligatorio antes de iniciar y exportar el cierre será obligatorio al terminar. Esta POC no implementará todavía el bloqueo de inicio ni el cierre/exportación.
