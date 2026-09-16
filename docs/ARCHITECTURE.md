# Arquitectura

## Módulos

La tabla recibida tenía etiquetas truncadas. Los siguientes nombres concretan las responsabilidades indicadas, con un módulo por feature (sin triplicar cada una):

| Módulo | Responsabilidad | Dependencias de proyecto permitidas |
| --- | --- | --- |
| `:app` | Application, actividad, navegación, DI y ViewModel de preferencias globales | Todos los módulos para ensamblar |
| `:core:domain` | Modelos/contratos/reglas Kotlin/JVM sin Android | Ninguna |
| `:core:data` | Repositorios, Room/SQLite y preferencias DataStore | domain |
| `:core:designsystem` | Tema Material 3 y componentes compartidos | Ninguna |
| `:feature:workday` | Importación, inicio, cierre y exportación | domain, designsystem |
| `:feature:collection` | Clientes, deudas, pagos manuales y gastos | domain, designsystem |
| `:feature:income` | Ingresos detectados, evidencias y asociación | domain, designsystem |
| `:integration:notifications` | Futuro servicio y parsers de notificaciones | domain, si es necesario |
| `:integration:excel` | Futura lectura, validación y escritura Excel | domain, si es necesario |

Sin dependencias entre features ni ciclos. Data no depende de integraciones: usar contrato de domain implementado por integración y ensamblado por app. Las integraciones solo contienen configuración compilable, manifiesto vacío y documentación en fase 0.

## Presentación y DI

Compose + Material 3, MVVM y flujo unidireccional. `AppViewModel` expone `StateFlow<AppUiState>` y `cycleTheme()`. La actividad observa con lifecycle y entrega estado/acciones a Compose. `@HiltAndroidApp` y `@AndroidEntryPoint` habilitan la Application y actividad reales; Hilt crea el ViewModel con el contrato `UserPreferencesRepository`. `AppModule` proporciona su implementación de data como singleton. No hay acceso desde UI a archivos/DAO/parsers.

Navigation Compose mantiene tres destinos raíz, evita duplicación con `launchSingleTop` y restaura estado al cambiar pestañas. Las pantallas vacías son funciones sin ViewModels artificiales. El cambio de tema sistema/claro/oscuro es una preferencia real persistida; no simula capacidades de cobranza. No hay casos de uso que solo reenvíen llamadas.

## Persistencia

DataStore Preferences solo almacena tema mediante un delegado único sobre el application context. Errores de E/S se muestran sin registrar contenido privado; la UI evita cambios antes de cargar preferencias o mientras guarda.

Room runtime está resuelto en `core:data`; su compilador se fija en el catálogo pero **no se aplica hasta que existan entidades reales en fase 2**. No hay `RoomDatabase`, DAO, tablas de relleno, esquema de cobranza ni migraciones. La capacidad de persistencia de pagos no está implementada ni validada. En fase 2 añadir KSP/compiler en data, exportar esquemas y probar transacciones, idempotencia y migraciones sin fallback destructivo.

Entidades previstas, sujetas a diseño: Customer, Route, Workday, Debt, ImportBatch/ImportedRow, DetectedIncome, Payment, PaymentAllocation, PaymentEvidence y Expense. Invariantes en PRODUCT_CONTEXT y AGENTS.

## Versiones y decisiones (fase 0)

| Componente | Versión/valor | Motivo |
| --- | --- | --- |
| AGP / Gradle Wrapper | 9.4.0 / 9.6.0 | Plantilla existente y pareja oficial compatible |
| Kotlin / plugin Compose | 2.2.10 | Plantilla; AGP resuelve este KGP incorporado; no aplicar kotlin-android |
| JDK del daemon | 25 | Criterio existente del Wrapper, disponible mediante Android Studio; mínimo de AGP 17 |
| Bytecode Java/Kotlin | 11 | Se conserva target de plantilla, uniforme en módulos |
| compileSdk / targetSdk | 37 / 37 | SDK 37.0 instalado y soportado por AGP 9.4 |
| minSdk | 26 | Propuesta del encargo; baja desde 29; parque físico pendiente |
| Compose BOM / Material 3 | 2026.02.01 / 1.4.0 resuelto | Se conserva la plantilla |
| Hilt | 2.60.1 | Versión estable con plugin compatible con AGP moderno y correcciones del compilador |
| KSP | 2.3.12 (KSP2) | Estable, soporta AGP ≥ 8.12; evita kapt incompatible con Kotlin incorporado |
| Room | 2.8.4 | Estable, minSdk 23 compatible; runtime preparado, esquema pospuesto |
| DataStore Preferences | 1.2.1 | Estable, preferencias con Flow |
| Navigation Compose | 2.9.7 | Navegación mínima estable sin introducir Navigation 3 |
| Lifecycle | 2.9.4 | Observación de StateFlow consciente del ciclo de vida y ViewModel |
| Coroutines | 1.10.2 | Flow/coroutines estable compatible con Kotlin 2.2 |

