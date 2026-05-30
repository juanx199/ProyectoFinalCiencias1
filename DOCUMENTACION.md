# Documentación Técnica: Motor de Base de Datos AVL

Esta documentación describe en detalle el diseño, la arquitectura y la implementación del motor de base de datos relacional indexado mediante un **Árbol AVL auto-balanceado** en Java.

---

## 1. Introducción

El **Motor de Base de Datos AVL** es un gestor de bases de datos relacionales (*RDBMS-like*) en memoria con persistencia física en disco. Está diseñado en Java para demostrar la eficiencia de las estructuras de datos avanzadas en la indexación de registros relacionales.

* **¿Qué hace el gestor?**: Permite crear tablas dinámicas, definir esquemas de datos con claves primarias enteras, y realizar operaciones CRUD completas (inserción, consulta, actualización y eliminación) tanto de forma individual como masiva.
* **Tipo de base de datos**: Es un motor relacional en memoria (*In-Memory Database*). Todos los datos se cargan en la memoria RAM al iniciar para ofrecer tiempos de respuesta ultrarrápidos, y se sincronizan atómicamente en disco (archivos CSV estructurados) para garantizar la persistencia física.
* **Estructura de indexación utilizada**: Un **Árbol AVL auto-balanceado** diseñado a medida. Este árbol indexa los registros utilizando su Clave Primaria (PK) de tipo entero (`INT`), garantizando búsquedas, inserciones y eliminaciones en tiempo logarítmico $O(\log N)$.

---

## 2. Arquitectura del Sistema

El sistema está diseñado bajo un modelo modular desacoplado. A continuación se presenta la topología de interacción de sus componentes principales:

<img width="511" height="553" alt="image" src="https://github.com/user-attachments/assets/39f04d97-3246-492c-8015-c437f967f2cf" />


### Componentes del Sistema

* **Interfaz de Usuario (Swing GUI / CLI REPL)**: Proporciona dos medios de interacción: una consola de comandos interactiva de terminal (`InterpreteREPL`) y un panel de control gráfico (`VentanaPrincipal`) que incluye un visor topológico del árbol AVL en tiempo real y atajos de ejecución.
* **Analizador Sintáctico (ParserSQL)**: Analiza las sentencias de texto ingresadas, valida su gramática mediante expresiones regulares (Regex) y traduce las consultas a operaciones lógicas del motor.
* **Núcleo del Motor (BaseDatos, Tabla, Registro)**:
  * `BaseDatos`: Administra el catálogo de tablas del sistema en memoria.
  * `Tabla`: Almacena el esquema de columnas y tipos, la clave primaria designada, y gestiona las operaciones invocando al índice AVL.
  * `Registro`: Representa una fila en la tabla como un mapa de pares clave-valor (`Map<String, Object>`).
* **Estructura del Índice (ArbolAVL, NodoAVL)**: Clase genérica que implementa el árbol AVL auto-balanceado para organizar los registros por clave primaria de forma ordenada.
* **Capa de Almacenamiento (GestorPersistencia)**: Serializa y deserializa el estado de las tablas del motor de memoria a archivos CSV estructurados con cabeceras de metadatos y esquemas.

---

## 3. Lenguaje de Comandos (Sintaxis y Tipos de Datos)

El motor implementa un dialecto SQL simplificado con tipos de datos básicos y soporte para operaciones complejas.

### Tipos de Datos Soportados
* `INT`: Valores enteros (utilizados obligatoriamente para claves primarias).
* `TEXT`: Cadenas de caracteres alfanuméricas.
* `REAL`: Números de coma flotante de precisión doble (`double`).
* `BOOLEAN`: Valores lógicos (`true` / `false`).

### Comandos Soportados y Sintaxis

#### CREATE TABLE (Definición de Esquema)
Crea una nueva tabla en memoria y disco. Si no se especifica una columna con la etiqueta `PK` o `PRIMARY KEY`, el sistema tomará la primera columna de tipo `INT` como la clave primaria por defecto.
```sql
CREATE TABLE estudiantes (id INT PK, nombre TEXT, promedio REAL, activo BOOLEAN);
```

#### DROP TABLE (Eliminación de Tabla)
Borra la tabla de la memoria y elimina su archivo físico `.csv` del disco.
```sql
DROP TABLE estudiantes;
```

