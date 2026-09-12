# CobroRutApp

Android nativo para cobranza por rutas. Fase 0: base local con navegación Jornada/Cobranza/Ingresos y preferencia de tema; aún sin operaciones de cobranza.

Repositorio: https://github.com/devlouis/cobrorutapp-android.git (`origin`, propietario `devlouis`). Application ID y namespace existentes: `com.pe.cobrorutapp`.

**Antes de trabajar en cada sesión, lee explícitamente [AGENTS.md](AGENTS.md), [WORK_STATUS](docs/WORK_STATUS.md) y [ROADMAP](docs/ROADMAP.md), y revisa Git/código.** No todos los chats o IDE cargan AGENTS automáticamente. No editar el mismo checkout desde chat y CLI simultáneamente.

## Abrir y compilar

Abrir esta raíz en Android Studio y sincronizar Gradle. Usar el Wrapper versionado, no Gradle global. Entorno detectado: macOS arm64; Gradle 9.6.0, AGP 9.4.0, Kotlin/Compose compiler 2.2.10; Android SDK 37.0 y Build Tools 36.0.0/37.0.0 instalados. `compileSdk/targetSdk = 37`, `minSdk = 26` pendiente de validar parque de dispositivos. Java/Kotlin generan bytecode 11.

El Wrapper conserva el criterio de daemon JDK 25 de la plantilla. Android Studio aporta JBR compatible; `./gradlew --version` muestra el criterio real. JDK mínimo de AGP es 17, pero este checkout pide 25: instalarlo/seleccionarlo o permitir resolución mediante el resolver ya configurado. La primera compilación necesita red para dependencias. Configurar SDK desde Android Studio o `local.properties` local (nunca versionarlo).

```sh
./gradlew --version
./gradlew :app:assembleDebug lintDebug testDebugUnitTest :core:domain:test
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`. Lint: `<módulo>/build/reports/lint-results-debug.html`; tests JVM: `<módulo>/build/reports/tests/`. Los tests heredados son de plantilla, no prueba de lógica de cobranza. No hay tests nuevos triviales de pantallas vacías.

## Ejecutar

Con Android SDK platform-tools en PATH y un emulador/dispositivo autorizado:

```sh
adb devices -l
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:installDebug
adb shell am start -W -n com.pe.cobrorutapp/.MainActivity
```

Las pruebas instrumentadas pueden retirar la app al terminar; instalar después, en una ejecución separada.

Tocar las tres pestañas y comprobar sus estados vacíos. El botón de tema recorre sistema → claro → oscuro y guarda la preferencia en DataStore. Cerrar el proceso y volver a abrir para comprobar persistencia. No requiere permisos sensibles ni cuenta bancaria.

## Documentación y pendientes

- [Contexto, reglas y dudas del producto](docs/PRODUCT_CONTEXT.md)
- [Módulos, versiones, dependencias y fuentes oficiales](docs/ARCHITECTURE.md)
- [Fases y criterios de cierre](docs/ROADMAP.md)
- [Trabajo y verificaciones reales](docs/WORK_STATUS.md)

Room queda preparado como dependencia; modelo, DAO, transacciones y migraciones son fase 2. Excel/notificaciones son esqueletos. El usuario facilitó `2SJL17ABRIL26.xlsx` fuera del repositorio durante el cierre de fase 0. Se comprobó su existencia, pero no fue inspeccionado; bibliotecas XLSX/OCR pendientes de pruebas técnicas. No subir datos reales; fixtures sintéticos revisados bajo `test-fixtures/synthetic/`.

Firebase/Crashlytics pendiente: obtener configuración real para `com.pe.cobrorutapp`, validar package name y seguir los pasos de ARCHITECTURE. La ausencia de `google-services.json` no impide compilar. Sin Analytics ni datos sensibles en reportes.

La cuenta Git de este proyecto es devlouis; autenticación local aislada. No cambiar configuración global ni credenciales de otros proyectos. El trabajo de fase 0 debe quedar en commit local si las validaciones pasan. **No ejecutar push sin encargo posterior.** Comando preparado para la rama actual, tras comparar remoto: `git push origin main`.

## Continuar en otro chat o CLI

> Continúa CobroRutApp. Lee AGENTS.md, docs/PRODUCT_CONTEXT.md, docs/ARCHITECTURE.md, docs/ROADMAP.md y docs/WORK_STATUS.md. Inspecciona Git y el código antes de editar. Retoma la fase en curso y completa únicamente su siguiente tarea pendiente. Si no hay fase en curso, resume el estado y propón la primera tarea de la siguiente fase antes de implementarla. Actualiza WORK_STATUS con cambios, validación real y siguiente paso. No asumas contexto de otros chats ni ejecutes push.
