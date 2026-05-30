
# Motor de Base de Datos Relacional Indexado por Árbol AVL

Este proyecto es un motor de base de datos relacional ligero, desarrollado en Java, que utiliza una estructura de árbol AVL auto-balanceado para indexar registros y garantizar consultas en complejidad logarítmica $\mathcal{O}(\log n)$.

Soporta comandos estándar de definición (DDL) y manipulación (DML) de datos a través de una interfaz de línea de comandos (REPL) y una interfaz gráfica de usuario interactiva (Swing GUI) que visualiza dinámicamente la topología del árbol en tiempo real.

---

## Requisitos e Instalación

### Requisitos Previos
* **Java Development Kit (JDK) 22** o superior instalado.
* **Apache Ant** (opcional, para compilación mediante NetBeans).

### Compilación desde Consola
Para compilar todas las clases del proyecto y generar los archivos binarios en el directorio `bin`:
```powershell
javac -d bin src/com/dbmotor/core/*.java src/com/dbmotor/model/*.java src/com/dbmotor/storage/*.java src/com/dbmotor/parser/*.java src/com/dbmotor/gui/*.java src/com/dbmotor/utils/*.java src/com/dbmotor/Main.java
```

---

## Ejecución del Motor

El motor admite dos modos de arranque a través de la clase principal `com.dbmotor.Main`:

### 1. Modo Interfaz Gráfica (GUI) - Recomendado para Demo
Lanza un panel de control interactivo en Swing con consola SQL integrada, controles rápidos de dataset y un visualizador gráfico dinámico del árbol AVL:
```powershell
java -cp bin com.dbmotor.Main
```

### 2. Modo Consola (CLI REPL)
Arranca una consola de comandos interactiva en terminal que procesa las sentencias SQL con salida de tablas alineadas en formato ASCII premium:
```powershell
java -cp bin com.dbmotor.Main -c
```

---

## Pruebas Unitarias e Integradas

El proyecto cuenta con dos suites de verificación automatizada:

1. **Pruebas Estructurales de árbol AVL (`ArbolAVLTest`)**:
   Valida el balance estricto, rotaciones e inserción/eliminación masiva (más de 1,000 elementos):
   ```powershell
   java -cp bin com.dbmotor.core.ArbolAVLTest
   ```

2. **Pruebas Integradas de Base de Datos (`DatabaseTest`)**:
   Valida el flujo CRUD de tablas, integridad de tipos, protección de la PK y la atomicidad transaccional con reversión (rollback) en memoria ante fallos en disco:
   ```powershell
   java -cp bin com.dbmotor.core.DatabaseTest
   ```

---

##  Lenguaje de Consultas Soportado

El motor parsea de forma segura sentencias SQL-like, aislando comillas y caracteres especiales en cadenas de texto:

| Operación | Sentencia Ejemplo | Descripción / Complejidad |
| :--- | :--- | :--- |
| **Crear Tabla** | `CREATE TABLE alumnos (id INT PK, nombre TEXT, nota REAL);` | Define el esquema en memoria y crea el archivo CSV físico. |
| **Insertar** | `INSERT INTO alumnos VALUES (1, 'Alice', 4.5);` | Valida tipos de datos, unicidad de PK e inserta en el Árbol AVL. |
| **Buscar (Exacto)** | `SELECT * FROM alumnos WHERE id = 1;` | Búsqueda indexada sobre el árbol AVL. Complejidad: **$\mathcal{O}(\log n)$**. |
| **Buscar (Rango)** | `SELECT * FROM alumnos WHERE id BETWEEN 1 AND 5;` | Recorrido inorden acotado eficiente sobre el AVL. Complejidad: **$\mathcal{O}(\log n + m)$**. |
| **Buscar (Lineal)** | `SELECT * FROM alumnos WHERE nombre = 'Alice';` | Escaneo secuencial sobre campos no indexados. Complejidad: **$\mathcal{O}(n)$**. |
| **Actualizar** | `UPDATE alumnos SET nota = 4.8 WHERE id = 1;` | Modifica en memoria y persiste. Revierte cambios en memoria si falla el disco. |
| **Eliminar** | `DELETE FROM alumnos WHERE id = 1;` | Elimina del Árbol AVL y actualiza persistencia física. |
| **Borrar Tabla** | `DROP TABLE alumnos;` | Borra el archivo físico del disco y la elimina del catálogo. |
| **Esquema** | `DESCRIBE alumnos;` | Muestra las columnas, tipos y clave primaria. |
| **Catálogo** | `SHOW TABLES;` | Lista todas las tablas registradas y sus volúmenes de datos. |

---

## Arquitectura del Sistema

* **`com.dbmotor.core`**: Contiene la implementación del `ArbolAVL` auto-balanceado y sus nodos de almacenamiento genéricos, además de las suites de prueba.
* **`com.dbmotor.model`**: Define las entidades relacionales básicas (`BaseDatos`, `Tabla`, `Registro` y la enumeración de validación de tipos `TipoDato`).
* **`com.dbmotor.storage`**: `GestorPersistencia` se encarga de leer y escribir físicamente las tablas a archivos CSV en el directorio de datos. Utiliza una estrategia atómica de archivos temporales (`.tmp`) y renombrado atómico a nivel de S.O.
* **`com.dbmotor.parser`**: El `ParserSQL` traduce texto en operaciones lógicas del motor. `InterpreteREPL` da formato a las salidas en tablas ASCII.
* **`com.dbmotor.gui`**: Módulo Swing que gestiona el renderizado de la ventana principal y la topología geométrica del árbol AVL.

## UML
<img width="3236" height="2568" alt="UMLFinalCiencias" src="https://github.com/user-attachments/assets/ac9ce809-4281-4403-9c66-e4d3ddbb83e8" />
<img width="2820" height="2408" alt="UMLciencias1 1" src="https://github.com/user-attachments/assets/6faaf08c-9c3c-4276-9cc4-dbc398d4f67b" />