#### INSERT INTO (Inserción de Datos)
Soporta inserciones individuales y múltiples de forma atómica.
* *Inserción Individual:*
  ```sql
  INSERT INTO estudiantes VALUES (1, 'Carlos Perez', 4.5, true);
  ```
* *Inserción Múltiple:*
  ```sql
  INSERT INTO estudiantes VALUES (2, 'Maria Gomez', 3.8, false), (3, 'Pedro Rodriguez', 4.0, true);
  ```

#### SELECT (Consulta de Registros)
Muestra los registros que cumplen ciertas condiciones. La cláusula `WHERE` admite:
* *Búsqueda exacta (Indexada en AVL en $O(\log N)$ si es por PK):*
  ```sql
  SELECT * FROM estudiantes WHERE id = 1;
  ```
* *Búsqueda secuencial (Escanéo lineal en $O(N)$ si es por columna no indexada):*
  ```sql
  SELECT * FROM estudiantes WHERE nombre = 'Maria Gomez';
  ```
* *Búsqueda por rango (Indexada en AVL en $O(\log N + m)$ si es por PK):*
  ```sql
  SELECT * FROM estudiantes WHERE id BETWEEN 1 AND 3;
  ```
* *Búsqueda múltiple indexada (Cláusula IN):*
  ```sql
  SELECT * FROM estudiantes WHERE id IN (1, 3);
  ```
* *Operadores de comparación:*
  ```sql
  SELECT * FROM estudiantes WHERE promedio >= 4.0;
  ```

#### DELETE FROM (Eliminación de Registros)
* *Borrado condicional (Soporta `=`, `>`, `<`, `>=`, `<=`, `IN`, `BETWEEN`):*
  ```sql
  DELETE FROM estudiantes WHERE id = 1;
  DELETE FROM estudiantes WHERE promedio < 3.0;
  ```
* *Borrado total (Vacía la tabla de forma física y en memoria):*
  ```sql
  DELETE FROM estudiantes;
  ```

#### UPDATE (Modificación de Registros)
Modifica valores de las columnas de un registro existente. Por seguridad, la clave primaria es inmutable.
```sql
UPDATE estudiantes SET promedio = 4.8, activo = false WHERE id = 1;
```

#### SHOW TABLES y DESCRIBE (Utilidades de Catálogo)
```sql
SHOW TABLES; -- Muestra las tablas existentes y su conteo de filas
DESCRIBE estudiantes; -- Muestra el esquema de la tabla estudiantes
```

---

## 4. Diseño del Árbol Autobalanceado (AVL)

El AVL es un árbol binario de búsqueda auto-balanceado donde la diferencia de alturas entre el subárbol izquierdo y derecho de cualquier nodo (Factor de Balanceo) se mantiene en el rango $[-1, 1]$.

### Invariantes del AVL
* Para cada nodo $N$, $Height(N) = 1 + \max(Height(N.left), Height(N.right))$.
* Factor de Balanceo: $Balance(N) = Height(N.left) - Height(N.right)$.
* Condición de balanceo: $|Balance(N)| \le 1$.

### Rotaciones para Rebalanceo
Cuando una inserción o eliminación viola el balanceo, el árbol realiza rotaciones en tiempo constante $O(1)$:

1. **Rotación Simple a la Derecha (Caso Izquierda-Izquierda - LL)**: Se aplica cuando el subárbol izquierdo es más alto y el elemento se insertó en el subárbol izquierdo de ese hijo izquierdo.
2. **Rotación Simple a la Izquierda (Caso Derecha-Derecha - RR)**: Se aplica cuando el subárbol derecho es más alto y el elemento se insertó en el subárbol derecho de ese hijo derecho.
3. **Rotación Doble Izquierda-Derecha (Caso Izquierda-Derecha - LR)**: Rotación izquierda en el hijo izquierdo, seguida de una rotación derecha en el nodo ancestro desbalanceado.
4. **Rotación Doble Derecha-Izquierda (Caso Derecha-Izquierda - RL)**: Rotación derecha en el hijo derecho, seguida de una rotación izquierda en el ancestro.

