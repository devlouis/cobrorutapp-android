---
name: model-routing
description: Clasifica tareas de ingeniería de software por complejidad y selecciona o recomienda el modelo y nivel de razonamiento menos costoso que pueda resolver correctamente cada fase.
---

# Model Routing Skill

Usa esta skill cuando una tarea de ingeniería de software pueda beneficiarse de elegir entre Sol y Astra o de ajustar el nivel de razonamiento.

## Objetivo

Seleccionar la configuración menos costosa que pueda resolver correctamente la fase actual de trabajo.

Optimiza en conjunto:

- corrección;
- mantenibilidad;
- latencia;
- eficiencia de cuota;
- riesgo;
- ambigüedad;
- alcance.

No optimices solo por capacidad bruta del modelo.

## Paso 1 — Clasificar la fase actual

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

Configuración preferida:

**Sol / Low**

### Nivel 2 — Ingeniería estándar

Ejemplos:

- pantalla Compose siguiendo patrones existentes;
- ViewModel;
- UseCase;
- Repository;
- Mapper;
- Room;
- integración API ya definida;
- navegación;
- feature siguiendo arquitectura existente;
- refactor acotado a un área conocida;
- tests de comportamiento conocido.

Configuración preferida:

**Sol / Medium**

### Nivel 3 — Ingeniería compleja

Ejemplos:

- repositorio o subsistema desconocido;
- análisis o propuesta de arquitectura;
- requerimientos ambiguos;
- debugging difícil;
- feature multi-módulo;
- concurrencia;
- análisis de rendimiento;
- migraciones;
- refactor transversal;
- decisión técnica con impacto relevante.

Configuración preferida:

**Astra / Medium**

### Nivel 4 — Complejidad excepcional

Ejemplos:

- incidente severo en producción;
- varios intentos fallidos de diagnóstico;
- condición de carrera compleja;
- migración arquitectónica de alto impacto;
- diseño sensible a seguridad con alta incertidumbre;
- refactor transversal grande y riesgoso.

Configuración preferida:

**Astra / High**

Usa este nivel solo cuando esté claramente justificado.

## Paso 2 — Verificar si realmente es necesario escalar

Antes de escalar:

1. Lee `AGENTS.md`.
2. Lee la documentación aplicable de la tarea.
3. Inspecciona el código existente.
4. Busca componentes, patrones y utilidades ya disponibles.
5. Reproduce o comprende el problema.
6. Separa falta de información de dificultad real de razonamiento.

Una tarea grande no es automáticamente una tarea de alta complejidad cognitiva.

## Paso 3 — Reevaluar después de cada fase

No mantengas una configuración costosa por inercia.

Flujo recomendado:

`Astra / Medium` para discovery, análisis o arquitectura  
→ `Sol / Medium` para implementación con solución conocida  
→ `Sol / Low` para tests, cleanup, imports y formato

Si durante la implementación aparece un bloqueo genuinamente complejo:

`Sol / Medium`  
→ `Astra / Medium`

Escala a:

`Astra / High`

solo cuando Medium resulte insuficiente y haya una justificación técnica clara.

## Paso 4 — Routing por subtareas

Cuando una tarea contenga subtareas con distinta complejidad, clasifica cada una por separado.

Ejemplo:

- analizar un módulo desconocido → **Astra / Medium**
- definir la solución → **Astra / Medium**
- implementar la solución aprobada → **Sol / Medium**
- agregar tests conocidos → **Sol / Low**
- corregir imports y formato → **Sol / Low**
- investigar un fallo inesperado complejo → **Astra / Medium**

No mantengas Astra para tareas mecánicas solo porque una fase anterior lo necesitó.

## Paso 5 — Respetar capacidades reales del entorno

Si el entorno soporta subagentes o delegación con selección de modelo:

- delega cada subtarea al modelo menos costoso que pueda resolverla correctamente;
- mantén implementación rutinaria, tests y cleanup en Sol;
- reserva Astra para discovery, arquitectura, ambigüedad y debugging difícil.

Si el entorno no puede cambiar automáticamente el modelo activo:

- no afirmar que el cambio ocurrió si no ocurrió;
- clasificar la tarea según esta skill;
- continuar si la configuración actual es adecuada;
- si otra configuración es materialmente mejor, recomendar explícitamente el cambio antes de realizar trabajo costoso.

## Paso 6 — Comportamiento de salida

Realiza la clasificación de forma silenciosa por defecto.

No informes el routing en cada respuesta.

Menciona modelo y razonamiento solo cuando:

- sea necesario cambiar de configuración;
- el usuario pregunte qué modelo conviene;
- la eficiencia de cuota sea parte explícita de la tarea;
- sea necesario escalar porque intentos anteriores fallaron;
- el usuario pida validar esta skill.

## Reglas de eficiencia

Evita Astra para:

- formato;
- imports;
- boilerplate;
- cambios simples de UI;
- tests triviales o de comportamiento ya definido;
- correcciones rutinarias de compilación;
- documentación mecánica.

Prefiere Astra para:

- discovery de repositorio;
- arquitectura;
- resolución de ambigüedad;
- debugging difícil;
- análisis de concurrencia o rendimiento;
- decisiones técnicas de alto impacto.

La configuración normal de ingeniería debe seguir siendo:

**Sol / Medium**

## Relación con AGENTS.md

`AGENTS.md` contiene las reglas permanentes del proyecto.

Esta skill define el procedimiento para clasificar tareas y reevaluar el routing por fase.

Si existe una contradicción, prevalece `AGENTS.md`.

No uses esta skill para modificar alcance, arquitectura, reglas de negocio o restricciones definidas en `AGENTS.md`.

## Casos de validación

### Caso A

Tarea:

> Cambia el texto de un botón y elimina imports no usados.

Esperado:

**Sol / Low**

### Caso B

Tarea:

> Implementa un ViewModel y Repository siguiendo la arquitectura existente.

Esperado:

**Sol / Medium**

### Caso C

Tarea:

> Analiza un repositorio desconocido, identifica su arquitectura y propone una migración multi-módulo.

Esperado:

**Astra / Medium**

### Caso D

Tarea:

> Llevamos varios intentos fallidos investigando una condición de carrera en producción que afecta varios módulos.

Esperado:

**Astra / High**

### Caso E — Routing por fases

Tarea:

> Analiza un módulo desconocido, diseña la solución, implementa la feature y luego agrega tests y cleanup.

Esperado:

- análisis / arquitectura → **Astra / Medium**
- implementación → **Sol / Medium**
- tests / cleanup → **Sol / Low**
