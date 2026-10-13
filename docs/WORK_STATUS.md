# Estado de trabajo

Fecha: 2026-09-24. **Fase 0 completada; fase 1 en curso por encargo explícito.** POC debug de Plin validada y primer flujo integrado de campo implementado. Yape, persistencia productiva, aplicación real de pagos y reglas de Jornada continúan pendientes. Las entradas siguientes conservan el historial; consultar la actualización final para el estado vigente.

## Git y punto de partida

Rama: `main`. Referencia inicial: `12925962c5dc152b4ea98a4e1081ab9f55693774` (`Add initial Android project`). Remoto: https://github.com/devlouis/cobrorutapp-android.git (`origin`, propietario `devlouis`).

Inspección inicial: checkout limpio; `git fetch origin` y ramas remotas mostraron solo `origin/main`; comparación `HEAD...origin/main`: 0/0. Se conserva historial y autenticación local de devlouis. No se cambió configuración global ni identidad. No había AGENTS ni documentos de continuidad. No se encontró Excel en el repositorio ni configuración Firebase.

Esta entrega se prepara como commit local `Prepare phase 0 Android foundation`, sobre la referencia inicial, con identidad existente y rutas explícitas revisadas. Consultar `git log -1 --oneline` para su identificador final. No se ejecutó push. Comando para un futuro encargo de publicación, después de `git fetch origin` y comparar: `git push origin main`.

## Implementación completada

- Nueve módulos: app; core/domain, data, designsystem; feature/workday, collection, income; integration/notifications, excel.
- Se conservaron applicationId/namespace `com.pe.cobrorutapp`, AGP 9.4.0, Wrapper 9.6.0, Kotlin/compiler Compose 2.2.10 y SDK 37. minSdk pasa de 29 a la propuesta 26; parque físico pendiente. Bytecode 11 uniforme.
- Application Hilt real, KSP2, DI de repositorio de preferencias, ViewModel con StateFlow y acciones. Hilt 2.60.1, KSP 2.3.12, Room 2.8.4, DataStore 1.2.1, Navigation 2.9.7, Lifecycle 2.9.4, Coroutines 1.10.2.
- Tema Material 3 existente movido a designsystem, componente de estado vacío, tres destinos sin operaciones ficticias. Tema sistema/claro/oscuro persistido en DataStore; barras de sistema siguen el tema para mantener contraste.
- Room runtime preparado; compilador fijado en catálogo para fase 2. **Sin esquema, DAO, tablas de relleno ni validación de persistencia de pagos.** Integraciones compilables sin servicios, permisos, parsers, XLSX ni OCR.
- AGENTS, README y documentos de producto, arquitectura, roadmap y estado. Exclusiones Git para compilados, credenciales, firma, Excel real y evidencias privadas. Wrapper conservado.

## Entorno detectado

macOS arm64. `./gradlew --version`: Gradle 9.6.0, launcher Java 22.0.2 y criterio daemon Java 25. Android Studio incluye JBR 25.0.3; se conserva la configuración del daemon de plantilla. El Kotlin 2.3.21 mostrado por Gradle es su Kotlin incorporado, distinto del compiler de app 2.2.10.

SDK instalados 31/33/34/35/36/37.0; Build Tools 30.0.3/33.0.1/34.0.0/35.0.0/36.0.0/37.0.0. Emulador autorizado `emulator-5554`, Pixel_7_Pro_API_33, Android 13/API 33, arm64. No se probó dispositivo físico ni API 26.

## Comandos y resultados reales

| Comprobación | Resultado |
| --- | --- |
| `git fetch origin`, `git branch -r`, `git rev-list --left-right --count HEAD...origin/main` iniciales | Acceso correcto; origin/main; 0/0 |
| `./gradlew :app:assembleDebug --console=plain` antes de editar | BUILD SUCCESSFUL, 6 s; plantilla válida |
| `./gradlew :app:assembleDebug lintDebug testDebugUnitTest :core:domain:test --console=plain` | BUILD SUCCESSFUL, 1 m 58 s; 358 tareas; Hilt/KSP compilados |
| Mismo comando tras corregir contraste de barras del sistema | BUILD SUCCESSFUL, 18 s; 30 ejecutadas y 328 actualizadas; configuration cache reutilizada |
| `./gradlew :app:connectedDebugAndroidTest :app:installDebug --console=plain` | BUILD SUCCESSFUL, 37 s; 1 test instrumentado heredado, 0 fallos/errores |
| `./gradlew :app:installDebug --console=plain` por separado, después de pruebas | Instalación correcta; repetida con APK final |
| `adb -s emulator-5554 shell am start -W -n com.pe.cobrorutapp/.MainActivity` | Arranque COLD, Status ok; final 3204 ms |
| Interacción con `adb shell input tap`, dump de UI y capturas inspeccionadas | Jornada → Cobranza → Ingresos; títulos y pestaña seleccionada correctos; sin acciones de cobranza |
| Tema sistema → claro → oscuro; `adb shell am force-stop com.pe.cobrorutapp` y nuevo arranque | Tema oscuro permanece tras reiniciar proceso; preferencia real DataStore y DI verificadas |
| Captura de APK final oscuro | Textos e iconos de estado/navegación legibles; corregido contraste detectado en revisión |
| `./gradlew :app:dependencies --configuration debugRuntimeClasspath --console=plain` | Grafo resuelto consultado; valores relevantes en ARCHITECTURE |
| `git diff --check` y `git check-ignore` de datos locales/compilados/Excel/firma/Firebase | Sin errores de whitespace; exclusiones esperadas confirmadas |

Lint de los ocho módulos Android: **0 errores**. App: 24 advertencias (versiones más recientes y recursos/etiqueta heredados); bibliotecas: 0 incidencias. No se ocultaron advertencias ni se cambiaron versiones solo por sugerencias de actualización. Reportes en `<módulo>/build/reports/lint-results-debug.html` y XML.