### Estrategia de Eliminación
Durante la eliminación de un nodo con 2 hijos, el motor utiliza el **Predecesor en Inorden** (el valor máximo en su subárbol izquierdo) en lugar del sucesor. Esto garantiza el reordenamiento del árbol manteniendo la consistencia de los datos históricos del motor.

---

## 5. Persistencia

El motor persiste los datos físicamente en archivos planos estructurados con formato **CSV (Comma-Separated Values)** bajo el estándar RFC-4180.

### Estructura de un Archivo de Datos (.csv)
Cada archivo se compone de una cabecera de metadatos (línea 1), cabecera de esquema (línea 2) y registros de datos en las líneas subsecuentes ordenados secuencialmente por su clave primaria (obtenida mediante un recorrido Inorder del árbol AVL).

```text
METADATA|estudiantes|id
ESQUEMA|id:INT,nombre:TEXT,promedio:REAL,activo:BOOLEAN
1,Carlos Perez,4.5,true
2,Maria Gomez,3.8,false
3,Pedro Rodriguez,4.0,true
```

### Garantía Transaccional: Escrituras Atómicas (.tmp)
Para evitar corrupción de datos en caso de un apagón repentino o falla del disco durante la escritura, el `GestorPersistencia` implementa una estrategia de **reemplazo atómico**:

1. Escribe todo el contenido de la tabla en un archivo temporal con extensión `.tmp` (ej. `estudiantes.csv.tmp`).
2. Si la escritura física termina exitosamente, se hace un reemplazo atómico a nivel de Sistema Operativo (`ATOMIC_MOVE`) sobreescribiendo el archivo final original `estudiantes.csv`.
3. Si el sistema operativo no soporta el movimiento atómico, se realiza un reemplazo estándar seguro. De este modo, si la escritura falla a mitad del proceso, el archivo `.csv` original nunca se corrompe.

---

## 6. Operaciones CRUD

A continuación se detalla cómo el motor gestiona de manera interna las operaciones principales utilizando el árbol AVL como índice:

* **CREATE**: Instancia una nueva estructura `Tabla` en memoria y genera un archivo CSV con las cabeceras de metadatos y esquema vacías en el disco.
* **INSERT**:
  1. Valida que el valor de la clave primaria no sea nulo.
  2. Valida y parsea que los tipos de datos coincidan con el esquema.
  3. Realiza la inserción en el árbol AVL. Si la clave primaria ya existe, aborta la operación arrojando una excepción de violación de integridad física.
  4. Llama a `guardarTabla()` para persistir el nuevo registro atómicamente en disco.
* **SELECT**:
  * Si busca por Clave Primaria (`WHERE id = X`): Recorre las ramas del árbol AVL buscando el nodo correspondiente. Complejidad óptima en $O(\log N)$.
  * Si busca por Rango de PK (`WHERE id BETWEEN X AND Y`): Realiza un recorrido Inorder acotado. Explora el subárbol izquierdo solo si es necesario, extrae los elementos y luego explora el derecho. Complejidad en $O(\log N + m)$, donde $m$ es el número de elementos en el rango.
  * Si busca por otra columna (`WHERE nombre = X`): Realiza un escaneo secuencial lineal recorriendo todos los nodos del árbol en $O(N)$.
* **UPDATE**:
  1. Busca el o los registros que coinciden con el criterio (búsqueda indexada si es por PK).
  2. Valida que las asignaciones coincidan con los tipos de esquema. Si se intenta modificar la Clave Primaria, arroja un error (es inmutable).
  3. **Mecanismo de Rollback:** Antes de modificar los valores, guarda un respaldo (`backup`) de los valores anteriores en memoria.
  4. Aplica los cambios a los registros en memoria e intenta persistir la tabla a disco.
  5. Si la persistencia física falla (por ejemplo, disco lleno o error de E/S), el motor atrapa la excepción, **restaura los valores originales desde el backup en memoria (Rollback)** y propaga el error hacia la interfaz. Esto asegura consistencia total.
* **DELETE**:
  1. Localiza las claves primarias de los registros a eliminar (búsqueda rápida por AVL o escaneo lineal).
  2. Remueve el nodo del árbol AVL aplicando las rotaciones necesarias para rebalancear el árbol.
  3. Guarda los cambios atómicamente en el disco.

