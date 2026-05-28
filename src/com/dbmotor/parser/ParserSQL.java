package com.dbmotor.parser;

import com.dbmotor.model.*;
import com.dbmotor.storage.GestorPersistencia;

import java.io.IOException;
import java.util.*;
import java.util.regex.*;

/**
 * Analizador de consultas (Parser) SQL-like simplificado.
 * Convierte comandos de texto a llamadas operacionales del motor de base de datos.
 */
public class ParserSQL {
    private final BaseDatos db;
    private final GestorPersistencia pers;

    // Patrones Regex de Comandos
    private static final Pattern PATTERN_CREATE = Pattern.compile("^CREATE\\s+TABLE\\s+(\\w+)\\s*\\(\\s*(.+)\\s*\\)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_INSERT = Pattern.compile("^INSERT\\s+INTO\\s+(\\w+)\\s+VALUES\\s*\\(\\s*(.+)\\s*\\)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SELECT = Pattern.compile("^SELECT\\s+\\*\\s+FROM\\s+(\\w+)(?:\\s+WHERE\\s+(.+))?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_DELETE = Pattern.compile("^DELETE\\s+FROM\\s+(\\w+)\\s+WHERE\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SHOW = Pattern.compile("^SHOW\\s+TABLES$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_DESCRIBE = Pattern.compile("^DESCRIBE\\s+(\\w+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_DROP = Pattern.compile("^DROP\\s+TABLE\\s+(\\w+)$", Pattern.CASE_INSENSITIVE);

    public ParserSQL(BaseDatos db, GestorPersistencia pers) {
        this.db = db;
        this.pers = pers;
    }

    /**
     * Ejecuta una sentencia SQL-like.
     * @param sql La consulta ingresada.
     * @return Un objeto de tipo ResultadoQuery que contiene filas estructuradas o mensaje de éxito.
     */
    public ResultadoQuery ejecutar(String sql) throws Exception {
        if (sql == null || sql.trim().isEmpty()) {
            return ResultadoQuery.vacio("Comando vacío.");
        }

        String comando = sql.trim().replaceAll("\\s+", " ");
        // Quitar punto y coma al final si existe
        if (comando.endsWith(";")) {
            comando = comando.substring(0, comando.length() - 1).trim();
        }

        // 1. SHOW TABLES
        if (PATTERN_SHOW.matcher(comando).matches()) {
            return ejecutarShowTables();
        }

        // 2. DESCRIBE <tabla>
        Matcher mDesc = PATTERN_DESCRIBE.matcher(comando);
        if (mDesc.matches()) {
            return ejecutarDescribe(mDesc.group(1));
        }

        // 3. CREATE TABLE
        Matcher mCreate = PATTERN_CREATE.matcher(comando);
        if (mCreate.matches()) {
            return ejecutarCreate(mCreate.group(1), mCreate.group(2));
        }

        // 4. INSERT INTO
        Matcher mInsert = PATTERN_INSERT.matcher(comando);
        if (mInsert.matches()) {
            return ejecutarInsert(mInsert.group(1), mInsert.group(2));
        }

        // 5. SELECT * FROM
        Matcher mSelect = PATTERN_SELECT.matcher(comando);
        if (mSelect.matches()) {
            return ejecutarSelect(mSelect.group(1), mSelect.group(2));
        }

        // 6. DELETE FROM
        Matcher mDelete = PATTERN_DELETE.matcher(comando);
        if (mDelete.matches()) {
            return ejecutarDelete(mDelete.group(1), mDelete.group(2));
        }

        // 7. DROP TABLE
        Matcher mDrop = PATTERN_DROP.matcher(comando);
        if (mDrop.matches()) {
            return ejecutarDrop(mDrop.group(1));
        }

        throw new IllegalArgumentException("Sintaxis no válida. Comandos admitidos: CREATE TABLE, DROP TABLE, INSERT INTO, SELECT, DELETE, SHOW TABLES, DESCRIBE.");
    }

    private ResultadoQuery ejecutarShowTables() {
        Collection<Tabla> tablas = db.obtenerTablas();
        LinkedHashMap<String, TipoDato> esquema = new LinkedHashMap<>();
        esquema.put("NombreTabla", TipoDato.TEXT);
        esquema.put("Registros", TipoDato.INT);

        List<Registro> filas = new ArrayList<>();
        for (Tabla t : tablas) {
            Registro r = new Registro();
            r.set("NombreTabla", t.getNombre());
            r.set("Registros", t.obtenerTodos().size());
            filas.add(r);
        }

        return new ResultadoQuery(esquema, filas, "Se encontraron " + filas.size() + " tablas.");
    }

    private ResultadoQuery ejecutarDescribe(String nombreTabla) {
        Tabla t = db.obtenerTabla(nombreTabla);
        if (t == null) {
            throw new IllegalArgumentException("La tabla '" + nombreTabla + "' no existe.");
        }

        LinkedHashMap<String, TipoDato> esquemaRes = new LinkedHashMap<>();
        esquemaRes.put("Columna", TipoDato.TEXT);
        esquemaRes.put("TipoDato", TipoDato.TEXT);
        esquemaRes.put("ClavePrimaria", TipoDato.TEXT);

        List<Registro> filas = new ArrayList<>();
        for (Map.Entry<String, TipoDato> col : t.getEsquema().entrySet()) {
            Registro r = new Registro();
            r.set("Columna", col.getKey());
            r.set("TipoDato", col.getValue().name());
            r.set("ClavePrimaria", col.getKey().equals(t.getClavePrimaria()) ? "PRI" : "");
            filas.add(r);
        }

        return new ResultadoQuery(esquemaRes, filas, "Esquema de la tabla '" + nombreTabla + "'.");
    }

    private ResultadoQuery ejecutarCreate(String nombreTabla, String columnasRaw) throws IOException {
        String[] columnas = columnasRaw.split(",");
        LinkedHashMap<String, TipoDato> esquema = new LinkedHashMap<>();
        String pkColumna = null;

        for (String col : columnas) {
            String[] partes = col.trim().split("\\s+");
            if (partes.length < 2) {
                throw new IllegalArgumentException("Definición de columna inválida: '" + col + "'. Debe tener formato: nombre TIPO [PK].");
            }
            
            String colName = partes[0];
            String typeStr = partes[1].toUpperCase();
            TipoDato tipo = TipoDato.valueOf(typeStr);

            esquema.put(colName, tipo);

            if (partes.length >= 3) {
                String pkIndicator = partes[2].toUpperCase();
                // Soportar "PK" o "PRIMARY" (y "KEY" si tiene 4 partes)
                if (pkIndicator.equals("PK") || pkIndicator.equals("PRIMARY")) {
                    if (pkColumna != null) {
                        throw new IllegalArgumentException("No se permiten múltiples claves primarias en la tabla.");
                    }
                    pkColumna = colName;
                }
            }
        }

        if (pkColumna == null) {
            // Por defecto, si no se especifica, la primera columna es la PK (si es INT)
            String primeraCol = esquema.keySet().iterator().next();
            if (esquema.get(primeraCol) == TipoDato.INT) {
                pkColumna = primeraCol;
            } else {
                throw new IllegalArgumentException("Debe especificar una clave primaria de tipo entero (INT PK).");
            }
        }

        Tabla t = db.crearTabla(nombreTabla, esquema, pkColumna);
        pers.guardarTabla(t); // Persistir la creación física de la tabla (con cabecera vacía)
        
        return ResultadoQuery.exito("Tabla '" + t.getNombre() + "' creada exitosamente en memoria y disco.");
    }

    private ResultadoQuery ejecutarInsert(String nombreTabla, String valoresRaw) throws IOException {
        Tabla t = db.obtenerTabla(nombreTabla);
        if (t == null) {
            throw new IllegalArgumentException("La tabla '" + nombreTabla + "' no existe.");
        }

        // Usar el tokenizador CSV para parsear valores de forma correcta (soporta comas dentro de strings)
        List<String> celdas = GestorPersistencia.parsearLineaCSV(valoresRaw);
        List<String> columnas = new ArrayList<>(t.getEsquema().keySet());

        if (celdas.size() != columnas.size()) {
            throw new IllegalArgumentException("Cantidad de valores (" + celdas.size() + ") no coincide con la cantidad de columnas de la tabla (" + columnas.size() + ").");
        }

        Registro registro = new Registro();
        for (int i = 0; i < celdas.size(); i++) {
            String colName = columnas.get(i);
            TipoDato tipo = t.getEsquema().get(colName);
            Object parsedVal = tipo.parsear(celdas.get(i));
            registro.set(colName, parsedVal);
        }

        t.insertar(registro);
        pers.guardarTabla(t); // Sincronizar y escribir atómicamente a disco

        return ResultadoQuery.exito("1 registro insertado exitosamente.");
    }

    private ResultadoQuery ejecutarSelect(String nombreTabla, String whereRaw) {
        Tabla t = db.obtenerTabla(nombreTabla);
        if (t == null) {
            throw new IllegalArgumentException("La tabla '" + nombreTabla + "' no existe.");
        }

        List<Registro> resultado = new ArrayList<>();
        String metricaBusqueda = "Escanear lineal O(N)";

        if (whereRaw == null) {
            // SELECT * FROM tabla
            resultado = t.obtenerTodos();
            metricaBusqueda = "Recorrido inorden del árbol AVL O(N)";
        } else {
            String queryTrim = whereRaw.trim();
            // Intentar matchear BETWEEN
            Matcher mBetween = Pattern.compile("^(\\w+)\\s+BETWEEN\\s+(.+)\\s+AND\\s+(.+)$", Pattern.CASE_INSENSITIVE).matcher(queryTrim);
            // Intentar matchear exacto (=)
            Matcher mExact = Pattern.compile("^(\\w+)\\s*=\\s*(.+)$", Pattern.CASE_INSENSITIVE).matcher(queryTrim);

            if (mBetween.matches()) {
                String col = mBetween.group(1);
                String val1Raw = mBetween.group(2).trim();
                String val2Raw = mBetween.group(3).trim();

                if (col.equalsIgnoreCase(t.getClavePrimaria())) {
                    // Consulta de rango en O(log N + m) usando árbol AVL
                    Integer min = Integer.parseInt(val1Raw);
                    Integer max = Integer.parseInt(val2Raw);
                    resultado = t.buscarRango(min, max);
                    metricaBusqueda = "Búsqueda por RANGO INDEXADA en AVL O(log N + m)";
                } else {
                    // Consulta secuencial O(N)
                    TipoDato tipo = t.getEsquema().get(col);
                    if (tipo == null) throw new IllegalArgumentException("La columna '" + col + "' no existe.");
                    Comparable val1 = (Comparable) tipo.parsear(val1Raw);
                    Comparable val2 = (Comparable) tipo.parsear(val2Raw);

                    for (Registro r : t.obtenerTodos()) {
                        Comparable valReg = (Comparable) r.get(col);
                        if (valReg != null && valReg.compareTo(val1) >= 0 && valReg.compareTo(val2) <= 0) {
                            resultado.add(r);
                        }
                    }
                }
            } else if (mExact.matches()) {
                String col = mExact.group(1);
                String valRaw = mExact.group(2).trim();

                if (col.equalsIgnoreCase(t.getClavePrimaria())) {
                    // Consulta exacta O(log N) usando árbol AVL
                    Integer pkVal = Integer.parseInt(valRaw);
                    Registro r = t.buscar(pkVal);
                    if (r != null) {
                        resultado.add(r);
                    }
                    metricaBusqueda = "Búsqueda EXACTA INDEXADA en AVL O(log N)";
                } else {
                    // Consulta secuencial O(N)
                    TipoDato tipo = t.getEsquema().get(col);
                    if (tipo == null) throw new IllegalArgumentException("La columna '" + col + "' no existe.");
                    Object targetVal = tipo.parsear(valRaw);

                    for (Registro r : t.obtenerTodos()) {
                        Object valReg = r.get(col);
                        if (valReg != null && valReg.equals(targetVal)) {
                            resultado.add(r);
                        }
                    }
                }
            } else {
                throw new IllegalArgumentException("Cláusula WHERE no soportada. Use 'col = val' o 'col BETWEEN val1 AND val2'.");
            }
        }

        ResultadoQuery res = new ResultadoQuery(t.getEsquema(), resultado, "Se encontraron " + resultado.size() + " registros.");
        res.setMetricaRendimiento(metricaBusqueda);
        return res;
    }

    private ResultadoQuery ejecutarDelete(String nombreTabla, String whereRaw) throws IOException {
        Tabla t = db.obtenerTabla(nombreTabla);
        if (t == null) {
            throw new IllegalArgumentException("La tabla '" + nombreTabla + "' no existe.");
        }

        String queryTrim = whereRaw.trim();
        Matcher mExact = Pattern.compile("^(\\w+)\\s*=\\s*(.+)$", Pattern.CASE_INSENSITIVE).matcher(queryTrim);

        if (!mExact.matches()) {
            throw new IllegalArgumentException("DELETE solo soporta comparación exacta '=' en el WHERE (ej: WHERE id = 5).");
        }

        String col = mExact.group(1);
        String valRaw = mExact.group(2).trim();
        int eliminados = 0;

        if (col.equalsIgnoreCase(t.getClavePrimaria())) {
            Integer pkVal = Integer.parseInt(valRaw);
            if (t.eliminar(pkVal)) {
                eliminados = 1;
            }
        } else {
            // Eliminar registros secuencialmente
            TipoDato tipo = t.getEsquema().get(col);
            if (tipo == null) throw new IllegalArgumentException("La columna '" + col + "' no existe.");
            Object targetVal = tipo.parsear(valRaw);

            List<Integer> keysToDelete = new ArrayList<>();
            for (Registro r : t.obtenerTodos()) {
                Object valReg = r.get(col);
                if (valReg != null && valReg.equals(targetVal)) {
                    keysToDelete.add((Integer) r.get(t.getClavePrimaria()));
                }
            }

            for (Integer key : keysToDelete) {
                t.eliminar(key);
                eliminados++;
            }
        }

        if (eliminados > 0) {
            pers.guardarTabla(t); // Sincronizar cambio atómicamente a disco
        }

        return ResultadoQuery.exito("Eliminados " + eliminados + " registros correctamente.");
    }

    private ResultadoQuery ejecutarDrop(String nombreTabla) throws Exception {
        if (!db.existeTabla(nombreTabla)) {
            throw new IllegalArgumentException("La tabla '" + nombreTabla + "' no existe.");
        }

        // 1. Borrar archivo de disco primero para asegurar la atomicidad transaccional
        try {
            pers.eliminarTablaFisica(nombreTabla);
        } catch (IOException e) {
            throw new IOException("Error crítico al intentar borrar el archivo físico de la tabla '" + nombreTabla + "': " + e.getMessage(), e);
        }

        // 2. Si el borrado en disco tiene éxito, remover de memoria
        db.eliminarTabla(nombreTabla);

        return ResultadoQuery.exito("Tabla '" + nombreTabla + "' borrada exitosamente de memoria y disco.");
    }
}
