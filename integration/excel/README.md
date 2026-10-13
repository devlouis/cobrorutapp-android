# excel

POC de fase 1 para lectura local de `Plan1`. Usa directamente el contenedor OOXML con APIs Java compatibles con Android, sin una biblioteca XLSX adicional. Lee cadenas compartidas, cadenas inline y valores cacheados de fórmulas; no evalúa fórmulas ni modifica el archivo.

Solo incluye filas con `NOMBRE` y `DNI` presentes. Conserva fecha, servicio y número de cuota como valores crudos, y representa importes válidos en céntimos PEN. Los DNI con formato distinto a ocho dígitos se incluyen con una advertencia porque esta fase valida presencia, no autenticidad.

El módulo depende solo de domain. La app ensamblará el contrato y la UI suministrará un `WorkbookSource`; data no depende de esta integración. Persistencia Room, reimportación y exportación continúan fuera de esta POC.
