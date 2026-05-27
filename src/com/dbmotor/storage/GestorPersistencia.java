package com.dbmotor.storage;

import com.dbmotor.model.BaseDatos;
import com.dbmotor.model.Registro;
import com.dbmotor.model.Tabla;
import com.dbmotor.model.TipoDato;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Gestor encargado de serializar y deserializar físicamente las tablas en archivos CSV.
 * Proporciona atomicidad en las escrituras mediante archivos temporales (.tmp) y reemplazos a nivel de S.O.
 */
public class GestorPersistencia {
    private final String directorioDatos;

    public GestorPersistencia(String directorioDatos) {
        this.directorioDatos = directorioDatos;
        // Crear el directorio de datos si no existe
        try {
            Files.createDirectories(Paths.get(directorioDatos));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de almacenamiento: " + e.getMessage(), e);
        }
    }

    public String getDirectorioDatos() {
        return directorioDatos;
    }

    /**
     * Guarda físicamente el estado de una tabla utilizando la estrategia de reemplazo atómico (.tmp).
     */
    public synchronized void guardarTabla(Tabla tabla) throws IOException {
        String nombreArchivoFinal = tabla.getNombre().toLowerCase() + ".csv";
        Path rutaFinal = Paths.get(directorioDatos, nombreArchivoFinal);
        Path rutaTemporal = Paths.get(directorioDatos, nombreArchivoFinal + ".tmp");

        // 1. Escribir todo el contenido en el archivo temporal (.tmp)
        try (BufferedWriter writer = Files.newBufferedWriter(rutaTemporal)) {
            // Escribir cabecera de Metadatos
            // Formato: METADATA|<nombre>|<columnaPK>
            writer.write("METADATA|" + tabla.getNombre() + "|" + tabla.getClavePrimaria());
            writer.newLine();

            // Escribir cabecera de Esquema
            // Formato: ESQUEMA|columna1:TIPO,columna2:TIPO,...
            StringBuilder esquemaSb = new StringBuilder("ESQUEMA|");
            boolean primero = true;
            for (Map.Entry<String, TipoDato> col : tabla.getEsquema().entrySet()) {
                if (!primero) {
                    esquemaSb.append(",");
                }
                esquemaSb.append(col.getKey()).append(":").append(col.getValue().name());
                primero = false;
            }
            writer.write(esquemaSb.toString());
            writer.newLine();

            // Escribir los registros en orden de Clave Primaria (recorrido inorden del AVL)
            List<Registro> registros = tabla.obtenerTodos();
            for (Registro reg : registros) {
                StringBuilder registroSb = new StringBuilder();
                primero = true;
                for (String colName : tabla.getEsquema().keySet()) {
                    if (!primero) {
                        registroSb.append(",");
                    }
                    Object val = reg.get(colName);
                    registroSb.append(escaparCeldaCSV(val));
                    primero = false;
                }
                writer.write(registroSb.toString());
                writer.newLine();
            }
        }

        // 2. Realizar reemplazo atómico a nivel de sistema operativo
        try {
            Files.move(rutaTemporal, rutaFinal, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            // Si falla el ATOMIC_MOVE (algunos S.O. o sistemas de archivos en red no lo soportan),
            // intentar un reemplazo estándar seguro
            Files.move(rutaTemporal, rutaFinal, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Carga y reconstruye todas las tablas persistidas en el directorio de almacenamiento.
     */
    public synchronized void cargarBaseDatos(BaseDatos db) throws IOException {
        Path directPath = Paths.get(directorioDatos);
        if (!Files.exists(directPath)) return;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directPath, "*.csv")) {
            for (Path entry : stream) {
                try {
                    Tabla tabla = cargarTablaDesdeArchivo(entry.toFile());
                    db.registrarTabla(tabla);
                    System.out.println("-> Tabla '" + tabla.getNombre() + "' cargada con éxito (" + tabla.obtenerTodos().size() + " registros).");
                } catch (Exception e) {
                    System.err.println("❌ Error al cargar la tabla desde " + entry.getFileName() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Reconstruye una sola tabla leyendo secuencialmente su archivo físico e insertando en el árbol AVL.
     */
    public Tabla cargarTablaDesdeArchivo(File archivo) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            // 1. Leer Metadatos
            String lineaMeta = reader.readLine();
            if (lineaMeta == null || !lineaMeta.startsWith("METADATA|")) {
                throw new IOException("Cabecera de METADATA inválida en el archivo: " + archivo.getName());
            }
            String[] partesMeta = lineaMeta.split("\\|");
            String nombreTabla = partesMeta[1];
            String pkColumna = partesMeta[2];

            // 2. Leer Esquema
            String lineaEsquema = reader.readLine();
            if (lineaEsquema == null || !lineaEsquema.startsWith("ESQUEMA|")) {
                throw new IOException("Cabecera de ESQUEMA inválida en el archivo: " + archivo.getName());
            }
            String esquemaRaw = lineaEsquema.substring(8);
            LinkedHashMap<String, TipoDato> esquema = new LinkedHashMap<>();
            String[] columnasRaw = esquemaRaw.split(",");
            for (String col : columnasRaw) {
                String[] colPartes = col.split(":");
                esquema.put(colPartes[0], TipoDato.valueOf(colPartes[1]));
            }

            // 3. Instanciar la tabla
            Tabla tabla = new Tabla(nombreTabla, esquema, pkColumna);

            // 4. Leer registros y reconstruir en el árbol AVL
            String lineaRegistro;
            List<String> nombresColumnas = new ArrayList<>(esquema.keySet());
            int nLinea = 2;
            while ((lineaRegistro = reader.readLine()) != null) {
                nLinea++;
                if (lineaRegistro.trim().isEmpty()) continue;

                List<String> celdas = parsearLineaCSV(lineaRegistro);
                if (celdas.size() != nombresColumnas.size()) {
                    System.err.println("⚠️ Saltando línea corrupta " + nLinea + " en " + archivo.getName() + " (columnas esperadas: " + nombresColumnas.size() + ", encontradas: " + celdas.size() + ")");
                    continue;
                }

                Registro registro = new Registro();
                for (int i = 0; i < celdas.size(); i++) {
                    String colName = nombresColumnas.get(i);
                    TipoDato tipo = esquema.get(colName);
                    Object val = tipo.parsear(celdas.get(i));
                    registro.set(colName, val);
                }

                try {
                    tabla.insertar(registro);
                } catch (IllegalArgumentException e) {
                    System.err.println("⚠️ Violación de integridad al cargar registro en línea " + nLinea + " de " + archivo.getName() + ": " + e.getMessage());
                }
            }

            return tabla;
        }
    }

    /**
     * Escapa un objeto en formato de celda CSV según el estándar RFC-4180.
     */
    private String escaparCeldaCSV(Object obj) {
        if (obj == null) {
            return "NULL";
        }
        String s = obj.toString();
        // Si contiene comas, comillas o saltos de línea, hay que escapar
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            // Duplicar las comillas
            s = s.replace("\"", "\"\"");
            return "\"" + s + "\"";
        }
        return s;
    }

    /**
     * Tokenizador estándar RFC-4180 para parsear celdas de una línea de archivo CSV.
     */
    public static List<String> parsearLineaCSV(String linea) {
        List<String> celdas = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean dentroComillas = false;

        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);

            if (c == '"') {
                if (dentroComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    // Es una comilla doble escapada ("") -> añadir comilla simple y saltar el siguiente char
                    sb.append('"');
                    i++;
                } else {
                    // Cambiar el estado de estar dentro de comillas
                    dentroComillas = !dentroComillas;
                }
            } else if (c == ',' && !dentroComillas) {
                // Separador fuera de comillas -> finalizar celda actual
                celdas.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        // Añadir la última celda
        celdas.add(sb.toString());

        return celdas;
    }
}
