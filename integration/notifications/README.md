# Notifications

POC diagnóstica disponible solo en `debug` para observar posibles notificaciones de ingreso de Yape e Interbank/Plin en un dispositivo de prueba. No es una integración bancaria, no confirma pagos y no interpreta montos.

El listener acepta únicamente estos paquetes exactos:

- `com.bcp.innovacxion.yapeapp`
- `pe.com.interbank.mobilebanking`

Antes de leer los extras se descartan otros paquetes. También se descartan resúmenes de grupo, notificaciones permanentes y textos que parezcan envíos, promociones o códigos de seguridad. Las heurísticas de posibles ingresos son conservadoras:

- Yape: `te yapeó`, `te yapearon`, o `recibiste`/`has recibido` con contexto de Yape.
- Interbank: contexto de `Plin` junto con `recibiste`, `has recibido`, `te plineó` o `te ha plineado`.

La forma `te ha plineado` y el ciclo de la sesión se validaron manualmente en un Z2474 con Interbank 23.4. Yape y otros formatos siguen pendientes; una coincidencia solo produce una muestra diagnóstica y nunca confirma un ingreso.

Solo después de superar ese filtro se copian versiones acotadas de título, texto, texto expandido, líneas de texto y canal. La huella SHA-256 identifica técnicamente una publicación mediante paquete, clave de notificación, hora de publicación y campos extraídos. Dos publicaciones distintas con el mismo nombre o monto conservan huellas distintas; la POC no deduplica pagos.

El servicio no abre pantallas, no recupera notificaciones anteriores al conectarse y no registra contenidos en logs. Su coroutine scope vive separado de cualquier `Activity` y termina al destruirse el servicio. El repositorio y su binding Hilt son ensamblados por la app en la variante `debug`.