JVM: 1 test heredado `ExampleUnitTest`, 0 fallos/errores. Instrumentación: 1 test heredado de package name, 0 fallos/errores. Ambos son de plantilla; **no validan reglas de cobranza**. Las tareas de tests de domain y bibliotecas indicaron `NO-SOURCE`; no se presentan como pruebas aprobadas de negocio. No se añadieron tests triviales de pantallas vacías. La última corrección afecta contraste y se verificó con compilación/lint/JVM y arranque visual; la prueba instrumentada de package name se ejecutó antes de esa corrección.

Evidencia local no versionada: `app/build/reports/phase0/` conserva logs de validación y capturas de las tres pantallas/tema final. Los reportes se regeneran; no contienen el Excel real. No se interpreta la navegación manual como un test automatizado de UI.

## Incidencias resueltas y límites

El servidor ADB no pudo iniciar dentro del sandbox; funcionó con ejecución autorizada. Un primer script Gradle aplicado no resolvía LibraryExtension; se sustituyó por configuración de bibliotecas en el Gradle raíz y compiló. La instrumentación retira la app al finalizar: instalar en otra ejecución antes de comprobaciones visuales. JDK 25 produce una advertencia `sun.misc.Unsafe` del protobuf del tooling instrumentado; no hizo fallar la tarea.

El Excel `2SJL17ABRIL26.xlsx` fue facilitado por el usuario durante el cierre en una ubicación local externa. Solo se comprobó su existencia: **no se abrió ni se inspeccionó su contenido, ni se copió a Git**. Solicitar de nuevo su ubicación si no está disponible en otra sesión; no guardar rutas personales en documentación compartida.

Sin Firebase real, Crashlytics/Analytics no están instalados ni inicializados. Notificaciones Yape/Interbank, OCR, permisos, Room de cobranza, reimportación, transacciones, duplicados y reglas monetarias siguen pendientes de sus fases. No se verificó pantalla bloqueada, recuperación de cobros ni comportamiento de bancos.

## Siguiente tarea y acciones manuales

No hay bloqueos para cerrar fase 0 ni comandos manuales indispensables restantes. No iniciar fase 1 automáticamente ni ejecutar push.

Tras recibir encargo de fase 1: leer AGENTS/estado/roadmap e inspeccionar Git; validar POC acotada de notificaciones en un Android real con cuentas de prueba y preparar round-trip del Excel facilitado fuera de Git. El usuario deberá disponer del dispositivo, autorizar accesos necesarios solo cuando exista la POC explicada y aclarar C/N/A/colores. No elegir XLSX/OCR ni prometer captura antes de obtener evidencia. Crashlytics requerirá configuración real compatible con `com.pe.cobrorutapp` cuando se encargue.

## Actualización posterior — skill model-routing (2026-09-12)

Por encargo explícito del usuario se creó `.codex/skills/model-routing/SKILL.md` con el contenido íntegro del archivo adjunto. `AGENTS.md` ya tenía cambios locales del usuario y la referencia al skill; se conservaron sin modificarlos.

Validación: `quick_validate.py .codex/skills/model-routing` devolvió `Skill is valid!`; `cmp` confirmó igualdad exacta con el adjunto. PyYAML 6.0.2 se instaló únicamente en un directorio temporal para ejecutar el validador; no se añadieron dependencias al proyecto Android. Esta comprobación valida formato y estructura, no acredita cambio automático de modelo ni descubrimiento por todos los clientes.

AGENTS.md y el skill quedaron en el commit local `a3fc786` (`docs: add Codex project instructions and model routing`), creado por encargo posterior del usuario. WORK_STATUS.md sigue con cambios sin commit; no se hizo push. Fase 0 sigue completada y fase 1 pendiente de encargo.

`git diff --check` general señaló cinco finales de línea con dos espacios en el cambio previo de AGENTS.md (saltos Markdown); no se alteraron cambios del usuario. La comprobación limitada a WORK_STATUS.md pasó.

## Continuidad — definiciones de producto

Finalizadas las pruebas conversacionales de clasificación de tareas. El usuario pidió retomar las definiciones pendientes de la app. Se revisaron instrucciones, contexto, roadmap y Git: main está dos commits por delante de origin/main; WORK_STATUS.md tiene cambios locales. Se retoma la conversación de requisitos sin iniciar implementación de fase 1.

Primer bloque a aclarar: significado operativo de C/N/A y colores del Excel. Después: identidad de clientes/deudas, aplicación de abonos, reimportación/cierre y correcciones/respaldo. Aún no hay nuevas reglas acordadas ni resultados de inspección del Excel.

## Definiciones recibidas — 2026-09-15

Registrados en PRODUCT_CONTEXT los apuntes de entrevista: C/N/A, amarillo/azul/plomo/verde, hasta tres deudas por cliente, interés general 20 %, plazos ≤400 a 20 días y ≥500 a 24 días y aviso previo de tres días. Se preserva el carácter reportado de las reglas; siguen abiertas diferencia C/N, deudas activas frente a historial, base/periodicidad del interés, intervalo 400–500, calendario y objeto del vencimiento.

Solo documentación; no se inspeccionó el Excel ni se modificó código o permisos. No se avanzó de fase, no se ejecutaron pruebas Android ni commit/push. Validación documental: git diff --check sobre PRODUCT_CONTEXT y WORK_STATUS. Siguiente paso: aclarar los puntos anteriores con el usuario antes de convertirlos en reglas de cálculo.

## Aclaraciones confirmadas — 2026-09-15

Actualizado PRODUCT_CONTEXT: C y N son equivalentes (cuota del día no pagada); hasta tres préstamos activos simultáneos por cliente; interés único del 20 % sobre capital incluido en cuotas diarias. Ejemplo confirmado: S/400 → S/480 en 20 cuotas de S/24. Aviso al cobrador tres días antes del vencimiento final de cada préstamo del cliente, no de cada cuota.

Calendario preliminar: lunes a sábado, incluidos feriados excepto nacionales. El usuario pospuso expresamente su definición; no se cerró cómputo de plazos, primera cuota, excepciones ni tipo de días del aviso. El tramo mayor que 400 y menor que 500 continúa sin resolver. Pendiente también distribución de pagos entre préstamos, parciales/adelantos y redondeo.

Solo documentación, conservando cambios locales previos; sin código, pruebas Android, commit ni push. git diff --check de los dos documentos pasó. Siguiente pregunta concreta: qué plazo corresponde a un préstamo de S/450 o si ese monto no se ofrece.