---

## 7. Análisis de Complejidad Temporal

El uso de un Árbol AVL sobre un escaneo secuencial lineal tradicional representa una optimización matemática a medida que el conjunto de datos escala ($N \to \infty$).

| Operación | Complejidad Temporal (AVL Indexado) | Complejidad Temporal (Escaneo Lineal) | Ventaja del AVL |
| :--- | :--- | :--- | :--- |
| **Buscar por PK** | $O(\log N)$ | $O(N)$ | Altamente eficiente para millones de datos. |
| **Buscar por No-PK** | $O(N)$ | $O(N)$ | Ambos requieren recorrer toda la estructura. |
| **Buscar por Rango de PK** | $O(\log N + m)$ | $O(N)$ | Localización ultrarrápida del rango. |
| **Insertar Registro** | $O(\log N)$ | $O(1)$ / $O(N)$ *(si valida duplicado)* | Evita colisiones de PK rápidamente en $O(\log N)$. |
| **Eliminar por PK** | $O(\log N)$ | $O(N)$ | Rebalanceo y eliminación rápida. |
| **Eliminar por No-PK** | $O(N)$ | $O(N)$ | Ambos escanean toda la tabla. |
| **Actualizar por PK** | $O(\log N)$ | $O(N)$ | Ubicación y modificación en tiempo logarítmico. |

---

## 8. Datasets de Prueba y Pruebas del Sistema

El sistema dispone de un conjunto estructurado de datasets y suites de pruebas para demostrar la eficiencia, robustez y consistencia transaccional del motor.

### 8.1. Datasets de Prueba y Scripts de Carga

