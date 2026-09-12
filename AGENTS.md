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

## Política de selección de modelo

Optimiza simultáneamente calidad de ingeniería, tiempo de respuesta y uso de cuota.

La configuración por defecto para trabajo de ingeniería es:

**Sol / Medium**

No uses un modelo más costoso ni un nivel de razonamiento mayor cuando una configuración menor sea suficiente.

### Nivel 1 — Mecánico / bajo riesgo

Ejemplos:

- renombrar símbolos;
- imports;
- formato;
- strings;
- colores;
- pequeños ajustes visuales;
- boilerplate repetitivo;
- tests sencillos con comportamiento ya definido;
- correcciones simples de lint o compilación.

Preferir:

**Sol / Low**

### Nivel 2 — Ingeniería estándar

Ejemplos:

- pantalla Compose siguiendo patrones existentes;
- ViewModel, UseCase, Repository o Mapper;
- integración Room o API ya definida;
- navegación;
- implementación de una feature siguiendo arquitectura existente;
- refactor acotado a un área conocida;
- tests de comportamiento conocido.

Preferir:

**Sol / Medium**

### Nivel 3 — Ingeniería compleja

Ejemplos:

- repositorio o subsistema desconocido;
- análisis de arquitectura;
- requerimientos ambiguos;
- debugging difícil;
- cambios multi-módulo;
- concurrencia;
- análisis de rendimiento;
- migraciones;
- refactors transversales;
- decisiones técnicas con impacto relevante.

Preferir:

**Astra / Medium**

### Nivel 4 — Complejidad excepcional

Ejemplos:

- incidente severo en producción;
- varios intentos fallidos de diagnóstico;
- condición de carrera compleja;
- migración arquitectónica de alto impacto;
- diseño sensible a seguridad con alta incertidumbre;
- refactor transversal grande y riesgoso.

Preferir:

**Astra / High**

Usar este nivel solo cuando esté claramente justificado.

## Routing por fases

Una misma tarea puede cambiar de complejidad durante su ejecución.

No mantengas Astra activo durante toda la tarea solo porque fue útil al inicio.

Flujo recomendado:

1. Discovery / análisis / arquitectura  
   → **Astra / Medium**

2. Implementación con solución ya definida  
   → **Sol / Medium**

3. Tests / cleanup / imports / formato  
   → **Sol / Low**

4. Aparece un bloqueo genuinamente complejo  
   → **Astra / Medium**

5. El problema sigue sin resolverse después de investigar  
   → **Astra / High**

## Política de escalamiento

Empieza con la configuración más económica que pueda resolver correctamente la fase actual.

Ruta preferida:

`Sol / Low -> Sol / Medium -> Astra / Medium -> Astra / High`

Escala porque se necesita más razonamiento, no solo porque la tarea sea grande.

Antes de escalar:

1. inspecciona el código relevante;
2. lee este AGENTS.md y la documentación aplicable;
3. busca componentes, patrones y utilidades existentes;
4. reproduce o comprende el problema;
5. distingue falta de información de dificultad real de razonamiento.

Una tarea grande no es automáticamente una tarea de alta complejidad cognitiva.

## Delegación y cambio de modelo

Si el entorno de Codex soporta subagentes o delegación con selección de modelo:

- delega cada subtarea al modelo menos costoso que pueda resolverla correctamente;
- mantén implementación rutinaria, tests y cleanup en Sol;
- reserva Astra para discovery, arquitectura, resolución de ambigüedad y debugging difícil.

Si el entorno no puede cambiar automáticamente el modelo activo:

- no afirmar que el modelo fue cambiado si no ocurrió;
- clasificar la tarea según esta política;
- continuar si la configuración actual es adecuada;
- si otra configuración es materialmente mejor, recomendar explícitamente el cambio antes de realizar trabajo costoso.

La clasificación puede hacerse silenciosamente. Menciona el routing solo cuando:

- sea necesario cambiar de modelo o nivel de razonamiento;
- el usuario lo pregunte;
- la eficiencia de cuota sea parte de la tarea;
- haya que escalar porque intentos anteriores fallaron.

## Uso de la skill model-routing

Si existe `.codex/skills/model-routing/SKILL.md`, úsala como procedimiento complementario para clasificar tareas y reevaluar el modelo por fase.

AGENTS.md define las reglas permanentes del proyecto. La skill define el procedimiento reutilizable de clasificación y routing.

La skill no sustituye estas reglas ni debe asumir capacidades de cambio automático de modelo que el entorno no tenga.

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