## Plazos cerrados — 2026-09-15

Confirmación explícita del usuario: capital ≤ S/400 → 20 días; capital > S/400 → 24 días. Sustituye el umbral preliminar de S/500 citado en entradas anteriores; ya no hay intervalo pendiente. PRODUCT_CONTEXT actualizado como referencia vigente. El calendario exacto continúa pospuesto.

Solo documentación; git diff --check pasó, sin código, pruebas Android, commit ni push. Siguiente definición: cómo asigna el cobrador un pago cuando el cliente tiene varios préstamos activos.


## Fase 1 — checklist y POC de notificaciones (2026-09-15)

El usuario encargó mantener un checklist mientras consulta al cobrador y avanzar con lectura de notificaciones Yape/Plin de Interbank. BUSINESS_RULES_CHECKLIST reúne confirmaciones y pendientes, sin resolver por inferencia la asignación de pagos. NOTIFICATION_POC describe alcance, limitaciones y protocolo físico. No se inició el round-trip Excel ni fases posteriores.

Implementado únicamente en debug: NotificationListenerService Hilt independiente de UI, allowlist exacta de dos paquetes, descarte temprano de fuentes ajenas/resúmenes/permanentes, filtro textual provisional y fingerprint técnico. Contrato puro en domain, almacenamiento diagnóstico atómico en data y pantalla/DI en app. Sesión explícita de diez minutos; máximo veinte muestras locales fuera de backup; detener, desconectar o recrear proceso impide reanudar automáticamente. Borrado explícito, errores visibles y pantalla protegida con FLAG_SECURE. Sin parser monetario, pagos, asociación de clientes, red ni avisos propios. Release carece de pantalla y servicio de captura, comprobado en los manifiestos fusionados.

Validaciones realizadas:

- `./gradlew :app:assembleDebug :app:assembleRelease lintDebug testDebugUnitTest :core:domain:test --console=plain`: BUILD SUCCESSFUL. Tras ajustes del filtro se repitieron assembleDebug, lintDebug y testDebugUnitTest: correctos. Nueve tests JVM nuevos de fuentes, textos sintéticos, límites y fingerprint, sin fallos; un test JVM heredado adicional. Domain sigue NO-SOURCE.
- Lint: cero errores; se conservan las 24 advertencias existentes de app y cero incidencias en bibliotecas.
- `:app:connectedDebugAndroidTest`: un test heredado correcto por dispositivo (emulador API 33 y Z2474 Android 15). No acredita recepción bancaria.
- Instalación debug y arranque en emulador correctos; navegación a Ingresos y diagnóstico inspeccionada mediante UIAutomator. Acceso pendiente, servicio desconectado, cero muestras e inicio deshabilitado: correcto. No se concedió acceso de notificaciones.
- A petición explícita posterior, APK debug instalado en Z2474 y MainActivity arrancada con Status ok; abierta la entrada de diagnóstico desde Ingresos. Se indicó al usuario cómo conceder acceso e iniciar la sesión. No se inspeccionaron ni operaron apps bancarias.

Incidencia de tests: las primeras tareas de core:data terminaron BUILD SUCCESSFUL pero reportaron cero tests. Ejecución directa del runner reveló ClassNotFoundException de AndroidJUnitRunner. Se añadió dependencia de instrumentación explícita 1.5.2, ya presente en el entorno del proyecto. Esas primeras ejecuciones NO cuentan como validación de almacenamiento. Con la dependencia corregida, `ANDROID_SERIAL=emulator-5554 ./gradlew :core:data:connectedDebugAndroidTest :core:data:lintDebug --console=plain` terminó correctamente: XML confirma siete tests ejecutados, cero fallos/errores/omitidos (sesión, caducidad, desconexión, replay, persistencia, límite y corrupción). `git diff --check` final pasó. No se repitieron estos tests en Z2474 para no interrumpir la prueba manual del usuario.

Pendiente: ejecutar el protocolo real en Z2474 con Yape e Interbank instalados, obtener resultados anonimizados de ingreso con pantalla abierta/segundo plano/bloqueada, revocación y formatos. Los filtros sintéticos pueden omitir mensajes reales. El usuario concede acceso y realiza cualquier operación bancaria. La prueba no valida dinero recibido ni aplica abonos. Excel, OCR y reglas de cobranza siguen fuera de este encargo.

Cambios locales conservados; sin commit ni push. Siguiente tarea: acompañar la prueba en Z2474 y ajustar solo a evidencia real anonimizada, sin ampliar indiscriminadamente la captura.


## Diagnóstico de primera prueba Plin — Z2474

El usuario reportó envío de un Plin al Z2474 y contador 0/20. Lecturas posteriores: Interbank 23.4 instalado; Yape no instalado en ese perfil; acceso especial concedido; servicio enlazado por Android; UI con sesión activa y cero muestras. Comprobado con settings, dumpsys activity services limitado a CobroRutApp y extracción de indicadores de UI. Esto describe el estado al inspeccionar, no demuestra que la sesión estuviera activa en el instante del aviso.

No se extrajo el contenido de notificaciones bancarias ni se cambió el filtro sin evidencia. Se solicitó confirmar si Interbank publicó una notificación y aportar título/frase anonimizados. Pendiente distinguir ausencia de aviso, formato no reconocido, contenido restringido o aviso anterior a la sesión. Sin cambios de código ni nueva validación bancaria exitosa; sin commit/push.


## Corrección de formato Plin

El usuario confirmó finalmente la expresión «PERSONA te ha plineado S/MONTO» (corrigió su transcripción inicial). El filtro inicial solo reconocía «te plineó», «recibiste» y «has recibido» para Interbank; el texto confirmado quedaba fuera. Se ajusta únicamente la variante «te ha plineado» en Interbank, conservando paquetes permitidos, exclusiones y confirmación humana. Nombres y montos reales no se incorporan a archivos del proyecto. El aviso anterior no se recupera automáticamente; tras actualizar será necesaria una sesión nueva y un aviso nuevo para verificar captura real.


