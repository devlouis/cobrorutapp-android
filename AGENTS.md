# CobroRutApp — guía compartida

## Antes de editar, en cada sesión

Lee este archivo, [WORK_STATUS](docs/WORK_STATUS.md), [ROADMAP](docs/ROADMAP.md) y el estado real de Git. Consulta [PRODUCT_CONTEXT](docs/PRODUCT_CONTEXT.md) y [ARCHITECTURE](docs/ARCHITECTURE.md) según la tarea. La documentación no sustituye inspeccionar código, versiones y cambios locales. No asumas historial de otros chats. No edites este checkout simultáneamente desde chat y CLI.

Comunicación en español; identificadores de código en inglés. Completa únicamente la fase encargada. No avances automáticamente. Al terminar, actualiza WORK_STATUS con implementación, comandos/resultados reales, pendientes y siguiente tarea.

## Arquitectura

Android nativo, Kotlin, Compose/Material 3, MVVM y flujo unidireccional: UiState con StateFlow; acciones por métodos del ViewModel. UI sin DAO, parsers, archivos ni Firebase. Features → domain y designsystem; data → domain; integraciones → domain si lo requieren; app ensambla con Hilt. Domain es Kotlin/JVM, sin Android ni módulos del proyecto. Sin ciclos ni dependencias entre features. Casos de uso solo para reglas/coordinación relevantes; no crear capas que solo reenvían llamadas.

Room será fuente de verdad de cobranza; DataStore solo preferencias. En fase 0 no hay esquema Room ni operaciones de cobranza. Sin tablas de relleno ni migración destructiva. No elegir XLSX/OCR antes de las pruebas técnicas de fase 1. No añadir red, WorkManager, Analytics o permisos sin caso implementado y explicado.

## Reglas que no deben perderse

- Dinero en céntimos `Long` y moneda explícita; nunca `Double` monetario.
- Saldo importado como base: descontar solo nuevos abonos, nunca otra vez el historial.
- Remitente y cliente deudor separados; pagos por terceros permitidos.
- Nombre solo sugiere: siempre confirmar humanamente cliente/deuda/monto. Pendientes no afectan saldo.
- Notificación, captura y manual pueden evidenciar un mismo pago. Conservar evidencias sin duplicar abonos. Nombre + monto no identifica un pago; distinguir duplicado técnico de sospecha revisable.
- Confirmación + aplicación en transacción idempotente. Correcciones preservan historial y motivo.
- Definir/probar política de reimportación antes de habilitarla. Nunca borrar o duplicar cobros. Pendientes sobreviven cierre y cambio de día.
- Captura independiente de pantallas/ViewModel; validar restricciones reales, incluida pantalla bloqueada. Avisar con notificación tocable, sin abrir UI automáticamente. Filtrar solo fuentes/tipos necesarios.
- Imagen/OCR no confirma ingreso bancario. Sin aprobación del supervisor dentro de la app.
- No inventar significado de C/N/A, colores ni estructura del Excel ausente. Solo fixtures sintéticos en Git.
- Nunca reportar DNI, nombres, montos, comprobantes o notificaciones a Crashlytics; no añadir Analytics.

## Validación y Git

Desde la raíz, con JDK/SDK indicados en README:

```sh
git status --short --branch
./gradlew :app:assembleDebug lintDebug testDebugUnitTest :core:domain:test
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:installDebug
adb shell am start -W -n com.pe.cobrorutapp/.MainActivity
git diff --check
```

Los comandos de dispositivo requieren uno autorizado; revisar `adb devices -l`. Verificar visualmente arranque y las tres pestañas. `NO-SOURCE` no prueba reglas. No añadir tests triviales de pantallas vacías ni afirmar verificaciones manuales no ejecutadas.

Respetar cambios ajenos, rama e historial. Remoto origin: `https://github.com/devlouis/cobrorutapp-android.git`; autenticación local de devlouis, sin cambiar cuentas/configuración global. Consultar remoto antes de publicar. No `reset --hard`, `clean` destructivo ni force push. Revisar y agregar rutas explícitas. Commit solo con identidad existente y validaciones satisfactorias. **Push requiere un encargo posterior explícito.** No versionar secretos, rutas personales, firmas ni datos reales. El Wrapper sí se conserva.