Core KTX 1.10.1, Activity Compose 1.8.0 y dependencias de tests conservan declaraciones de plantilla; Gradle puede elevar transitivamente algunas versiones. No se afirma que las declaraciones antiguas sean las versiones efectivamente resueltas. Consultar `:app:dependencies --configuration debugRuntimeClasspath` para el grafo real. La consulta ejecutada en fase 0 resolvió Core KTX 1.16.0, Activity Compose 1.8.2, Material 3 1.4.0 y Kotlin stdlib 2.3.21. Esta última es la biblioteca de runtime, no el plugin/compiler Kotlin 2.2.10. Compilación real y arranque verificados con ese grafo. Versiones directas fijas en `libs.versions.toml`, sin rangos ni pre-releases añadidas.

El archivo Gradle raíz centraliza SDK y target Java de bibliotecas; sin build-logic complejo. Compose solo se aplica a app, designsystem y features. Hilt/KSP solo a app, donde existen anotaciones. Domain usa kotlin-jvm y Coroutines; ninguna API Android.

## Fuentes oficiales consultadas

- [AGP 9.4 y compatibilidad](https://developer.android.com/build/releases/agp-9-4-0-release-notes)
- [Kotlin incorporado en AGP](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Hilt y procesamiento](https://dagger.dev/hilt/gradle-setup.html), [Dagger releases](https://github.com/google/dagger/releases)
- [KSP 2.3.12](https://github.com/google/ksp/releases/tag/2.3.12)
- [Room](https://developer.android.com/jetpack/androidx/releases/room), [DataStore](https://developer.android.com/jetpack/androidx/releases/datastore)
- [Navigation](https://developer.android.com/jetpack/androidx/releases/navigation), [Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle)
- [Coroutines 1.10.2](https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.10.2)

## Firebase y siguientes decisiones

No existe `google-services.json`; la base compila sin plugins, SDK ni inicialización Firebase. Antes de incorporar Crashlytics: obtener configuración real autorizada, verificar `client_info.android_client_info.package_name == com.pe.cobrorutapp`, añadir plugins/BOM/SDK estables compatibles y realizar una prueba controlada de crash. No crear archivo falso ni proyecto remoto. Mantener Analytics fuera; no registrar datos de clientes, dinero o evidencias. [Guía oficial](https://firebase.google.com/docs/crashlytics/get-started?platform=android).

No seleccionar biblioteca XLSX u OCR todavía. No añadir red, trabajo en segundo plano ni permisos sin una función concreta. En fase 1 documentar viabilidad en dispositivos reales y round-trip del Excel antes de implementar funcionalidades productivas.

## POC de notificaciones — fase 1 (debug)

El usuario autorizó una prueba técnica de captura de Yape/Interbank. El contrato de diagnóstico vive en domain; el servicio y filtro de notificaciones, en el source set debug de integration:notifications; el repositorio de muestras descartables, en core:data/debug; pantalla, ViewModel y ensamblado Hilt, en app/debug. La entrada release es vacía y no declara el servicio. No hay dependencias data → integration ni entre features. Hilt/KSP se aplica también en integration:notifications para su servicio debug.

Se usa un archivo atómico acotado fuera de backup para diagnóstico, no para pagos/jornadas. Room sigue pendiente de fase 2 como fuente de verdad de cobranza. No convertir las muestras de esta POC en registros financieros. Diseño, límites, filtros provisionales y validación manual en [NOTIFICATION_POC](NOTIFICATION_POC.md).