Validación de la corrección: `./gradlew :app:assembleDebug :integration:notifications:testDebugUnitTest :integration:notifications:lintDebug --console=plain` terminó BUILD SUCCESSFUL. XML: diez tests, cero fallos/errores; incluye frase sin título Plin separado, porque «plineado» aporta contexto, y rechazo de la transcripción incorrecta «ah». APK actualizado en Z2474 mediante `adb install -r`: Success. `git diff --check` pasó. Pendiente nueva sesión y aviso real posterior a la actualización; no se declara recepción bancaria validada. Sin commit/push.


## Primera captura real Plin confirmada por el usuario

Tras instalar la corrección, el usuario informó que recibió un nuevo Plin en Z2474 y el contador cambió de 0/20 a 1/20. Se registra como prueba manual reportada por el usuario de captura de un aviso Interbank, no como verificación independiente de contenido o recepción bancaria ni como abono aplicado. No se copiaron datos personales ni montos a Git.

Pendientes: repetir con CobroRutApp en segundo plano y pantalla bloqueada dentro de una sesión activa, verificar conservación de muestras y sesión detenida al reiniciar, revocación del acceso y Yape. No se conoce el estado de pantalla de esta primera captura; no atribuirle cobertura de esos escenarios. Fase 1 continúa abierta. Solo actualización documental; git diff --check pasó; sin commit/push.


## Plin con pantalla bloqueada — confirmado por el usuario

Siguiendo el protocolo de bloquear el Z2474 con sesión activa y recibir otro aviso, el usuario confirmó cambio a 2/20. Prueba manual reportada satisfactoria para ese dispositivo y versión de Interbank; no generalizar a otros Android ni a proceso detenido. Siguiente prueba: pantalla encendida, CobroRutApp en segundo plano mediante Inicio, sesión activa y aviso nuevo (esperado 3/20 si permanece en 2/20 antes de probar). Después: persistencia al recrear proceso, parada/caducidad, revocación y Yape. No borrar muestras antes de verificar persistencia. Solo documentación, sin nuevos tests de código, commit ni push; git diff --check pasó.


## Plin en segundo plano — confirmado por el usuario

El usuario confirmó 3/20 tras la prueba con CobroRutApp en segundo plano y pantalla encendida. Resultado manual reportado satisfactorio en Z2474/Interbank 23.4. Junto con 2/20 en pantalla bloqueada, acredita esos escenarios concretos; no confirma Yape ni captura tras detener el proceso. Siguiente comprobación manual: forzar detención desde ajustes, reabrir y verificar tres muestras conservadas y captura detenida, sin borrar almacenamiento ni recibir otro pago. Pendientes también caducidad, parada y revocación. Solo documentación; git diff --check pasó; sin commit/push.


## Persistencia tras forzar detención — confirmada por el usuario

El usuario confirmó ambos resultados del protocolo en Z2474: tras forzar detención desde Ajustes y reabrir, se mantienen 3/20 muestras y la captura está detenida. Es validación manual reportada de persistencia de diagnósticos y ausencia de reanudación automática, no de captura mientras el proceso está detenido. Siguiente prueba: iniciar una sesión, dejar transcurrir diez minutos sin reiniciarla y comprobar captura detenida con 3/20 conservadas, sin nuevos avisos. Pendientes parada con aviso posterior, revocación, borrado final y Yape. Solo documentación; git diff --check pasó; sin commit/push.


## Caducidad automática — confirmada por el usuario

El usuario confirmó el protocolo de iniciar sesión, dejar pasar diez minutos en segundo plano y volver: captura detenida y tres muestras conservadas sin nuevos avisos. Resultado manual reportado en Z2474. Siguiente comprobación: iniciar sesión, revocar el acceso especial de CobroRutApp desde Ajustes y volver; esperado servicio desconectado, captura detenida, inicio deshabilitado y muestras conservadas. Pendientes revocación, parada con aviso posterior, borrado final y Yape. Solo documentación; git diff --check pasó; sin commit/push.


## Revocación del acceso — confirmada por el usuario

El usuario confirmó el protocolo de revocar el acceso especial durante una sesión activa y volver: servicio desconectado, captura detenida, inicio deshabilitado y 3/20 muestras conservadas. Resultado manual reportado en Z2474. Siguiente comprobación: volver a conceder acceso y verificar servicio conectado, captura aún detenida y muestras conservadas; no iniciar automáticamente una sesión. Pendientes reconexión, parada con aviso posterior, borrado final y Yape. Solo documentación; git diff --check pasó; sin commit/push.


## Reconexión sin reanudación — confirmada por el usuario

El usuario confirmó que, al volver a conceder acceso y regresar sin iniciar sesión, el servicio está conectado, la captura permanece detenida, se conservan 3/20 muestras y el botón de inicio está habilitado. Resultado manual reportado en Z2474. Siguiente prueba: iniciar sesión, detenerla manualmente y comprobar ante un nuevo aviso de Plin que el contador siga en 3/20; puede aprovecharse el próximo ingreso previsto. Después borrar muestras y verificar persistencia del borrado al reabrir. Yape y otros formatos siguen pendientes; fase 1 no cerrada. Solo documentación; git diff --check pasó; sin commit/push.


## Parada manual con aviso posterior — confirmada por el usuario

El usuario confirmó el protocolo de iniciar sesión, pulsar Detener captura y recibir otro aviso Plin: contador conservado en 3/20. Resultado manual reportado en Z2474; no se almacenó una nueva muestra según ese contador. Siguiente y última comprobación de este bloque: Borrar muestras y detener, verificar 0/20 y captura detenida, forzar detención y reabrir para confirmar que no reaparezcan las muestras. Yape, otros formatos y demás validaciones de fase 1 siguen pendientes. Solo documentación; git diff --check pasó; sin commit/push.

## Borrado persistente — confirmado por el usuario

El usuario confirmó el protocolo final del bloque Plin: después de pulsar Borrar muestras y detener, el contador quedó en 0/20 y la captura detenida; tras forzar detención y reabrir, ambos estados se conservaron. Con esto queda validado manualmente en Z2474/Interbank 23.4 el borrado persistente de diagnósticos.

