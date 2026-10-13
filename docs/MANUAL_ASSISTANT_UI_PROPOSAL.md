# Propuesta de UI — asistente de pagos manuales

## Objetivo de esta entrega

Construir una POC visual instalable para validar en el teléfono el registro rápido durante la ruta y la revisión posterior. La POC usará únicamente un directorio, préstamos y pagos sintéticos. No importará datos reales, no persistirá pagos financieros, no modificará saldos y no sustituirá el esquema Room de fase 2.

El alcance posterior solicitado para importar el Excel real, mostrar un directorio y añadir un menú lateral se analiza por separado en [FIELD_FLOW_POC_PROPOSAL](FIELD_FLOW_POC_PROPOSAL.md). Ese documento sustituye la restricción de usar únicamente un directorio sintético cuando se apruebe la POC integrada; sigue manteniendo lectura read-only y sin aplicación a saldos.

## Acceso global

El `Scaffold` principal tendrá un `FloatingActionButton` con icono de chat y descripción accesible `Registrar pago`. Se mostrará en las tres pestañas raíz:

- Jornada.
- Cobranza.
- Ingresos.

El botón se ubicará sobre la barra inferior, en el extremo derecho. Mostrará un badge con la cantidad de borradores manuales pendientes cuando sea mayor que cero. Al pulsarlo abrirá el asistente desde la pestaña actual. Al volver, el usuario regresará a esa misma pestaña y conservará su estado.

El FAB no aparecerá dentro del asistente, la lista de revisión ni el detalle de un borrador porque esas pantallas ya tienen sus propias acciones.

## Pantalla 1 — captura rápida

Pantalla completa para evitar que el teclado reduzca demasiado un modal inferior. Tendrá:

- barra superior `Registro rápido`, acción Atrás y acceso `Revisar pendientes (N)`;
- indicación breve: `Escribe nombre o apodo y monto`;
- chip de medio seleccionado, inicialmente `Efectivo`;
- historial visual de entradas de la sesión;
- campo inferior de una línea y botón Enviar;
- ejemplos sintéticos: `pablito1 20` y `juan mototaxi 150.50`.

El teclado será normal porque la misma entrada combina texto y números. La acción IME Enviar ejecutará lo mismo que el botón.

Al enviar:

1. `ManualPaymentCommandParser` separa consulta e importe.
2. `CustomerNameMatcher` obtiene cero, uno o varios candidatos sintéticos.
3. Se crea un borrador local de POC con medio, PEN, céntimos, consulta original y resultado de matching.
4. Aparece una respuesta compacta, por ejemplo `Guardado: pablito1 · S/20.00 · 1 sugerencia`.
5. El campo se limpia y recupera el foco para registrar el siguiente pago.

No se abrirá automáticamente la selección de cliente o préstamo. Esto permite que el cobrador siga avanzando por la ruta.

Errores como monto ausente, cero, negativo o más de dos decimales se muestran debajo del campo sin borrar el texto. El borrador no se crea hasta que la sintaxis sea válida.

## Pantalla 2 — pendientes de revisión

Lista ordenada por captura más reciente con filtros `Todos`, `Sin coincidencia`, `Varias coincidencias` y `Listos para revisar`. Cada tarjeta mostrará:

- consulta escrita;
- monto en PEN;
- medio;
- hora local solo como contexto visual, nunca como identidad del pago;
- estado de matching;
- cliente sugerido únicamente cuando exista una sugerencia;
- acción `Revisar`.

La lista permitirá editar o eliminar un borrador de la POC mediante una acción explícita. Eliminar pedirá confirmación para evitar pérdidas por toque accidental.

## Pantalla 3 — revisión del borrador

Permitirá completar y corregir:

- consulta original de nombre o alias;
- cliente deudor;
- préstamo;
- medio;
- moneda PEN;
- monto;
- remitente, si después se habilita el flujo de terceros.

Cero coincidencias obliga a buscar o elegir un cliente. Varias coincidencias obliga a seleccionar una. Si el cliente sintético tiene varios préstamos, también se debe elegir uno. Ninguna sugerencia se selecciona como confirmación automática.

El botón `Confirmar pago` permanecerá deshabilitado mientras falte o sea ambiguo cualquier dato.

## Confirmación final

Al pulsar `Confirmar pago` se abrirá un diálogo con cliente, préstamo, medio, moneda y monto. La acción principal será `Confirmar y aplicar` y la secundaria `Volver a editar`.

En esta POC la acción se llamará `Validar flujo` y marcará el borrador sintético como revisado sin modificar un saldo. El texto visible indicará que es una prueba. La acción productiva `Confirmar y aplicar` se habilitará en fase 2 junto con Room, transacción idempotente, historial y correcciones.

## Navigation 3

El proyecto usa actualmente Navigation Compose 2.9.7. La migración propuesta será atómica; no se mantendrán Navigation 2 y Navigation 3 en paralelo.

Dependencias previstas:

