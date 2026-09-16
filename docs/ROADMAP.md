# Hoja de ruta

Fase 0 completada. El usuario autorizó iniciar la parte técnica de fase 1 con una POC de lectura de notificaciones Yape/Plin (Interbank) y mantener un checklist de reglas pendientes. No avanzar a funcionalidad productiva ni a otras fases.

| Fase | Estado | Entrega | Criterio de cierre |
| --- | --- | --- | --- |
| 0 — Espacio de trabajo | Completado | Git, documentación, módulos, dependencias, DI, tema y navegación mínima | Debug compila; lint y pruebas disponibles pasan; instalación, arranque y navegación si hay dispositivo |
| 1 — Validaciones técnicas | En curso: Plin comprobado; Yape pendiente | POC Yape/Interbank y round-trip del Excel real | Contenido real disponible, limitaciones y conservación documentados; bibliotecas decididas con evidencia |
| 2 — Cobranza local | Pendiente | Modelo persistente, clientes/deudas, pagos manuales, saldos | Reinicio, transacciones, idempotencia y terceros verificados |
| 3 — Jornada y Excel | Pendiente | Importación real, movimientos, gastos, cierre, exportación | Importar → cobrar → exportar; reimportación e historial sin pérdida ni duplicación |
| 4 — Ingresos digitales | Pendiente | Captura, bandeja, sugerencias, evidencias | Confirmación humana, pendientes persistentes y duplicados controlados |
| 5 — Comprobantes | Pendiente | Compartir desde WhatsApp y OCR | Revisión humana, respaldo y vínculo a pago existente |
| 6 — Piloto | Pendiente | Robustez, migraciones, recuperación, Crashlytics, campo | Jornada real validada, respaldo y fallos documentados |

Crashlytics puede entrar antes si hay configuración real válida; hoy está pendiente. La fase 1 autoriza POC acotadas, no implementación productiva completa.

Secuencia acordada: continuar ampliando las pruebas y comprobaciones de Plin cuando aparezcan nuevos casos o formatos. Después realizar el mismo bloque de pruebas con Yape en un dispositivo que tenga una cuenta operativa. El round-trip del Excel sigue pendiente de ejecución. No generalizar los resultados de Plin a Yape ni inventar formatos ausentes.