El bloque Plin cubre captura con la app visible, pantalla bloqueada y segundo plano; conservación de muestras sin reanudar captura tras forzar detención; caducidad a los diez minutos; revocación y reconexión sin reanudación; parada manual; y borrado persistente. Los resultados fueron reportados por el usuario y no convierten una notificación en confirmación bancaria ni en un abono. Yape, formatos alternativos de Interbank y el round-trip Excel siguen pendientes, por lo que fase 1 continúa abierta. Solo documentación; sin cambios de código, commit ni push.

## Punto guardado — POC Plin y reglas de negocio

El usuario pidió guardar y detallar el trabajo hasta este punto. La revisión final incluye la POC debug completa, la corrección de `te ha plineado`, los resultados manuales de Z2474/Interbank 23.4, el checklist de reglas confirmadas y pendientes, y la documentación de arquitectura y continuidad. No se encontraron secretos ni datos personales reales en las rutas preparadas.

Validación final antes del commit local:

- `./gradlew :app:assembleDebug :app:assembleRelease lintDebug testDebugUnitTest :core:domain:test --console=plain`: BUILD SUCCESSFUL, 649 tareas. Diez tests JVM del filtro y el test heredado de app sin fallos; domain continúa `NO-SOURCE`.
- `ANDROID_SERIAL=emulator-5554 ./gradlew :core:data:connectedDebugAndroidTest :app:connectedDebugAndroidTest --console=plain`: BUILD SUCCESSFUL. Siete tests del repositorio diagnóstico y un test heredado de app, cero fallos, errores u omitidos.
- Lint: cero errores. App conserva 25 advertencias de actualización, recursos y etiqueta; las bibliotecas no reportan incidencias. No se ocultaron advertencias.
- Manifiestos fusionados: Activity y NotificationListenerService presentes en debug y ausentes en release.
- `git diff --check`: sin errores.

El commit será local y no se ejecutará push. Fase 1 continúa con Yape, formatos alternativos de Interbank y el round-trip Excel pendientes.

## Secuencia acordada para notificaciones

El usuario indicó continuar avanzando con pruebas y comprobaciones de Plin a medida que existan nuevos escenarios o formatos. La matriz ejecutada en Z2474 queda como línea base comprobada, sin considerar cerrada toda compatibilidad futura de Interbank. Las pruebas equivalentes de Yape se realizarán después y permanecen pendientes; los resultados de Plin no se trasladan por suposición a Yape. Solo actualización documental, sin cambios de código ni pruebas nuevas; no ejecutar push.

## Discovery de asociación de pagos — 2026-09-24

El usuario propuso tres flujos para la siguiente POC: sugerir cliente por coincidencia del nombre Plin manteniendo confirmación obligatoria; conservar sin coincidencia en una lista pendiente; y registrar efectivo/manual mediante un asistente tipo chat con nombre o apodo y monto.

Se inspeccionó `2SJL17ABRIL26.xlsx` localmente y en modo lectura con el runtime de spreadsheets. `Plan1` tiene encabezados relevantes en `C:L`; se analizaron 4,062 filas dentro de ese rango sin imprimir nombres, DNI, direcciones ni montos. Resultado anonimizado: 3,909 filas con nombre, 2,047 nombres normalizados distintos, 696 grupos repetidos que abarcan 2,558 filas, máximo 22 filas para un mismo nombre normalizado y 53 filas con DNI. Hay fórmulas numerosas en saldo y abono. El archivo no se modificó ni se copió al repositorio.

Conclusión: una coincidencia puede sugerir, nunca identificar automáticamente cliente o deuda. Se documentó una bandeja única de pendientes y un asistente determinista que crea borradores revisables en [PAYMENT_MATCHING_POC](PAYMENT_MATCHING_POC.md). No se implementó matcher, parser monetario, pantalla, Room ni escritura Excel en este paso de discovery. Siguiente tarea técnica propuesta: parser Plin y matcher puro con fixtures sintéticos; después UI debug e importación read-only. Sin commit ni push.

## Parser Plin y matcher de nombres — 2026-09-24

El usuario aprobó que un pago manual se aplique inmediatamente después de una confirmación explícita con cliente, préstamo, medio, moneda y monto. Si falta un dato o existe ambigüedad, la confirmación permanece deshabilitada. Se actualizó PRODUCT_CONTEXT, BUSINESS_RULES_CHECKLIST y PAYMENT_MATCHING_POC. La aplicación real al saldo continúa reservada para la transacción idempotente de fase 2; esta entrega no crea pagos ni modifica saldos.

Implementado un modelo de dinero Kotlin/JVM con `Long` en unidades menores y moneda `PEN`. El matcher puro normaliza mayúsculas, tildes, puntuación y espacios solo para búsqueda; conserva el nombre mostrado y devuelve todos los candidatos del primer nivel con resultados: nombre exacto normalizado, alias confirmado exacto o los mismos términos completos en otro orden. No usa coincidencia aproximada, consultas parciales, monto, hora ni fila de Excel, y nunca selecciona automáticamente un cliente.

Implementado en el source set debug de notificaciones un parser acotado al formato comprobado `PERSONA te ha plineado S/MONTO`, con dos decimales, cálculo entero de céntimos, moneda PEN y control de overflow. Revisa campos individuales de la notificación; acepta repeticiones idénticas y rechaza resultados conflictivos. Permanece desacoplado del listener, la persistencia y la UI hasta construir la bandeja debug. Formatos no observados quedan sin interpretar en vez de inferirse.

Validación realizada:

- `./gradlew :core:domain:test :integration:notifications:testDebugUnitTest --console=plain`: BUILD SUCCESSFUL. Matcher: 9 tests; parser: 6; filtro existente: 10. Total 25, cero fallos y errores.
- `./gradlew :app:assembleDebug :app:assembleRelease lintDebug --console=plain`: BUILD SUCCESSFUL, 621 tareas. El reporte XML de lint contiene cero incidencias.
- `git diff --check`: sin errores.

