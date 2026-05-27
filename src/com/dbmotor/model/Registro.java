package com.dbmotor.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encapsula un registro o fila dentro de una tabla.
 * Mantiene un mapeo interno de nombre de columna -> valor.
 */
public class Registro {
    private final Map<String, Object> valores;

    public Registro() {
        // Usamos LinkedHashMap para mantener el orden de inserción de las columnas
        this.valores = new LinkedHashMap<>();
    }

    public Registro(Map<String, Object> valoresIniciales) {
        this.valores = new LinkedHashMap<>(valoresIniciales);
    }

    public Object get(String columna) {
        return valores.get(columna);
    }

    public void set(String columna, Object valor) {
        valores.put(columna, valor);
    }

    public Map<String, Object> getValores() {
        return valores;
    }

    @Override
    public String toString() {
        return valores.toString();
    }
}
