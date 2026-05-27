package com.dbmotor.utils;

import com.dbmotor.model.Registro;
import com.dbmotor.model.Tabla;
import com.dbmotor.model.TipoDato;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generador automatizado de conjuntos de datos y herramientas de benchmarking.
 * Genera datasets pequeños para depuración y datasets medianos para probar la eficiencia logarítmica.
 */
public class GeneradorDatasets {
    private static final Random random = new Random(12345); // Semilla para consistencia

    private static final String[] NOMBRES = {
        "Juan", "Maria", "Carlos", "Sofia", "Pedro", "Ana", "Luis", "Elena", "Andres", "Clara",
        "Jose", "Laura", "Diego", "Lucia", "Manuel", "Isabel", "Javier", "Carmen", "Francisco", "Marta"
    };
    
    private static final String[] APELLIDOS = {
        "Gomez", "Rodriguez", "Fernandez", "Lopez", "Diaz", "Martinez", "Perez", "Garcia", "Sanchez", "Romero",
        "Torres", "Ruiz", "Ramirez", "Flores", "Acosta", "Benitez", "Medina", "Herrera", "Suarez", "Gimenez"
    };

    /**
     * Genera e inserta una cantidad específica de registros aleatorios válidos en la tabla.
     */
    public static void generarEInsertar(Tabla tabla, int cantidad) {
        String pkCol = tabla.getClavePrimaria();
        
        for (int i = 0; i < cantidad; i++) {
            // Generar clave primaria entera única
            int id = random.nextInt(cantidad * 20);
            while (tabla.buscar(id) != null) {
                id = random.nextInt(cantidad * 20);
            }

            Registro registro = new Registro();
            for (String colName : tabla.getEsquema().keySet()) {
                if (colName.equals(pkCol)) {
                    registro.set(colName, id);
                    continue;
                }

                TipoDato tipo = tabla.getEsquema().get(colName);
                switch (tipo) {
                    case INT:
                        registro.set(colName, random.nextInt(100));
                        break;
                    case TEXT:
                        String nombreCompleto = NOMBRES[random.nextInt(NOMBRES.length)] + " " + APELLIDOS[random.nextInt(APELLIDOS.length)];
                        registro.set(colName, nombreCompleto);
                        break;
                    case REAL:
                        double saldo = 100.0 + random.nextDouble() * 5000.0;
                        registro.set(colName, Math.round(saldo * 100.0) / 100.0);
                        break;
                    case BOOLEAN:
                        registro.set(colName, random.nextBoolean());
                        break;
                }
            }

            try {
                tabla.insertar(registro);
            } catch (IllegalArgumentException e) {
                // Clave colisionada en inserción paralela extrema, decrementar i y reintentar
                i--;
            }
        }
    }

    /**
     * Ejecuta una prueba de benchmarking para medir la eficiencia de búsqueda indexada (AVL)
     * en comparación con la búsqueda secuencial lineal.
     * Realiza 1,000 búsquedas aleatorias y devuelve un reporte en texto.
     */
    public static String ejecutarBenchmark(Tabla tabla) {
        List<Registro> todos = tabla.obtenerTodos();
        if (todos.isEmpty()) {
            return "No hay datos suficientes para ejecutar el benchmark.";
        }

        int cantidadBusquedas = 1000;
        int nRegistros = todos.size();
        
        // Seleccionar 1,000 claves que sabemos que existen para buscar
        int[] clavesABuscar = new int[cantidadBusquedas];
        for (int i = 0; i < cantidadBusquedas; i++) {
            clavesABuscar[i] = (Integer) todos.get(random.nextInt(nRegistros)).get(tabla.getClavePrimaria());
        }

        System.gc(); // Solicitar recolección de basura para limpiar mediciones de memoria

        // 1. Benchmarking de Búsqueda Indexada en Árbol AVL O(log n)
        long startAVL = System.nanoTime();
        for (int i = 0; i < cantidadBusquedas; i++) {
            Registro r = tabla.buscar(clavesABuscar[i]);
            if (r == null) {
                throw new IllegalStateException("Error crítico: Clave no encontrada en el AVL durante el benchmark.");
            }
        }
        long endAVL = System.nanoTime() - startAVL;

        // 2. Benchmarking de Búsqueda Secuencial Lineal O(N)
        long startSec = System.nanoTime();
        for (int i = 0; i < cantidadBusquedas; i++) {
            int target = clavesABuscar[i];
            Registro encontrado = null;
            // Escaneo secuencial manual
            for (Registro r : todos) {
                if (((Integer) r.get(tabla.getClavePrimaria())) == target) {
                    encontrado = r;
                    break;
                }
            }
            if (encontrado == null) {
                throw new IllegalStateException("Error crítico: Clave no encontrada en escaneo secuencial.");
            }
        }
        long endSec = System.nanoTime() - startSec;

        // 3. Generar estadísticas legibles
        double tiempoTotalAVLMilis = endAVL / 1_000_000.0;
        double tiempoTotalSecMilis = endSec / 1_000_000.0;
        
        double avgAVLNanos = (double) endAVL / cantidadBusquedas;
        double avgSecNanos = (double) endSec / cantidadBusquedas;

        double factorMejora = (double) endSec / endAVL;

        StringBuilder sb = new StringBuilder();
        sb.append("+-------------------------------------------------------------------------+\n");
        sb.append("|        BENCHMARK DE RENDIMIENTO ALGORÍTMICO: O(log N) vs O(N)           |\n");
        sb.append("+-------------------------------------------------------------------------+\n");
        sb.append(String.format("| Registros en la Tabla: %-49d|\n", nRegistros));
        sb.append(String.format("| Búsquedas Ejecutadas : %-49d|\n", cantidadBusquedas));
        sb.append("+-------------------------------------------------------------------------+\n");
        sb.append(String.format("| 🚀 ÍNDICE AVL (O(log N))  | Tiempo Total: %8.3f ms | Promedio: %7.1f ns |\n", tiempoTotalAVLMilis, avgAVLNanos));
        sb.append(String.format("| 🐌 LINEAL SCAN (O(N))     | Tiempo Total: %8.3f ms | Promedio: %7.1f ns |\n", tiempoTotalSecMilis, avgSecNanos));
        sb.append("+-------------------------------------------------------------------------+\n");
        sb.append(String.format("| 🔥 EL ÁRBOL AVL ES %.1f VECES MÁS RÁPIDO QUE EL ESCANEO LINEAL.        |\n", factorMejora));
        sb.append("+-------------------------------------------------------------------------+\n");

        return sb.toString();
    }
}