No se ejecutaron pruebas conectadas porque parser y matcher son piezas puras sin integración al listener o UI. Siguiente tarea: conectar parser y matcher a un directorio sintético y mostrar una bandeja debug con cero, una o varias sugerencias. La importación read-only del directorio real, claves estables, UI manual, Room, aplicación de pagos, Yape y round-trip Excel continúan pendientes. Cambios locales sin commit ni push.

## Captura manual rápida — 2026-09-24

El usuario aclaró que el cobrador necesita registrar durante la ruta entradas breves como `pablito1 20` y `juan mototaxi 150.50`, sin escribir obligatoriamente un signo de moneda, para revisar los pagos al finalizar. Esto no contradice la aplicación inmediata del pago manual confirmado: enviar el comando solo guarda un borrador; la aplicación ocurre después de revisar y confirmar cliente, préstamo, medio, moneda y monto.

Implementado en domain un parser determinista de comandos manuales. Interpreta el último elemento numérico como monto y todo el texto anterior como consulta de nombre o alias; admite soles enteros o uno/dos decimales, `S/` opcional, moneda PEN y conversión segura a céntimos `Long`. Rechaza consulta o monto ausentes, cero, negativos, coma decimal, más de dos decimales, símbolos no admitidos y overflow. El medio de pago se elegirá mediante el contexto de la pantalla para evitar escribirlo en cada comando.

Validación: `./gradlew :core:domain:test :app:assembleDebug lintDebug --console=plain` terminó BUILD SUCCESSFUL. Domain ejecutó 15 tests —9 del matcher y 6 del parser manual— sin fallos ni errores; lint contiene cero incidencias. `git diff --check` y la revisión de espacios finales pasaron. No se implementó todavía la pantalla, persistencia de borradores ni aplicación a saldo. Siguiente tarea: integrar parser, matcher y directorio sintético en la UI debug con una cola de revisión posterior. Sin commit ni push.

## Propuesta de UI del asistente — 2026-09-24

El usuario pidió detallar, sin implementar todavía, una vista de asistente accesible mediante un FAB con icono de chat visible en Jornada, Cobranza e Ingresos, y usar Jetpack Navigation 3. Se inspeccionó la app: actualmente `CobroRutApp.kt` usa Navigation Compose 2.9.7, un `NavController`, `NavHost` y tres destinos raíz sin flujos hijos; las pantallas de feature continúan vacías.

Se preparó [MANUAL_ASSISTANT_UI_PROPOSAL](MANUAL_ASSISTANT_UI_PROPOSAL.md) para revisión. Recomienda migración atómica a Navigation 3 1.2.0 estable, rutas `NavKey` tipadas, back stack por pestaña y `NavDisplay`; FAB global; captura rápida en pantalla completa; cola de borradores; revisión detallada; y diálogo final sin efecto financiero. La UI y ViewModel pertenecerán a `:feature:collection`; `:app` ensamblará navegación. La POC usará solo fixtures sintéticos y estado local, sin Room, Excel real, pagos ni saldos.

Fuentes oficiales consultadas: documentación principal, guía de migración, receta de múltiples back stacks, guardado de estado y release notes de Navigation 3. La versión 1.2.0 figura estable desde el 23 de septiembre de 2026 y exige `compileSdk` 36 o superior; el proyecto usa 37. No se modificó código ni catálogo de dependencias en este paso. Pendiente aprobación de interacción a pantalla completa, efectivo predeterminado, badge, eliminación con confirmación y alcance efímero de la POC. Sin pruebas Android nuevas, commit ni push; `git diff --check` y la revisión de espacios finales pasaron.

## Análisis del flujo integrado de campo — 2026-09-24

El usuario amplió la POC: importar el Excel para obtener nombre, dirección, DNI, fecha, venta, total, valor de cuota, saldo, abono y número de cuota; mostrar Clientes desde un menú lateral; y usar el directorio con el asistente y detector de notificaciones para simular el flujo de campo. Solicitó análisis y observaciones, sin implementación todavía.

Se reinspeccionó `Plan1!C1:M4062` read-only y sin imprimir datos personales. `No CUOTA` existe en M, con 4,005 valores no vacíos, 1,175 fórmulas y 133 valores textuales. `DIRECCION` existe en C, pero las 4,061 filas de datos están vacías en este archivo. `SALDO` tiene 1,189 fórmulas y `ABONO` 1,292. El archivo también contiene `SERVICIO`, con 213 valores no vacíos, aunque no fue incluido en la lista solicitada. Los conteos anteriores de nombres repetidos y DNI escaso se mantienen.

Se documentó la propuesta completa en [FIELD_FLOW_POC_PROPOSAL](FIELD_FLOW_POC_PROPOSAL.md): `ModalNavigationDrawer` para Importar, Clientes y Configuración; tres pestañas intactas; importación mediante selector Android y validación previa; directorio lazy; filas importadas separadas de identidad de cliente/crédito; integración con matching manual y digital; y dos niveles de prueba. La POC read-only puede ser temporal, pero una simulación de ruta que deba sobrevivir cierre de proceso requiere una base Room mínima y entra en la fundación de fase 2.

Observaciones principales: no agrupar automáticamente por nombre; leer DNI como texto formateado; conservar `SERVICIO` sin atribuirle semántica; tratar `ABONO` como dato importado que no genera ni reaplica un pago; conservar `No CUOTA` también como valor original; no evaluar fórmulas; y dejar actualización/exportación para el round-trip posterior. Sin cambios de código, dependencias, Excel ni datos reales versionados; sin pruebas Android nuevas, commit ni push. `git diff --check` y la revisión de espacios finales pasaron. Pendiente aprobación de las siete decisiones de alcance.

## Filtro temporal por DNI — 2026-09-24

El usuario definió que la POC solo importará usuarios con DNI. Se adopta como regla de elegibilidad por presencia: nombre no vacío y celda DNI no vacía. No se interpreta todavía como validación oficial de ocho dígitos ni como clave estable de cliente.

La inspección read-only de `Plan1!D2:E4062` confirma 3,909 filas con nombre y solo 53 con nombre más DNI; 3,856 filas con nombre quedarían excluidas. Las 53 celdas son valores únicos en este archivo: 52 numéricas y una textual. Sus representaciones crudas tienen de uno a tres caracteres, ninguna tiene ocho dígitos y las celdas no reportan un formato numérico que complete ceros. Esto sugiere que pueden ser códigos u otro dato incompleto; la UI debe indicar `DNI presente — formato no validado` y no afirmar autenticidad.

