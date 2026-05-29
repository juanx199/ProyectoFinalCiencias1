package com.dbmotor.parser;

import com.dbmotor.model.*;
import com.dbmotor.storage.GestorPersistencia;

import java.io.IOException;
import java.util.*;
import java.util.regex.*;

public class ParserSQL {
    private final BaseDatos db;
    private final GestorPersistencia pers;

    // Patrones Regex de Comandos
    private static final Pattern PATTERN_CREATE = Pattern.compile("^CREATE\\s+TABLE\\s+(\\w+)\\s*\\(\\s*(.+)\\s*\\)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_INSERT = Pattern
            .compile("^INSERT\\s+INTO\\s+(\\w+)\\s+VALUES\\s*\\(\\s*(.+)\\s*\\)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_INSERT_MULTI = Pattern
            .compile("^INSERT\\s+INTO\\s+(\\w+)\\s+VALUES\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SELECT = Pattern
            .compile("^SELECT\\s+\\*\\s+FROM\\s+(\\w+)(?:\\s+WHERE\\s+(.+))?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_DELETE = Pattern.compile("^DELETE\\s+FROM\\s+(\\w+)\\s+WHERE\\s+(.+)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_DELETE_ALL = Pattern.compile("^DELETE\\s+FROM\\s+(\\w+)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SHOW = Pattern.compile("^SHOW\\s+TABLES$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_DESCRIBE = Pattern.compile("^DESCRIBE\\s+(\\w+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_DROP = Pattern.compile("^DROP\\s+TABLE\\s+(\\w+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_UPDATE = Pattern.compile("^UPDATE\\s+(\\w+)\\s+SET\\s+(.+)$", Pattern.CASE_INSENSITIVE);

    public ParserSQL(BaseDatos db, GestorPersistencia pers) {
        this.db = db;
        this.pers = pers;
    }

    public ResultadoQuery ejecutar(String sql) throws Exception {
        if (sql == null || sql.trim().isEmpty()) {
            return ResultadoQuery.vacio("Comando vacío.");
        }

        String cleanedSql = sql.trim();
        if (cleanedSql.toLowerCase().startsWith("sql>")) {
            cleanedSql = cleanedSql.substring(4).trim();
        } else if (cleanedSql.toLowerCase().startsWith("avl-db>")) {
            cleanedSql = cleanedSql.substring(7).trim();
        }

        String comando = cleanedSql.replaceAll("\\s+", " ");
        if (comando.endsWith(";")) {
            comando = comando.substring(0, comando.length() - 1).trim();
        }

        // UPDATE
        if (comando.toUpperCase().startsWith("UPDATE ")) {
            return ejecutarUpdate(comando);
        }

        // SHOW TABLES
        if (PATTERN_SHOW.matcher(comando).matches()) {
            return ejecutarShowTables();
        }

        // DESCRIBE
        Matcher mDesc = PATTERN_DESCRIBE.matcher(comando);
        if (mDesc.matches()) {
            return ejecutarDescribe(mDesc.group(1));
        }

        // CREATE TABLE
        Matcher mCreate = PATTERN_CREATE.matcher(comando);
        if (mCreate.matches()) {
            return ejecutarCreate(mCreate.group(1), mCreate.group(2));
        }

        // INSERT INTO (soporta múltiples registros)
        Matcher mInsertMulti = PATTERN_INSERT_MULTI.matcher(comando);
        if (mInsertMulti.matches()) {
            return ejecutarInsertMulti(mInsertMulti.group(1), mInsertMulti.group(2));
        }

        // SELECT
        Matcher mSelect = PATTERN_SELECT.matcher(comando);
        if (mSelect.matches()) {
            return ejecutarSelect(mSelect.group(1), mSelect.group(2));
        }

        // DELETE FROM
        Matcher mDeleteAll = PATTERN_DELETE_ALL.matcher(comando);
        if (mDeleteAll.matches()) {
            return ejecutarDeleteAll(mDeleteAll.group(1));
        }
        
        Matcher mDelete = PATTERN_DELETE.matcher(comando);
        if (mDelete.matches()) {
            return ejecutarDelete(mDelete.group(1), mDelete.group(2));
        }

        // DROP TABLE
        Matcher mDrop = PATTERN_DROP.matcher(comando);
        if (mDrop.matches()) {
            return ejecutarDrop(mDrop.group(1));
        }

        throw new IllegalArgumentException(
                "Sintaxis no válida. Comandos admitidos: CREATE TABLE, DROP TABLE, INSERT INTO, SELECT, DELETE, UPDATE, SHOW TABLES, DESCRIBE.");
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
                throw new IllegalArgumentException(
                        "Definición de columna inválida: '" + col + "'. Debe tener formato: nombre TIPO [PK].");
            }

            String colName = partes[0];
            String typeStr = partes[1].toUpperCase();
            TipoDato tipo = TipoDato.valueOf(typeStr);

            esquema.put(colName, tipo);

            if (partes.length >= 3) {
                String pkIndicator = partes[2].toUpperCase();
                if (pkIndicator.equals("PK") || pkIndicator.equals("PRIMARY")) {
                    if (pkColumna != null) {
                        throw new IllegalArgumentException("No se permiten múltiples claves primarias en la tabla.");
                    }
                    pkColumna = colName;
                }
            }
        }

        if (pkColumna == null) {
            String primeraCol = esquema.keySet().iterator().next();
            if (esquema.get(primeraCol) == TipoDato.INT) {
                pkColumna = primeraCol;
            } else {
                throw new IllegalArgumentException("Debe especificar una clave primaria de tipo entero (INT PK).");
            }
        }

        Tabla t = db.crearTabla(nombreTabla, esquema, pkColumna);
        pers.guardarTabla(t);

        return ResultadoQuery.exito("Tabla '" + t.getNombre() + "' creada exitosamente en memoria y disco.");
    }

    private ResultadoQuery ejecutarInsert(String nombreTabla, String valoresRaw) throws IOException {
        Tabla t = db.obtenerTabla(nombreTabla);
        if (t == null) {
            throw new IllegalArgumentException("La tabla '" + nombreTabla + "' no existe.");
        }

        List<String> celdas = GestorPersistencia.parsearLineaCSV(valoresRaw);
        List<String> columnas = new ArrayList<>(t.getEsquema().keySet());

        if (celdas.size() != columnas.size()) {
            throw new IllegalArgumentException("Cantidad de valores (" + celdas.size()
                    + ") no coincide con la cantidad de columnas de la tabla (" + columnas.size() + ").");
        }

        Registro registro = new Registro();
        for (int i = 0; i < celdas.size(); i++) {
            String colName = columnas.get(i);
            TipoDato tipo = t.getEsquema().get(colName);
            Object parsedVal = tipo.parsear(celdas.get(i));
            registro.set(colName, parsedVal);
        }

        t.insertar(registro);
        pers.guardarTabla(t);

        return ResultadoQuery.exito("1 registro insertado exitosamente.");
    }

    private ResultadoQuery ejecutarInsertMulti(String nombreTabla, String valoresRaw) throws IOException {
        Tabla t = db.obtenerTabla(nombreTabla);
        if (t == null) {
            throw new IllegalArgumentException("La tabla '" + nombreTabla + "' no existe.");
        }

        List<String> gruposValores = parsearGruposValores(valoresRaw);
        List<String> columnas = new ArrayList<>(t.getEsquema().keySet());
        
        List<Registro> registrosInsertados = new ArrayList<>();
        int insertados = 0;
        int errores = 0;
        StringBuilder erroresMsg = new StringBuilder();

        for (String grupo : gruposValores) {
            try {
                List<String> celdas = GestorPersistencia.parsearLineaCSV(grupo);
                
                if (celdas.size() != columnas.size()) {
                    errores++;
                    erroresMsg.append(String.format("Grupo '%s' tiene %d columnas (esperaba %d)\n", 
                        grupo, celdas.size(), columnas.size()));
                    continue;
                }

                Registro registro = new Registro();
                for (int i = 0; i < celdas.size(); i++) {
                    String colName = columnas.get(i);
                    TipoDato tipo = t.getEsquema().get(colName);
                    Object parsedVal = tipo.parsear(celdas.get(i));
                    registro.set(colName, parsedVal);
                }

                t.insertar(registro);
                registrosInsertados.add(registro);
                insertados++;
                
            } catch (Exception e) {
                errores++;
                erroresMsg.append(String.format("Error en grupo '%s': %s\n", grupo, e.getMessage()));
            }
        }

        if (insertados > 0) {
            pers.guardarTabla(t);
        }

        String mensaje = String.format("Insertados %d registros exitosamente.", insertados);
        if (errores > 0) {
            mensaje += String.format(" (%d fallaron)\nDetalles:\n%s", errores, erroresMsg.toString());
        }
        
        return ResultadoQuery.exito(mensaje);
    }

    private ResultadoQuery ejecutarSelect(String nombreTabla, String whereRaw) {
        Tabla t = db.obtenerTabla(nombreTabla);
        if (t == null) {
            throw new IllegalArgumentException("La tabla '" + nombreTabla + "' no existe.");
        }

        List<Registro> resultado = new ArrayList<>();
        String metricaBusqueda = "Escanear lineal O(N)";

        if (whereRaw == null) {
            resultado = t.obtenerTodos();
            metricaBusqueda = "Recorrido inorden del árbol AVL O(N)";
        } else {
            String queryTrim = whereRaw.trim();
            
            // BETWEEN
            Matcher mBetween = Pattern.compile("^(\\w+)\\s+BETWEEN\\s+(.+)\\s+AND\\s+(.+)$", Pattern.CASE_INSENSITIVE)
                    .matcher(queryTrim);
            // IN
            Matcher mIn = Pattern.compile("^(\\w+)\\s+IN\\s*\\(\\s*(.+)\\s*\\)$", Pattern.CASE_INSENSITIVE)
                    .matcher(queryTrim);
            // Operadores de comparación
            Matcher mComparacion = Pattern.compile("^(\\w+)\\s*(=|>|<|>=|<=)\\s*(.+)$", Pattern.CASE_INSENSITIVE)
                    .matcher(queryTrim);

            if (mBetween.matches()) {
                String col = mBetween.group(1);
                String val1Raw = mBetween.group(2).trim();
                String val2Raw = mBetween.group(3).trim();

                if (col.equalsIgnoreCase(t.getClavePrimaria())) {
                    Integer min = Integer.parseInt(val1Raw);
                    Integer max = Integer.parseInt(val2Raw);
                    resultado = t.buscarRango(min, max);
                    metricaBusqueda = "Búsqueda por RANGO INDEXADA en AVL O(log N + m)";
                } else {
                    TipoDato tipo = t.getEsquema().get(col);
                    if (tipo == null)
                        throw new IllegalArgumentException("La columna '" + col + "' no existe.");
                    Comparable val1 = (Comparable) tipo.parsear(val1Raw);
                    Comparable val2 = (Comparable) tipo.parsear(val2Raw);

                    for (Registro r : t.obtenerTodos()) {
                        Comparable valReg = (Comparable) r.get(col);
                        if (valReg != null && valReg.compareTo(val1) >= 0 && valReg.compareTo(val2) <= 0) {
                            resultado.add(r);
                        }
                    }
                }
            } else if (mIn.matches()) {
                String col = mIn.group(1);
                String valoresList = mIn.group(2);
                
                List<String> valoresParseados = parsearListaValores(valoresList);
                Set<Object> valoresSet = new HashSet<>();
                
                TipoDato tipo = t.getEsquema().get(col);
                if (tipo == null)
                    throw new IllegalArgumentException("La columna '" + col + "' no existe.");
                
                for (String valRaw : valoresParseados) {
                    valoresSet.add(tipo.parsear(quitarComillas(valRaw)));
                }
                
                if (col.equalsIgnoreCase(t.getClavePrimaria())) {
                    for (Object val : valoresSet) {
                        Registro r = t.buscar((Integer) val);
                        if (r != null) {
                            resultado.add(r);
                        }
                    }
                    metricaBusqueda = "Búsqueda MÚLTIPLE INDEXADA en AVL O(k * log N)";
                } else {
                    for (Registro r : t.obtenerTodos()) {
                        Object valReg = r.get(col);
                        if (valReg != null && valoresSet.contains(valReg)) {
                            resultado.add(r);
                        }
                    }
                    metricaBusqueda = "Búsqueda MÚLTIPLE secuencial O(N * k)";
                }
            } else if (mComparacion.matches()) {
                String col = mComparacion.group(1);
                String operador = mComparacion.group(2);
                String valRaw = mComparacion.group(3).trim();

                if (col.equalsIgnoreCase(t.getClavePrimaria()) && operador.equals("=")) {
                    Integer pkVal = Integer.parseInt(valRaw);
                    Registro r = t.buscar(pkVal);
                    if (r != null) {
                        resultado.add(r);
                    }
                    metricaBusqueda = "Búsqueda EXACTA INDEXADA en AVL O(log N)";
                } else {
                    TipoDato tipo = t.getEsquema().get(col);
                    if (tipo == null)
                        throw new IllegalArgumentException("La columna '" + col + "' no existe.");
                    Object valorComparacion = tipo.parsear(valRaw);

                    for (Registro r : t.obtenerTodos()) {
                        Object valReg = r.get(col);
                        if (valReg == null) continue;
                        
                        boolean coincide = false;
                        if (tipo == TipoDato.INT) {
                            int a = (Integer) valReg;
                            int b = (Integer) valorComparacion;
                            switch (operador) {
                                case "=": coincide = a == b; break;
                                case ">": coincide = a > b; break;
                                case "<": coincide = a < b; break;
                                case ">=": coincide = a >= b; break;
                                case "<=": coincide = a <= b; break;
                            }
                        } else if (tipo == TipoDato.REAL) {
                            double a = (Double) valReg;
                            double b = (Double) valorComparacion;
                            switch (operador) {
                                case "=": coincide = Math.abs(a - b) < 0.0001; break;
                                case ">": coincide = a > b; break;
                                case "<": coincide = a < b; break;
                                case ">=": coincide = a >= b; break;
                                case "<=": coincide = a <= b; break;
                            }
                        } else if (tipo == TipoDato.TEXT) {
                            String a = (String) valReg;
                            String b = (String) valorComparacion;
                            if (operador.equals("=")) {
                                coincide = a.equals(b);
                            } else if (operador.equals(">")) {
                                coincide = a.compareTo(b) > 0;
                            } else if (operador.equals("<")) {
                                coincide = a.compareTo(b) < 0;
                            }
                        } else if (tipo == TipoDato.BOOLEAN) {
                            if (operador.equals("=")) {
                                coincide = valReg.equals(valorComparacion);
                            }
                        }
                        
                        if (coincide) {
                            resultado.add(r);
                        }
                    }
                    metricaBusqueda = "Búsqueda secuencial O(N)";
                }
            } else {
                throw new IllegalArgumentException(
                        "Cláusula WHERE no soportada. Use 'col = val', 'col > val', 'col < val', 'col >= val', 'col <= val', 'col BETWEEN val1 AND val2' o 'col IN (val1, val2, ...)'.");
            }
        }

        ResultadoQuery res = new ResultadoQuery(t.getEsquema(), resultado,
                "Se encontraron " + resultado.size() + " registros.");
        res.setMetricaRendimiento(metricaBusqueda);
        return res;
    }

    private ResultadoQuery ejecutarDelete(String nombreTabla, String whereRaw) throws IOException {
    Tabla t = db.obtenerTabla(nombreTabla);
    if (t == null) {
        throw new IllegalArgumentException("La tabla '" + nombreTabla + "' no existe.");
    }

    String queryTrim = whereRaw.trim();
    
    // Primero, intentar identificar el patrón de la condición
    // Patrón para IN
    Pattern patternIn = Pattern.compile("^(\\w+)\\s+IN\\s*\\((.+)\\)$", Pattern.CASE_INSENSITIVE);
    Matcher mIn = patternIn.matcher(queryTrim);
    
    // Patrón para BETWEEN
    Pattern patternBetween = Pattern.compile("^(\\w+)\\s+BETWEEN\\s+(.+?)\\s+AND\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    Matcher mBetween = patternBetween.matcher(queryTrim);
    
    // Patrón para comparación simple (maneja espacios correctamente)
    // IMPORTANTE: Los operadores de dos caracteres (>=, <=) deben ir ANTES que los de un caracter
    Pattern patternComparacion = Pattern.compile("^(\\w+)\\s*(>=|<=|>|<|=)\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    Matcher mComparacion = patternComparacion.matcher(queryTrim);

    List<Integer> keysToDelete = new ArrayList<>();

    if (mIn.matches()) {
        String columna = mIn.group(1);
        String valoresRaw = mIn.group(2);
        String[] valoresArray = valoresRaw.split(",");
        
        for (String val : valoresArray) {
            String valorLimpio = val.trim().replaceAll("^['\"]|['\"]$", "");
            keysToDelete.add(Integer.parseInt(valorLimpio));
        }
        
    } else if (mBetween.matches()) {
        String columna = mBetween.group(1);
        String val1 = mBetween.group(2).trim();
        String val2 = mBetween.group(3).trim();
        
        // Quitar comillas si existen
        val1 = val1.replaceAll("^['\"]|['\"]$", "");
        val2 = val2.replaceAll("^['\"]|['\"]$", "");
        
        int min = Integer.parseInt(val1);
        int max = Integer.parseInt(val2);
        
        List<Registro> registros = t.buscarRango(min, max);
        for (Registro r : registros) {
            keysToDelete.add((Integer) r.get(t.getClavePrimaria()));
        }
        
    } else if (mComparacion.matches()) {
        String columna = mComparacion.group(1);
        String operador = mComparacion.group(2);
        String valorRaw = mComparacion.group(3).trim();
        
        // Quitar comillas del valor si existen
        String valorLimpio = valorRaw.replaceAll("^['\"]|['\"]$", "");
        
        System.out.println("DEBUG - Columna: '" + columna + "'");
        System.out.println("DEBUG - Operador: '" + operador + "'");
        System.out.println("DEBUG - Valor raw: '" + valorRaw + "'");
        System.out.println("DEBUG - Valor limpio: '" + valorLimpio + "'");
        
        // Verificar si la columna existe
        if (!t.getEsquema().containsKey(columna)) {
            throw new IllegalArgumentException("La columna '" + columna + "' no existe en la tabla. Columnas disponibles: " + t.getEsquema().keySet());
        }
        
        TipoDato tipo = t.getEsquema().get(columna);
        
        // Para cada registro en la tabla, verificar si cumple la condición
        for (Registro r : t.obtenerTodos()) {
            Object valorRegistro = r.get(columna);
            if (valorRegistro == null) continue;
            
            boolean cumple = false;
            
            if (tipo == TipoDato.INT) {
                int a = (Integer) valorRegistro;
                int b = Integer.parseInt(valorLimpio);
                switch (operador) {
                    case "=": cumple = a == b; break;
                    case ">": cumple = a > b; break;
                    case "<": cumple = a < b; break;
                    case ">=": cumple = a >= b; break;
                    case "<=": cumple = a <= b; break;
                }
            } else if (tipo == TipoDato.REAL) {
                double a = (Double) valorRegistro;
                double b = Double.parseDouble(valorLimpio);
                switch (operador) {
                    case "=": cumple = Math.abs(a - b) < 0.0001; break;
                    case ">": cumple = a > b; break;
                    case "<": cumple = a < b; break;
                    case ">=": cumple = a >= b; break;
                    case "<=": cumple = a <= b; break;
                }
            } else if (tipo == TipoDato.TEXT) {
                String a = (String) valorRegistro;
                String b = valorLimpio;
                switch (operador) {
                    case "=": cumple = a.equals(b); break;
                    case ">": cumple = a.compareTo(b) > 0; break;
                    case "<": cumple = a.compareTo(b) < 0; break;
                    case ">=": cumple = a.compareTo(b) >= 0; break;
                    case "<=": cumple = a.compareTo(b) <= 0; break;
                }
            } else if (tipo == TipoDato.BOOLEAN) {
                if (operador.equals("=")) {
                    boolean a = (Boolean) valorRegistro;
                    boolean b = Boolean.parseBoolean(valorLimpio);
                    cumple = a == b;
                }
            }
            
            if (cumple) {
                keysToDelete.add((Integer) r.get(t.getClavePrimaria()));
            }
        }
    } else {
        throw new IllegalArgumentException(
            "Formato WHERE no válido.\n" +
            "Ejemplos:\n" +
            "  DELETE FROM tabla WHERE id = 10\n" +
            "  DELETE FROM tabla WHERE id >= 400\n" +
            "  DELETE FROM tabla WHERE nombre = 'Juan'\n" +
            "  DELETE FROM tabla WHERE id IN (1,2,3)\n" +
            "  DELETE FROM tabla WHERE id BETWEEN 10 AND 20"
        );
    }

    // Eliminar los registros encontrados
    int eliminados = 0;
    for (Integer key : keysToDelete) {
        if (t.eliminar(key)) {
            eliminados++;
        }
    }

    if (eliminados > 0) {
        pers.guardarTabla(t);
    }

    return ResultadoQuery.exito("Eliminados " + eliminados + " registros correctamente.");
}

    private ResultadoQuery ejecutarDeleteAll(String nombreTabla) throws IOException {
        Tabla t = db.obtenerTabla(nombreTabla);
        if (t == null) {
            throw new IllegalArgumentException("La tabla '" + nombreTabla + "' no existe.");
        }
        
        int eliminados = t.obtenerTodos().size();
        
        Tabla nuevaTabla = new Tabla(t.getNombre(), t.getEsquema(), t.getClavePrimaria());
        db.eliminarTabla(nombreTabla);
        db.registrarTabla(nuevaTabla);
        pers.guardarTabla(nuevaTabla);
        
        return ResultadoQuery.exito("Eliminados " + eliminados + " registros de la tabla '" + nombreTabla + "'.");
    }

    private ResultadoQuery ejecutarDrop(String nombreTabla) throws Exception {
        if (!db.existeTabla(nombreTabla)) {
            throw new IllegalArgumentException("La tabla '" + nombreTabla + "' no existe.");
        }

        try {
            pers.eliminarTablaFisica(nombreTabla);
        } catch (IOException e) {
            throw new IOException("Error crítico al intentar borrar el archivo físico de la tabla '" + nombreTabla
                    + "': " + e.getMessage(), e);
        }

        db.eliminarTabla(nombreTabla);

        return ResultadoQuery.exito("Tabla '" + nombreTabla + "' borrada exitosamente de memoria y disco.");
    }

      
    // --- Métodos auxiliares ---

    private List<String> parsearGruposValores(String valoresRaw) {
        List<String> grupos = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int parentesis = 0;
        boolean enComillas = false;
        
        for (int i = 0; i < valoresRaw.length(); i++) {
            char c = valoresRaw.charAt(i);
            
            if (c == '\'' && (i == 0 || valoresRaw.charAt(i-1) != '\\')) {
                enComillas = !enComillas;
                sb.append(c);
            } else if (c == '(' && !enComillas) {
                parentesis++;
                if (parentesis == 1) {
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            } else if (c == ')' && !enComillas) {
                parentesis--;
                if (parentesis == 0) {
                    grupos.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            } else if (c == ',' && parentesis == 0 && !enComillas) {
                continue;
            } else {
                sb.append(c);
            }
        }
        
        return grupos;
    }

    private List<String> parsearListaValores(String valoresList) {
        List<String> valores = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean enComillas = false;
        
        for (int i = 0; i < valoresList.length(); i++) {
            char c = valoresList.charAt(i);
            if (c == '\'' && (i == 0 || valoresList.charAt(i-1) != '\\')) {
                enComillas = !enComillas;
                sb.append(c);
            } else if (c == ',' && !enComillas) {
                valores.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) {
            valores.add(sb.toString().trim());
        }
        return valores;
    }

    private String quitarComillas(String s) {
        s = s.trim();
        if ((s.startsWith("'") && s.endsWith("'")) || (s.startsWith("\"") && s.endsWith("\""))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private int buscarWhereFueraComillas(String texto) {
    boolean enComillaSimple = false;
    boolean enComillaDoble = false;
    int len = texto.length();

    for (int i = 0; i < len; i++) {
        char c = texto.charAt(i);
        if (c == '\'' && !enComillaDoble) {
            enComillaSimple = !enComillaSimple;
        } else if (c == '"' && !enComillaSimple) {
            enComillaDoble = !enComillaDoble;
        } else if (!enComillaSimple && !enComillaDoble) {
            if (i + 5 <= len && texto.substring(i, i + 5).equalsIgnoreCase("WHERE")) {
                boolean limiteIzquierdo = (i == 0 || Character.isWhitespace(texto.charAt(i - 1)));
                boolean limiteDerecho = (i + 5 == len || Character.isWhitespace(texto.charAt(i + 5)));
                if (limiteIzquierdo && limiteDerecho) {
                    return i;
                }
            }
        }
    }
    return -1;
}

    private List<String> dividirPorComasFueraComillas(String texto) {
        List<String> partes = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean enComillas = false;
        
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '\'' && (i == 0 || texto.charAt(i-1) != '\\')) {
                enComillas = !enComillas;
                sb.append(c);
            } else if (c == ',' && !enComillas) {
                partes.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) {
            partes.add(sb.toString().trim());
        }
        return partes;
    }

    private String[] dividirAsignacion(String asignacion) {
        int idxIgual = -1;
        boolean enComillas = false;
        
        for (int i = 0; i < asignacion.length(); i++) {
            char c = asignacion.charAt(i);
            if (c == '\'' && (i == 0 || asignacion.charAt(i-1) != '\\')) {
                enComillas = !enComillas;
            } else if (c == '=' && !enComillas) {
                idxIgual = i;
                break;
            }
        }
        
        if (idxIgual == -1) {
            throw new IllegalArgumentException("Asignación inválida: " + asignacion);
        }
        
        String columna = asignacion.substring(0, idxIgual).trim();
        String valor = asignacion.substring(idxIgual + 1).trim();
        return new String[]{columna, valor};
    }
}