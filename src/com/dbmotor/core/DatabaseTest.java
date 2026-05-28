package com.dbmotor.core;

import com.dbmotor.model.BaseDatos;
import com.dbmotor.model.Registro;
import com.dbmotor.model.Tabla;
import com.dbmotor.model.TipoDato;
import com.dbmotor.parser.ParserSQL;
import com.dbmotor.parser.ResultadoQuery;
import com.dbmotor.storage.GestorPersistencia;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;

//Suite de pruebas integradas para el motor de base de datos.
//Verifica creación, operaciones CRUD, validación de tipos, inmutabilidad de la PK
// y la consistencia transaccional (Rollback) en memoria ante fallas en disco.

public class DatabaseTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  INICIANDO SUITE DE PRUEBAS: MOTOR DE BASE DE DATOS ");
        System.out.println("==================================================");

        String tempDir = System.getProperty("user.dir") + File.separator + "test_data";

        try {
            // Limpiar datos previos de prueba
            eliminarDirectorio(new File(tempDir));

            BaseDatos db = new BaseDatos();
            GestorPersistencia pers = new GestorPersistencia(tempDir);
            ParserSQL parser = new ParserSQL(db, pers);

            // 1. Probar CREATE TABLE
            System.out.print(" Probando CREATE TABLE... ");
            parser.ejecutar("CREATE TABLE estudiantes (id INT PK, nombre TEXT, promedio REAL, activo BOOLEAN);");
            if (!db.existeTabla("estudiantes")) {
                throw new Exception("Error: La tabla 'estudiantes' no fue creada en memoria.");
            }
            if (!Files.exists(Paths.get(tempDir, "estudiantes.csv"))) {
                throw new Exception("Error: El archivo físico de 'estudiantes' no fue creado.");
            }
            System.out.println("¡ÉXITO!");

            // 2. Probar INSERT INTO
            System.out.print("Probando INSERT INTO... ");
            parser.ejecutar("INSERT INTO estudiantes VALUES (1, 'Carlos Perez', 4.5, true);");
            parser.ejecutar("INSERT INTO estudiantes VALUES (2, 'Maria Gomez', 3.8, false);");
            parser.ejecutar("INSERT INTO estudiantes VALUES (3, 'Pedro Rodriguez', 4.0, true);");

            Tabla tabla = db.obtenerTabla("estudiantes");
            if (tabla.obtenerTodos().size() != 3) {
                throw new Exception("Error: Se esperaban 3 registros en la tabla.");
            }
            System.out.println("Sirve muejejejjeje");

            // 3. Probar SELECT (Exacto por PK y secuencial)
            System.out.print("Probando SELECT exacto e inorden... ");
            ResultadoQuery resSelectPk = parser.ejecutar("SELECT * FROM estudiantes WHERE id = 2;");
            if (resSelectPk.getFilas().size() != 1
                    || !resSelectPk.getFilas().get(0).get("nombre").equals("Maria Gomez")) {
                throw new Exception("Error: Falló búsqueda exacta por clave primaria.");
            }

            ResultadoQuery resSelectNombre = parser
                    .ejecutar("SELECT * FROM estudiantes WHERE nombre = 'Pedro Rodriguez';");
            if (resSelectNombre.getFilas().size() != 1 || !resSelectNombre.getFilas().get(0).get("id").equals(3)) {
                throw new Exception("Error: Falló búsqueda secuencial por campo de texto.");
            }
            System.out.println("También sirve!! ");

            // 4. Probar UPDATE (Caso normal)
            System.out.print("Probando UPDATE normal... ");
            parser.ejecutar("UPDATE estudiantes SET promedio = 4.8, activo = false WHERE id = 1;");
            Registro carlos = tabla.buscar(1);
            if (!carlos.get("promedio").equals(4.8) || !carlos.get("activo").equals(false)) {
                throw new Exception("Error: Los campos no fueron actualizados correctamente en memoria.");
            }
            // Verificar persistencia al recargar
            Tabla tablaRecargada = pers.cargarTablaDesdeArchivo(new File(tempDir, "estudiantes.csv"));
            Registro carlosFisico = tablaRecargada.buscar(1);
            if (!carlosFisico.get("promedio").equals(4.8) || !carlosFisico.get("activo").equals(false)) {
                throw new Exception("Error: Las actualizaciones no fueron persistidas a disco.");
            }
            System.out.println("Sirve x3");

            // 5. Probar UPDATE (Caso de error e invarianza de PK)
            System.out.print("-> Probando restricciones de UPDATE (PK y tipos)... ");
            try {
                parser.ejecutar("UPDATE estudiantes SET id = 10 WHERE id = 1;");
                throw new Exception("Error: Se permitió actualizar la clave primaria.");
            } catch (IllegalArgumentException e) {
                // Éxito esperado
            }

            try {
                parser.ejecutar("UPDATE estudiantes SET promedio = 'no-es-un-numero' WHERE id = 1;");
                throw new Exception("Error: Se permitió asignar un valor de tipo incorrecto.");
            } catch (IllegalArgumentException e) {
                // Éxito esperado
            }
            System.out.println("SI SIrve x4");

            // 6. Probar UPDATE transaccional con Rollback ante fallos de disco
            System.out.print(" Probando atomicidad/rollback en UPDATE ante fallo simulado de disco... ");
            // Crear un GestorPersistencia defectuoso para forzar IOException al guardar
            GestorPersistencia persDefectuoso = new GestorPersistencia(tempDir) {
                @Override
                public synchronized void guardarTabla(Tabla t) throws IOException {
                    throw new IOException("Fallo de escritura simulado.");
                }
            };
            ParserSQL parserDefectuoso = new ParserSQL(db, persDefectuoso);

            // Intentar actualizar y verificar que se realiza el rollback en memoria
            double promedioOriginal = (Double) carlos.get("promedio");
            try {
                parserDefectuoso.ejecutar("UPDATE estudiantes SET promedio = 5.0 WHERE id = 1;");
                throw new Exception("Error: El comando no falló a pesar del error de persistencia.");
            } catch (IOException e) {
                // Verificar que el promedio sigue siendo el original
                if (!carlos.get("promedio").equals(promedioOriginal)) {
                    throw new Exception("Error: No se realizó rollback en memoria tras fallar la persistencia.");
                }
            }
            System.out.println("Sirve x5");

            // 7. Probar DROP TABLE
            System.out.print("Probando DROP TABLE... ");
            parser.ejecutar("DROP TABLE estudiantes;");
            if (db.existeTabla("estudiantes")) {
                throw new Exception("Error: La tabla 'estudiantes' sigue en memoria tras el DROP.");
            }
            if (Files.exists(Paths.get(tempDir, "estudiantes.csv"))) {
                throw new Exception("Error: El archivo físico sigue existiendo tras el DROP.");
            }
            System.out.println("Sirve final");

            System.out.println(
                    "\n ¡FASE COMPLETADA! Todas las pruebas del motor de base de datos pasaron exitosamente.\n");

        } catch (Exception e) {
            System.err.println("\nERROR en las pruebas del motor: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Limpieza
            eliminarDirectorio(new File(tempDir));
        }
    }

    private static void eliminarDirectorio(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    eliminarDirectorio(f);
                }
            }
            dir.delete();
        }
    }
}
