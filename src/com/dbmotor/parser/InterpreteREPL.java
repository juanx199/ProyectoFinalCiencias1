package com.dbmotor.parser;

import com.dbmotor.model.BaseDatos;
import com.dbmotor.model.Registro;
import com.dbmotor.model.TipoDato;

import java.util.*;

// Consola de comandos interactiva REPL (Read-Eval-Print Loop).
//Proporciona interacción en consola con formateo ASCII premium para consultas relacionales.

public class InterpreteREPL {
    private final ParserSQL parser;
    private final Scanner scanner;

    public InterpreteREPL(ParserSQL parser) {
        this.parser = parser;
        this.scanner = new Scanner(System.in);
    }

    /**
     * Inicia el bucle REPL interactivo.
     */
    public void iniciar() {
        System.out.println("==================================================================");
        System.out.println("   MOTOR DE BASE DE DATOS INDEXADO POR ÁRBOL AVL - CONSOLA SQL    ");
        System.out.println("==================================================================");
        System.out.println("Escriba comandos SQL-like (CREATE TABLE, INSERT INTO, SELECT, DELETE).");
        System.out.println("Comandos adicionales: SHOW TABLES, DESCRIBE <tabla>.");
        System.out.println("Para salir escriba 'EXIT' o 'QUIT'. Cada comando finaliza con ';'.\n");

        while (true) {
            System.out.print("avl-db> ");
            String input;
            try {
                if (!scanner.hasNextLine()) {
                    break;
                }
                input = scanner.nextLine().trim();
            } catch (Exception e) {
                System.out.println(" Error al leer entrada.");
                break;
            }

            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit;")
                    || input.equalsIgnoreCase("quit;")) {
                System.out.println("\nCerrando motor de base de datos. ¡Hasta luego!");
                break;
            }

            if (input.isEmpty()) {
                continue;
            }

            // Bucle para acumular líneas si no termina en ';'
            StringBuilder queryBuilder = new StringBuilder(input);
            while (!queryBuilder.toString().trim().endsWith(";") && !queryBuilder.toString().equalsIgnoreCase("exit")
                    && !queryBuilder.toString().equalsIgnoreCase("quit")) {
                System.out.print("     -> ");
                if (!scanner.hasNextLine())
                    break;
                String nextLine = scanner.nextLine();
                queryBuilder.append(" ").append(nextLine.trim());
            }

            String query = queryBuilder.toString().trim();
            if (query.equalsIgnoreCase("exit") || query.equalsIgnoreCase("quit")) {
                System.out.println("\n👋 Cerrando motor de base de datos. ¡Hasta luego!");
                break;
            }

            // Ejecutar consulta de forma segura
            try {
                long start = System.nanoTime();
                ResultadoQuery resultado = parser.ejecutar(query);
                long elapsed = System.nanoTime() - start;

                imprimirResultado(resultado, elapsed);

            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
            System.out.println();
        }
    }

    // Imprime el resultado de forma estilizada con una tabla ASCII alineada.

    public static void imprimirResultado(ResultadoQuery res, long elapsedNanoseconds) {
        if (res.getMensaje() != null && !res.tieneFilas()) {
            System.out.println(" Confirmación: " + res.getMensaje());
            System.out.printf("   [Tiempo: %.2f ms]\n", elapsedNanoseconds / 1_000_000.0);
            return;
        }

        if (!res.tieneFilas()) {
            System.out.println("Empty set (0 registros).");
            System.out.printf("   [Tiempo: %.2f ms]\n", elapsedNanoseconds / 1_000_000.0);
            return;
        }

        LinkedHashMap<String, TipoDato> esquema = res.getEsquema();
        List<Registro> filas = res.getFilas();

        // 1. Calcular anchos máximos de columna
        Map<String, Integer> anchos = new LinkedHashMap<>();
        for (String col : esquema.keySet()) {
            anchos.put(col, col.length());
        }

        for (Registro reg : filas) {
            for (String col : esquema.keySet()) {
                Object val = reg.get(col);
                String valStr = val == null ? "NULL" : val.toString();
                anchos.put(col, Math.max(anchos.get(col), valStr.length()));
            }
        }

        // 2. borde horizontal (+----+--------+)
        StringBuilder bordeHorizontal = new StringBuilder("+");
        for (String col : esquema.keySet()) {
            int ancho = anchos.get(col) + 2;
            bordeHorizontal.append("-".repeat(ancho)).append("+");
        }

        // Imprimir Borde Superior
        System.out.println(bordeHorizontal);

        // 3. Imprimir Nombres de Columnas (| Columna1 | Columna2 |)
        StringBuilder cabecera = new StringBuilder("|");
        for (String col : esquema.keySet()) {
            int ancho = anchos.get(col);
            cabecera.append(" ").append(ajustarTexto(col, ancho)).append(" |");
        }
        System.out.println(cabecera);

        System.out.println(bordeHorizontal);

        // 4. Imprimir Filas
        for (Registro reg : filas) {
            StringBuilder fila = new StringBuilder("|");
            for (String col : esquema.keySet()) {
                int ancho = anchos.get(col);
                Object val = reg.get(col);
                String valStr = val == null ? "NULL" : val.toString();
                fila.append(" ").append(ajustarTexto(valStr, ancho)).append(" |");
            }
            System.out.println(fila);
        }

        // Borde Inferior
        System.out.println(bordeHorizontal);

        // Imprimir Estadísticas y Métricas
        System.out.println(res.getFilas().size() + " registros en conjunto (set).");
        System.out.println(" Algoritmo de Acceso: " + res.getMetricaRendimiento());
        System.out.printf(" Tiempo de Ejecución: %.3f ms\n", elapsedNanoseconds / 1_000_000.0);
    }

    // Centra/ajusta el texto agregando espacios en blanco para encajar en el ancho
    // especificado.

    private static String ajustarTexto(String texto, int ancho) {
        if (texto.length() >= ancho) {
            return texto.substring(0, ancho);
        }
        // Alinear a la izquierda
        return texto + " ".repeat(ancho - texto.length());
    }
}