Se actualizó la propuesta integrada, PRODUCT_CONTEXT y BUSINESS_RULES_CHECKLIST. La vista previa de importación mostrará incluidos y excluidos; solo las 53 filas elegibles alimentarán Clientes y matching. No se modificó código ni Excel, no se imprimieron valores personales y no hubo pruebas Android, commit ni push. `git diff --check` y la revisión de espacios finales pasaron.

## Aprobación de la POC y regla futura de jornada — 2026-09-24

El usuario aprobó la propuesta integrada: Navigation 3, drawer, FAB, importación read-only, directorio filtrado por DNI presente, asistente y asociación de notificaciones. También indicó que las reglas detalladas de Jornada se trabajarán después.

Quedaron confirmadas dos reglas de alto nivel para esa futura feature: cada día se debe importar el Excel antes de iniciar la jornada y, si no existe una importación aceptada, el inicio permanece bloqueado; al finalizar se debe exportar el archivo de cierre. Continúan pendientes validación de fecha/ruta, reemplazo o reimportación, jornadas activas, pendientes que bloquean cierre, fallo de exportación, nombre/destino, reapertura y reexportación.

Se actualizaron FIELD_FLOW_POC_PROPOSAL, MANUAL_ASSISTANT_UI_PROPOSAL, PRODUCT_CONTEXT, BUSINESS_RULES_CHECKLIST y ROADMAP. No se inició la feature Jornada ni se implementaron bloqueos o exportación. Sin código, pruebas Android, commit ni push en este paso; `git diff --check` y la revisión de espacios finales pasaron.

## Implementación del flujo integrado de campo — 2026-09-24

Por encargo explícito se implementó la POC aprobada. La app usa Navigation 3 1.2.0 con rutas tipadas, tres back stacks independientes para Jornada/Cobranza/Ingresos, drawer para Importar archivo/Clientes/Configuración y un FAB global del asistente con contador de borradores pendientes. Los destinos secundarios ocultan barra inferior y FAB; volver conserva la pestaña de origen.

La importación abre el selector Android, lee `Plan1` en segundo plano y presenta una vista previa antes de aceptar. El lector OOXML usa ZIP+SAX, no ejecuta macros, enlaces, fórmulas ni contenido activo, y aplica límites defensivos. Solo incluye filas con nombre y DNI presentes; conserva el valor crudo, convierte dinero válido a céntimos `Long` PEN y advierte DNI no validado, montos inválidos o fórmulas sin valor cacheado. La importación aceptada permanece únicamente en memoria para esta POC: no crea Room, jornada, pagos, saldo nuevo ni exportación.

Clientes muestra una fila importada por operación, sin fusionar nombres o DNI. Oculta parcialmente el DNI, permite búsqueda por nombre/DNI/dirección, señala nombres repetidos y muestra fecha, saldo y número de cuota para distinguir filas. El detalle conserva dirección, fecha, venta, servicio opcional, total, cuota, saldo, abono importado y `No.CUOTA`.

El asistente acepta comandos como `pablito1 20`, fija Efectivo/PEN, guarda un borrador y mantiene el foco para captura rápida. La cola exige selección humana de cliente/fila y deuda aun con una sola sugerencia; después muestra una confirmación explícita. La acción final se denomina `Validar simulación` y no altera saldos. Al aceptar un Excel, el matcher usa exclusivamente sus filas elegibles; sin importación se conservan fixtures sintéticos para probar la UI.

Validaciones reales:

- `./gradlew :app:assembleDebug lintDebug testDebugUnitTest :core:domain:test`: BUILD SUCCESSFUL, 379 tareas; lint sin errores.
- Domain: 15 pruebas del matcher y parser manual; notificaciones: 16 pruebas incluyendo parser Plin y filtros; Excel: 5 pruebas sintéticas; collection: 6 pruebas del ViewModel manual. Cero fallos.
- Smoke temporal contra `2SJL17ABRIL26.xlsx`: 53 filas incluidas y 3,856 excluidas sin DNI. El test temporal y la ruta personal se eliminaron; el archivo y sus datos no se versionaron.
- `git diff --check`: sin errores.
- APK debug instalada con `adb install -r` en Z2474; arranque COLD correcto. Se inspeccionaron visualmente Jornada, drawer, Importar archivo y Asistente. El comando manual `pablito1 20` creó un pendiente y mostró una coincidencia sugerida sin selección automática.

El Excel de referencia se copió temporalmente a Descargas del Z2474 para continuar la prueba manual mediante el selector del sistema. La selección automatizada con toques ADB no produjo un URI utilizable, por lo que no se declara verificada la vista previa dentro del dispositivo; el mismo archivo sí pasó el lector real en el smoke test JVM. Siguiente tarea: seleccionar manualmente el XLSX en Z2474, aceptar la vista previa, revisar el directorio y probar cero/una/varias coincidencias con datos permitidos. Después corresponde diseñar persistencia e idempotencia antes de aplicar pagos o iniciar las reglas de Jornada. Sin commit ni push.

## Corrección de importación XLSX en Android — 2026-09-25

El usuario comprobó en Z2474 que la importación no mostraba progreso visible y terminaba con `No se pudo leer el archivo. Selecciona un XLSX válido` al elegir el archivo de referencia. Se reprodujo el fallo con una prueba instrumentada antes de modificar el lector. La causa no era el formato del archivo: Android rechazaba dos opciones de seguridad configuradas sobre `SAXParserFactory` y la excepción de compatibilidad quedaba clasificada como XLSX inválido.

Se adaptó el lector OOXML al parser SAX de Android. Las entidades externas generales y parametrizadas se desactivan en el `XMLReader`, se conserva el `EntityResolver` vacío y se rechaza `DOCTYPE` antes del parseo para mantener el límite de seguridad sin depender de opciones que Android no reconoce. Los errores ahora distinguen archivo inaccesible, XLSX inválido y fallo interno de lectura, con mensajes específicos en la UI.

