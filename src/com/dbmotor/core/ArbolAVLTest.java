package com.dbmotor.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Suite de pruebas unitarias y de estrés para el Árbol AVL.
 * Valida la consistencia estructural del árbol (balance estricto [-1, 1])
 * ante inserciones masivas aleatorias y eliminaciones masivas.
 */
public class ArbolAVLTest {

    public static void runTests() {
        System.out.println("==================================================");
        System.out.println("  INICIANDO SUITE DE PRUEBAS DE ESTRÉS: ÁRBOL AVL ");
        System.out.println("==================================================");

        try {
            testInsercionMasivaYBalance();
            testEliminacionYRebalance();
            testBusquedaYRango();
            System.out.println("\n ¡FASE 1 COMPLETADA! Todas las pruebas del Árbol AVL pasaron de forma exitosa.\n");
        } catch (Exception e) {
            System.err.println("\n ERROR en las pruebas unitarias: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testInsercionMasivaYBalance() {
        System.out.print(" Ejecutando prueba de inserción de 10,000 elementos aleatorios... ");
        ArbolAVL<String> arbol = new ArbolAVL<>();
        Random random = new Random(42); // Semilla fija para reproducibilidad
        List<Integer> insertados = new ArrayList<>();

        for (int i = 0; i < 10000; i++) {
            int key = random.nextInt(1000000);
            // Evitar duplicados para no lanzar excepciones esperadas
            if (arbol.search(key) == null) {
                arbol.insert(key, "Val-" + key);
                insertados.add(key);

                // Verificar balance del árbol en tiempo real cada 1000 inserciones
                if (i % 1000 == 0) {
                    if (!arbol.verifyBalance()) {
                        throw new IllegalStateException(
                                "¡Fallo estructural! El factor de balanceo no es estricto en [-1, 1] en el paso " + i);
                    }
                }
            }
        }

        // Validación estructural final
        if (!arbol.verifyBalance()) {
            throw new IllegalStateException(" El árbol AVL no está balanceado.");
        }

        // Validación de recuperación
        for (int key : insertados) {
            String val = arbol.search(key);
            if (val == null || !val.equals("Val-" + key)) {
                throw new IllegalStateException("No se pudo recuperar la clave insertada: " + key);
            }
        }
        System.out.println("¡ÉXITO! (Estructura 100% balanceada y recuperable)");
    }

    private static void testEliminacionYRebalance() {
        System.out.print("-> Ejecutando prueba de eliminación masiva (5,000 elementos)... ");
        ArbolAVL<String> arbol = new ArbolAVL<>();
        Random random = new Random(100);
        List<Integer> insertados = new ArrayList<>();

        // 1. Insertar 5,000 elementos
        for (int i = 0; i < 5000; i++) {
            int key = random.nextInt(500000);
            if (arbol.search(key) == null) {
                arbol.insert(key, "Val-" + key);
                insertados.add(key);
            }
        }

        // Desordenar la lista para eliminar en orden aleatorio
        Collections.shuffle(insertados, random);

        // 2. Eliminar la mitad (2,500 elementos)
        int limite = insertados.size() / 2;
        for (int i = 0; i < limite; i++) {
            int key = insertados.get(i);
            arbol.delete(key);

            // Validar balanceo después de cada eliminación
            if (i % 500 == 0) {
                if (!arbol.verifyBalance()) {
                    throw new IllegalStateException(
                            "¡Fallo estructural en eliminación! Árbol desbalanceado en el paso " + i);
                }
            }
        }

        // 3. Verificar que los elementos eliminados ya no existen y los restantes sí
        if (!arbol.verifyBalance()) {
            throw new IllegalStateException("¡Fallo de balanceo al finalizar las eliminaciones!");
        }

        for (int i = 0; i < insertados.size(); i++) {
            int key = insertados.get(i);
            String val = arbol.search(key);
            if (i < limite) {
                // Debería estar eliminado
                if (val != null) {
                    throw new IllegalStateException("La clave eliminada " + key + " aún se encuentra en el árbol.");
                }
            } else {
                // Debería persistir
                if (val == null || !val.equals("Val-" + key)) {
                    throw new IllegalStateException("La clave remanente " + key + " se perdió o se corrompió.");
                }
            }
        }
        System.out.println("¡ÉXITO! (Eliminación correcta y balance estructural mantenido)");
    }

    private static void testBusquedaYRango() {
        System.out.print(" Ejecutando prueba de búsquedas exactas y consultas por rango... ");
        ArbolAVL<Integer> arbol = new ArbolAVL<>();

        // Insertar claves ordenadas para verificar que se autobalancea perfectamente
        for (int i = 1; i <= 100; i++) {
            arbol.insert(i, i * 10);
        }

        // Validar búsqueda exacta
        if (arbol.search(50) != 500) {
            throw new IllegalStateException("Búsqueda exacta fallida para la clave 50");
        }

        // Validar rango inclusivo [25, 35]
        List<Integer> rango = arbol.searchRange(25, 35);
        if (rango.size() != 11) {
            throw new IllegalStateException("El rango [25, 35] debió retornar 11 elementos, retornó: " + rango.size());
        }
        for (int i = 0; i < rango.size(); i++) {
            int expectedVal = (25 + i) * 10;
            if (rango.get(i) != expectedVal) {
                throw new IllegalStateException(
                        "Valor incorrecto en el rango: esperado " + expectedVal + " pero se obtuvo " + rango.get(i));
            }
        }

        // Validar rango con límites nulos (infinito)
        List<Integer> todo = arbol.searchRange(null, null);
        if (todo.size() != 100) {
            throw new IllegalStateException(
                    "El rango sin límites debió retornar 100 elementos, retornó: " + todo.size());
        }

        System.out.println("¡ÉXITO!");
    }

    public static void main(String[] args) {
        runTests();
    }
}
