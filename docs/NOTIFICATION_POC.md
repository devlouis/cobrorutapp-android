# POC de notificaciones — fase 1

## Alcance y estado

Prueba técnica debug para determinar qué texto exponen las notificaciones de ingreso de Yape y Plin en Interbank en el mismo Android del cobrador. No es integración bancaria ni acredita recepción de dinero; no crea pagos, deudas o abonos. No hay parser monetario ni asociación de clientes. La bandeja productiva es fase 4.

El acceso especial de Android permite observar notificaciones de forma amplia; la aplicación filtra fuentes antes de leer extras y solo retiene candidatos a ingresos durante una sesión iniciada por el usuario. No usa Accessibility, SMS, contactos, contraseñas, API bancaria, OCR, red, WorkManager ni permisos para publicar notificaciones. No abre pantallas desde el servicio.

## Diseño

- `core:domain`: contrato y valores de diagnóstico Kotlin puro; ninguna dependencia Android.
- `integration:notifications/src/debug`: NotificationListenerService enlazado por Android y filtro provisional. Independiente de Activity/ViewModel. Sin consulta de avisos anteriores ni cancelación de notificaciones del banco.
- `core:data/src/debug`: muestras JSON atómicas en `noBackupFilesDir`, máximo 20 y tamaño de campos limitado. Son diagnósticos descartables, **no almacenamiento de jornada**. Room sigue reservado para el modelo de negocio; no sustituirlo por este archivo.
- `app/src/debug`: ensamblado Hilt, ViewModel/StateFlow y pantalla de prueba con explicación, ajuste de acceso, inicio de sesión de 10 minutos, parada y borrado. La pantalla bloquea capturas y previews del sistema mediante FLAG_SECURE porque puede mostrar datos privados.
- `app/src/release`: punto de entrada vacío; sin pantalla o servicio de captura en el manifiesto release. No se añade una dependencia entre features ni entre data e integraciones.

La sesión empieza apagada. Caduca a los 10 minutos (reloj monotónico), se detiene al desconectarse el listener y no se restaura al recrear el proceso. Las muestras sí se restauran. Borrar muestras también detiene la sesión; no revoca el acceso de Android: hacerlo en Ajustes. La rotación o cierre de la pantalla no es el dueño de la captura; la controla el repositorio compartido con el servicio.

Error de almacenamiento: mostrar fallo sin registrar contenido, detener captura; ante archivo ilegible no sobrescribir silenciosamente, ofrecer borrado explícito. No hay exportación ni envío a Crashlytics. Diagnósticos se conservan hasta borrado o expulsión de los más antiguos al superar 20; borrarlos al terminar la prueba.

## Fuentes y filtro provisional

Paquetes permitidos exactos, verificados en las fichas oficiales:

- [Yape — Banco de Crédito del Perú](https://play.google.com/store/apps/details?id=com.bcp.innovacxion.yapeapp): `com.bcp.innovacxion.yapeapp`.
- [Interbank APP](https://play.google.com/store/apps/details?id=pe.com.interbank.mobilebanking): `pe.com.interbank.mobilebanking`.

Estos identificadores no prueban autenticidad bancaria ni sustituyen comprobar la procedencia de la app instalada en un dispositivo de prueba. No instalar imitaciones de paquetes bancarios para probar.

El filtro usa frases de ingreso provisionales y exclusiones de promociones, envíos y seguridad. Los textos de tests son **sintéticos**, no muestras verificadas de los bancos. Se ignoran resúmenes de grupo, avisos permanentes y fuentes no permitidas. Un aviso de Interbank requiere contexto Plin; otros movimientos bancarios quedan fuera. El usuario confirmó la frase «PERSONA te ha plineado S/MONTO», omitida por el filtro inicial; se contempla esa forma con fixtures sintéticos. Tras instalar la corrección, el usuario confirmó en Z2474 capturas con la app visible, pantalla bloqueada y segundo plano. También confirmó persistencia sin reanudación después de forzar detención, caducidad automática, revocación y reconexión sin reanudación, parada manual y borrado persistente. Son resultados manuales reportados en Z2474; otros formatos y Yape siguen pendientes. Texto vacío/oculto/desconocido se descarta, sin guardar extras para investigación general.

Limitación deliberada: un ingreso real cuyo formato no coincida puede omitirse. Obtener un ejemplo anonimizado con consentimiento del cobrador para ajustar el filtro; no ampliar la captura a todas las notificaciones de los bancos. Nunca afirmar que la ausencia de muestra implica ausencia de pago.

Se registran campos title/text/bigText/textLines, canal, origen y tiempos. Un hash técnico usa origen, clave del aviso, postTime y contenido: replay exacto no duplica; cambios de contenido o clave se conservan como muestras distintas. **No es una clave de pago**: el banco puede reutilizar avisos, cambiar tiempos o agrupar pagos. Nombre + monto no deduplica. No se confirma dinero automáticamente.

## Prueba manual en Android real

1. Registrar modelo, Android, versión de las dos apps, cuenta/perfil Android y escenario (sin nombres, cuentas, saldos ni teléfonos personales en Git).
2. Instalar APK debug. En Ingresos abrir «Prueba de notificaciones (debug)». Leer alcance y almacenamiento antes de abrir Ajustes. La app no solicita ni concede acceso al arrancar.
3. El usuario concede acceso a CobroRutApp en Ajustes y vuelve. Confirmar «Servicio conectado». Si el sistema impone restricciones de instalación administrada o de seguridad, documentarlas; no evadirlas.
4. Iniciar la prueba temporal y recibir un ingreso de prueba previamente acordado. El usuario realiza la operación bancaria; el asistente no inicia transferencias.
5. Comprobar en Yape/Interbank el ingreso real y comparar si Android mostró aviso y si la POC registró una muestra. No copiar textos reales a este documento ni capturar la pantalla de diagnóstico.
6. Repetir por fuente con pantalla abierta, app en segundo plano y pantalla bloqueada. Probar permisos revocados y notificaciones desactivadas en la app bancaria. Comprobar que promociones/envíos/OTP no se guardan usando ejemplos sintéticos o inspección local controlada, sin solicitar OTP reales.
7. Detener captura, volver a abrir: muestras previas deben mantenerse, nuevas no deben guardarse. Reiniciar proceso: sesión apagada, muestras presentes. Borrar al terminar y revocar el acceso si no se continuará probando.
8. Registrar resultado anonimizado: campos disponibles sí/no, agrupamiento, actualización/reuso de claves, posibles omisiones, redacción y restricciones. Los casos no ejecutados permanecen pendientes.

No se garantiza captura con pantalla bloqueada, proceso terminado, ahorro de batería, restricciones OEM, perfiles de trabajo o ausencia de notificación bancaria. En Android 15 ciertas notificaciones sensibles pueden llegar redactadas. No tratar de eludir esa protección.

## Validaciones

Resultados de compilación, tests sintéticos, almacenamiento y dispositivo se registran en WORK_STATUS. El bloque Plin quedó validado manualmente por el usuario en Z2474 para app visible, segundo plano, pantalla bloqueada, persistencia, caducidad, revocación, reconexión, parada y borrado. Yape y formatos alternativos continúan pendientes. El round-trip Excel es otro bloque de fase 1 y no está cubierto aquí.

## Referencias oficiales

- [NotificationListenerService y ciclo de vida](https://developer.android.com/reference/android/service/notification/NotificationListenerService)
- [Protección de notificaciones sensibles en Android 15](https://developer.android.com/about/versions/15/behavior-changes-all)