- `androidx.navigation3:navigation3-runtime:1.2.0`;
- `androidx.navigation3:navigation3-ui:1.2.0`;
- plugin Kotlin Serialization alineado con Kotlin 2.2.10;
- runtime de Kotlin Serialization compatible y fijado en el catálogo;
- `lifecycle-viewmodel-navigation3` solo si el ViewModel se vincula a una entrada de navegación.

Navigation 3 1.2.0 es estable desde el 23 de septiembre de 2026. El proyecto usa `compileSdk 37`, por encima del mínimo 36 indicado para la migración.

Rutas tipadas y serializables previstas:

- `WorkdayRoute`;
- `CollectionRoute`;
- `IncomeRoute`;
- `ManualAssistantRoute`;
- `ManualReviewListRoute`;
- `ManualDraftReviewRoute(draftId)`.

Cada ruta implementará `NavKey`. `NavDisplay` sustituirá a `NavHost`; `rememberNavBackStack` y un estado de navegación con una pila por pestaña sustituirán a `NavController`. Abrir el asistente agregará su ruta a la pila de la pestaña activa. Atrás retirará la ruta y volverá a la pestaña de origen. El estado de cada pestaña se conservará al cambiar entre ellas.

Referencias oficiales:

- [Navigation 3](https://developer.android.com/guide/navigation/navigation-3)
- [Migración de Navigation 2 a Navigation 3](https://developer.android.com/guide/navigation/navigation-3/migration-guide)
- [Múltiples back stacks](https://developer.android.com/guide/navigation/navigation-3/recipes/multiple-backstacks)
- [Estado guardable y ViewModels](https://developer.android.com/guide/navigation/navigation-3/save-state)
- [Releases de Navigation 3](https://developer.android.com/jetpack/androidx/releases/navigation3)

## Arquitectura propuesta

La UI del asistente vivirá en `:feature:collection`, porque ese módulo es responsable de pagos manuales. `:app` seguirá ensamblando navegación y dependencias. No se crearán dependencias entre features.

Componentes previstos:

- `ManualAssistantUiState` expuesto por `StateFlow`;
- `ManualAssistantViewModel` con acciones explícitas para enviar, editar, eliminar, seleccionar y validar;
- `ManualDraftRepository` como contrato de domain para la implementación productiva posterior;
- estado local de POC con fixtures sintéticos para esta primera prueba visual;
- componentes reutilizables pequeños en `:core:designsystem` solo si aparecen en más de una pantalla.

La POC no guardará nombres reales en logs, Analytics ni Crashlytics. Tampoco usará DataStore para borradores. Room seguirá siendo la fuente de verdad productiva cuando se implemente fase 2.

## Directorio sintético

Los fixtures cubrirán:

- alias exacto `pablito1` con una sugerencia;
- alias `juan mototaxi` con una sugerencia;
- nombre sin coincidencia;
- alias ambiguo compartido por dos clientes;
- cliente con tres préstamos sintéticos;
- importes enteros y con decimales.

Los identificadores, nombres, préstamos y montos serán ficticios. El Excel real no se copiará ni se leerá desde esta UI.

## Orden de implementación propuesto

1. Migrar las tres pestañas existentes a Navigation 3 y verificar restauración/Atrás.
2. Añadir el FAB global y comprobar su presencia en las tres pestañas.
3. Crear estado, ViewModel y fixtures sintéticos del asistente.
4. Construir captura rápida y validación de comandos.
5. Construir lista de pendientes, filtros y detalle de revisión.
6. Añadir diálogo final de validación sin efecto financiero.
7. Probar en emulador y Z2474: teclado, rotación, Atrás, cambio de pestañas, segundo plano y recreación de actividad.

## Criterios de aceptación de la POC

- El FAB aparece en las tres pestañas raíz y abre el asistente.
- Atrás devuelve a la pestaña desde la cual se abrió.
- `pablito1 20` se muestra como `pablito1`, S/20.00 y una sugerencia sintética.
- `juan mototaxi 150.50` se muestra como `juan mototaxi`, S/150.50 y una sugerencia sintética.
- El envío guarda el borrador y deja el campo listo para la siguiente entrada.
- Cero o varias coincidencias no interrumpen la captura y quedan pendientes.
- La lista permite revisar y corregir después.
- No se puede validar un borrador ambiguo o incompleto.
- Ninguna acción modifica saldos, crea pagos reales o procesa datos del Excel.
- Navegación, estado de pestañas y UI sobreviven rotación y recreación de actividad; la persistencia tras cierre de proceso queda expresamente fuera de esta POC hasta Room.

## Decisiones aprobadas — 2026-09-24

1. Usar pantalla completa para el asistente, recomendada por el espacio que necesita el teclado y el historial.
2. Medio predeterminado `Efectivo`, modificable con un chip antes de capturar.
3. Mostrar badge de pendientes en el FAB.
4. Permitir eliminar borradores de POC con diálogo de confirmación.
5. Mantener esta primera prueba con datos sintéticos y sin persistencia tras cerrar el proceso.