La pantalla de importación muestra una tarjeta de progreso con indicador circular, barra indeterminada y el texto `Leyendo y validando el archivo…`. El ViewModel conserva ese estado al menos 700 ms para que una lectura rápida también produzca respuesta visual y cancela una importación anterior si se elige otro archivo.

Validaciones realizadas:

- La prueba instrumentada de regresión falló en Z2474 antes de la corrección y pasó después.
- `ANDROID_SERIAL=LZA56PPEYDC009239 ./gradlew :integration:excel:connectedDebugAndroidTest`: BUILD SUCCESSFUL, una prueba Android permanente sin fallos.
- Prueba temporal Android contra el archivo real ubicado en Descargas del Z2474: importación exitosa, 53 filas incluidas y 3,856 excluidas por ausencia de DNI. No se imprimieron datos de filas. La prueba temporal se eliminó y no se versionaron el archivo ni su ruta.
- `./gradlew :integration:excel:testDebugUnitTest :app:compileDebugKotlin`: BUILD SUCCESSFUL.
- `./gradlew :app:assembleDebug lintDebug testDebugUnitTest :core:domain:test`: BUILD SUCCESSFUL, 379 tareas; lint sin errores y tests sin fallos.
- APK corregida instalada en Z2474 y arranque COLD correcto.
- `git diff --check`: sin errores.

Siguiente comprobación manual: abrir Menú → Importar archivo en Z2474, seleccionar el XLSX, observar el progreso y confirmar que aparece la vista previa con 53 incluidos y 3,856 excluidos. Cambios locales sin commit ni push.

## Análisis de mensajería + IA + Supabase — 2026-10-13

Se revisó la propuesta de recordatorios de cobranza, recepción de vouchers, clasificación Gemini, sincronización Supabase y asistente de respuestas. Se documentó la arquitectura y contratos propuestos en [WHATSAPP_AI_SUPABASE_ANALYSIS](WHATSAPP_AI_SUPABASE_ANALYSIS.md), sin implementar código ni crear recursos remotos.

Hallazgo bloqueante: la política vigente de WhatsApp Business incluye expresamente `payday loans`, préstamos entre pares y `debt collection` entre los usos prohibidos, con independencia de licencias locales. Una plantilla Utility aprobada no autoriza ese uso. La arquitectura queda planteada con proveedor intercambiable y un `policy gate`; WhatsApp debe permanecer deshabilitado para recordatorios, negociación y cobranza.

El análisis también confirma prerrequisitos aún ausentes: Room y aplicación idempotente de pagos, identidad estable a través de importaciones, teléfonos verificados, consentimiento por canal/finalidad, política de reimportación/exportación y sincronización outbox/inbox. El directorio efímero y el campo rotulado DNI de la POC no bastan para asociar conversaciones o deudas.

Se definieron estados separados para conversación, evidencia, revisión, pago y entrega; schemas JSON para texto y voucher; contrato incremental Android/Room; manejo de la carrera de pago offline antes del lote vespertino; worker durable; y límites/costos. Gemini Free se descarta para datos reales porque sus términos indican no enviar información personal, sensible o confidencial. Vouchers e intenciones siempre requieren revisión humana y no modifican saldo.

Siguiente decisión: escoger un canal permitido y validar marco contractual/legal. Antes de cualquier integración remota, continuar con fase 2 local durable e identidad/reimportación/exportación. Solo análisis y documentación; sin compilación, pruebas Android, commit ni push.

### Blindaje de plantillas — 2026-10-13

Se aclaró que la aprobación de una plantilla no autoriza una finalidad prohibida ni garantiza que Meta no la pause posteriormente. El análisis incorpora un `ChannelPolicyGate` cerrado por defecto, registro versionado de plantillas, variables tipadas, consentimiento por finalidad, control de destinatario, revisión previa, límites de frecuencia, canary y `kill switch`. WhatsApp para cobranza permanece bloqueado hasta obtener de Meta o un BSP una determinación escrita de elegibilidad sobre el modelo operativo completo; no se recomienda disfrazar recordatorios de mora como mensajes Utility.

## Análisis de comprobantes compartidos — 2026-10-13

Se analizó recibir manualmente una captura desde WhatsApp u otra aplicación mediante Android Sharesheet. La propuesta se documentó en [SHARED_VOUCHER_POC](SHARED_VOUCHER_POC.md). Este mecanismo no lee chats, controla WhatsApp ni requiere Business API: el cobrador elige `Compartir` y CobroRutApp recibe temporalmente una imagen.

La primera POC debe usar OCR local y parsers deterministas por formato; Gemini Vision pagado queda como fallback solo si una comparación medida demuestra mejora. La imagen crea evidencia `EN_REVISION`, nunca un ingreso confirmado. Remitente y deudor siguen separados; nombre y monto solo generan candidatos que el cobrador debe seleccionar junto con el préstamo. Se debe vincular una captura con una posible notificación Plin o entrada manual sin duplicar abonos.

Antes de implementar: obtener capturas Yape/Plin anonimizadas y autorizadas fuera de Git, validar campos realmente visibles, definir retención y acordar si la POC será efímera o esperará Room. Solo análisis y documentación; sin dependencias, manifiesto, compilación, pruebas Android, commit ni push.

## Publicación de cambios — 2026-10-13

Por encargo explícito del usuario ("subamos nuestro cambios"), se preparó y ejecutó el commit y push de las entregas acumuladas (POC de flujo integrado de campo, matcher/parser de pagos, asistente manual, lectura XLSX corregida en Android y análisis de integraciones).

Validaciones de la entrega:
- `./gradlew :app:assembleDebug lintDebug testDebugUnitTest :core:domain:test --console=plain`: BUILD SUCCESSFUL; 44 tests JVM sin fallos (15 domain, 16 notifications, 6 excel, 6 collection, 1 app).
- `./gradlew :app:connectedDebugAndroidTest :integration:excel:connectedDebugAndroidTest`: BUILD SUCCESSFUL en `emulator-5554` (2 tests Android aprobados).
- `git diff --check`: sin errores de whitespace.
- Ignoradas carpetas locales `/output/` y `/tmp/` en `.gitignore`.
