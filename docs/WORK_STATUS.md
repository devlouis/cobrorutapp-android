# Estado de trabajo

Fecha: 2026-09-12. **Fase 0 completada; ninguna fase en curso.** Fase 1 preparada, pendiente de encargo explícito.

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