Para evaluar el rendimiento del motor bajo diferentes cargas de trabajo, el proyecto incluye dos conjuntos de datos físicos pre-generados en el directorio [datasets/](file:///c:/Users/JUANCA/Documents/NetBeansProjects/FinalCiencias/datasets/):

1. **Dataset Pequeño (`usuarios_pequeno.csv`)**: Contiene **50 registros** estructurados de estudiantes. Es óptimo para la depuración visual del Árbol AVL en la interfaz Swing.
2. **Dataset Mediano (`usuarios_mediano.csv`)**: Contiene **1,000 registros** estructurados. Diseñado para pruebas de carga y para medir la diferencia empírica entre las búsquedas indexadas y secuenciales.

#### Métodos de Carga de Datos:
* **Carga por Interfaz Gráfica (Swing GUI)**: El panel derecho ("Atajos y Generadores") cuenta con dos botones de acción rápida:
  * **Cargar Dataset Pequeño (50)**: Invoca al generador en memoria, construye el árbol, lo muestra gráficamente y guarda los datos en disco de forma atómica.
  * **Cargar Dataset Grande (1000+)**: Realiza la carga masiva y ejecuta un benchmark de rendimiento algorítmico automático imprimiendo los tiempos de respuesta.
* **Carga Física Automatizada**: Al iniciar el motor, la clase `GestorPersistencia` escanea el directorio de almacenamiento y carga automáticamente cualquier archivo `.csv` (reconstruyendo los índices AVL en memoria).
* **Generación Dinámica Programática**: La clase [GeneradorDatasets.java](file:///c:/Users/JUANCA/Documents/NetBeansProjects/FinalCiencias/src/com/dbmotor/utils/GeneradorDatasets.java) genera registros sintéticos consistentes respetando las restricciones de integridad y tipos mediante semillas aleatorias deterministas.

---

### 8.2. Suite de Pruebas Integradas y Unitarias

Las pruebas demuestran la viabilidad técnica y correctitud matemática del motor. Se estructuran en dos suites principales:

#### A. Pruebas Unitarias del Índice (`ArbolAVLTest.java`)
Valida la estructura del índice de datos en memoria:
* **Inserción**: Comprueba que al insertar elementos desordenados, el árbol realice las rotaciones simples y dobles necesarias, verificando la validez del balanceo en cada paso.
* **Eliminación**: Evalúa la eliminación de hojas, nodos con un hijo y nodos con dos hijos (comprobando el reemplazo correcto por el *predecesor en inorden* y re-balanceando el árbol).
* **Búsqueda**: Valida la recuperación exacta en tiempo logarítmico y búsquedas por rango.

#### B. Pruebas Integradas del Motor (`DatabaseTest.java`)
Valida las operaciones de base de datos de extremo a extremo simulando un ciclo de vida real:
1. **CREATE**: Crea una tabla `estudiantes` en memoria y genera el esquema en el disco.
2. **INSERT (Inserción)**: Inserta registros, valida tipos (ej. no permite insertar cadenas en columnas numéricas) y valida unicidad de clave primaria.
3. **SELECT (Búsqueda)**: Comprueba búsquedas indexadas rápidas sobre la clave primaria y búsquedas secuenciales lineales sobre otras columnas.
4. **UPDATE (Actualización)**: Modifica registros garantizando la inmutabilidad de la clave primaria (lanza una excepción si se intenta modificar el `id`).
5. **Garantía de Atomicidad y Rollback**: Simula un fallo de hardware de disco (lanzando una `IOException` forzada) durante un `UPDATE`. Comprueba que el motor detecta la falla y **revierte automáticamente en memoria (Rollback)** los registros a su estado original previo a la transacción.
6. **Recuperación tras Reinicio**: Valida que al deserializar archivos físicos en disco, el motor sea capaz de reconstruir exactamente el mismo estado del árbol y de los datos.
7. **DELETE (Eliminación)**: Remueve registros del AVL, re-balancea la estructura y persiste los cambios.
8. **DROP**: Elimina la tabla de memoria y remueve el archivo CSV correspondiente de la persistencia física.

---

## 9. Conclusiones y Posibles Mejoras

### Resultados 
* **Eficiencia del Balanceo:** Se comprobó mediante benchmarks empíricos que las búsquedas e inserciones en el AVL escalan en $O(\log N)$, logrando ser cientos de veces más rápidas que un escaneo lineal en datasets de más de 1,000 registros.
* **Consistencia:** El diseño atómico mediante archivos temporales `.tmp` y el rollback en memoria garantiza que el motor sea resiliente a fallos físicos de energía o espacio en disco.

### Posibles Mejoras del Sistema
* **Índices Secundarios:** Implementar árboles AVL adicionales para columnas no primarias frecuentemente consultadas (ej. indexar por `nombre`), permitiendo búsquedas rápidas en múltiples columnas.
* **Transacciones Multicomando:** Añadir soporte para iniciar bloques de transacciones explicitas (`BEGIN TRANSACTION`, `COMMIT`, `ROLLBACK`) multicomando.
* **Tipos de Datos Avanzados:** Añadir soporte para tipos de datos como fechas (`DATE`) o campos de texto de longitud variable delimitada.

---

## 10. Instrucciones de Compilación y Ejecución

El proyecto está estructurado como un proyecto estándar de Java con soporte para compilación mediante consola y **Apache Ant**.

### Requisitos previos
* Tener instalado el JDK (versión 8 o superior).
* Tener configurado `javac` y `java` en las variables de entorno de tu sistema.
* *(Opcional)* Tener instalado **Apache Ant** para usar los scripts autogenerados.

### Compilación y Ejecución con Apache Ant (Recomendado)
Desde la terminal en el directorio raíz del proyecto:

* **Compilar el proyecto:**
  ```bash
  ant compile
  ```
* **Limpiar archivos compilados previos:**
  ```bash
  ant clean
  ```
* **Ejecutar la Interfaz Gráfica (Swing GUI):**
  ```bash
  ant run
  ```
* **Ejecutar la Consola interactiva (CLI REPL):**
  ```bash
  ant run -Dargs="--console"
  ```

### Compilación y Ejecución Manual (Sin Ant)
Desde la terminal en el directorio raíz del proyecto:

* **Crear directorio de salida:**
  ```bash
  mkdir bin
  ```
* **Compilar todos los archivos fuentes Java:**
  ```bash
  javac -d bin -cp src src/com/dbmotor/Main.java
  ```
* **Ejecutar el programa en modo Interfaz Gráfica (GUI):**
  ```bash
  java -cp bin com.dbmotor.Main
  ```
* **Ejecutar el programa en modo Consola Interactiva (CLI REPL):**
  ```bash
  java -cp bin com.dbmotor.Main --console
  ```

---

