# ProyectoFinalCiencias1
Motor/Gestor de Bases de Datos con Árboles Autobalanceados
Objetivo Desarrollar un motor/gestor de bases de datos (relacional o no relacional a elección) que soporte comandos básicos de definición y manipulación de datos (crear/borrar tablas/espacios, insertar, actualizar, borrar, buscar) y utilice una estructura de árbol autobalanceado (AVL o Rojo‑Negro) para indexación/almacenamiento y búsqueda eficiente. La solución debe ser ejecutable y defendida presencialmente.

# Requisitos funcionales obligatorios
# Gestión de esquemas
Comandos para crear y borrar "espacios" de almacenamiento (tablas/colecciones/entidades).
Definición simple de esquema: nombre de espacio + lista de campos con tipos básicos (entero, texto, real, boolean).
Soportar espacios sin esquema fijo si el grupo elige enfoque no relacional.
Operaciones CRUD básicas
INSERT: insertar registros en un espacio.
SELECT / FIND: buscar registros por clave primaria y por consultas simples (igualdad y rango sobre campos indexados).
UPDATE: actualizar uno o varios registros que cumplan criterio.
DELETE: eliminar registros por clave o por condición.
Índice basado en árbol autobalanceado
Implementar internamente un árbol autobalanceado (elección: AVL o Rojo‑Negro) para indexar por la clave primaria y/o por otros campos indexados.
Operaciones del árbol: inserción, búsqueda, eliminación y recorrido (inorder).
Garantizar complejidad O(log n) en operaciones sobre el índice.
Persistencia
Datos persistidos en archivos (formatos permitidos: JSON, CSV, binario simple); el estado del espacio debe poder restaurarse al reiniciar el gestor.
El índice también debe persistirse o reconstruirse rápidamente desde los archivos de datos al iniciar.
Interfaz de uso
Interfaz de línea de comandos (requerida): REPL simple que acepte comandos definidos (SQL‑like o comandos propios).
Interfaz gráfica (opcional, extra crédito): GUI minimalista para ejecutar comandos, ver contenidos y visualizar estructura del árbol.
Transacciones y consistencia (mínimo básico)
Al menos garantizar atomicidad básica por operación (si una operación falla no debe dejar datos corruptos).
Manejo simple de concurrencia no obligatorio; si se implementa, documentarlo.
Requisitos no funcionales

## Lenguajes permitidos: Python, Java, C++, C#, o JavaScript/TypeScript. Uso de librerías estándar permitido; la implementación del árbol debe ser propia (no usar implementaciones de AVL/RB externas).
## Documentar complejidad temporal/espacial de las operaciones implementadas.
## Código legible y modular, con pruebas unitarias mínimas.
### Entregables

## Repositorio con código fuente y README (instrucciones para ejecutar, ejemplos de comandos).
## Documentación técnica (máx. 6 páginas) que incluya:
## Diseño arquitectónico (módulos: parser, almacenamiento, índice, persistencia, API CLI/UI).
## Especificación del lenguaje/comandos soportados.
## Diseño del árbol (AVL o Rojo‑Negro): invariantes y cómo se mantienen.
## Formato de persistencia y política de recuperación.
## Análisis de complejidad.
## Conjunto de datasets de prueba (al menos 2: pequeño y mediano) y scripts para cargar datos.
## Pruebas: suite con casos que demuestren inserción, búsqueda, actualización, eliminación y recuperación tras reinicio.
## Demo y defensa presencial (8–12 minutos por grupo) — no requiere video.
## (Opcional) Interfaz gráfica empaquetada o instrucción para lanzar.
