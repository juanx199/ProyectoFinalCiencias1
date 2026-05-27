package com.dbmotor.model;

/**
 * Enumeración que representa los tipos de datos soportados por el motor de base de datos.
 */
public enum TipoDato {
    INT,
    TEXT,
    REAL,
    BOOLEAN;

    /**
     * Valida y parsea un valor de texto al tipo de dato correspondiente en Java.
     * @param valorTexto El valor como cadena.
     * @return El objeto Java correspondiente (Integer, String, Double, Boolean).
     * @throws IllegalArgumentException si el formato de texto no es válido para el tipo.
     */
    public Object parsear(String valorTexto) {
        if (valorTexto == null || valorTexto.trim().equalsIgnoreCase("NULL")) {
            return null;
        }
        
        String cleanVal = valorTexto.trim();
        
        switch (this) {
            case INT:
                try {
                    return Integer.parseInt(cleanVal);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("No se puede parsear '" + cleanVal + "' como entero (INT).");
                }
            case REAL:
                try {
                    return Double.parseDouble(cleanVal);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("No se puede parsear '" + cleanVal + "' como decimal (REAL).");
                }
            case BOOLEAN:
                if (cleanVal.equalsIgnoreCase("true") || cleanVal.equalsIgnoreCase("1")) {
                    return Boolean.TRUE;
                } else if (cleanVal.equalsIgnoreCase("false") || cleanVal.equalsIgnoreCase("0")) {
                    return Boolean.FALSE;
                } else {
                    throw new IllegalArgumentException("No se puede parsear '" + cleanVal + "' como lógico (BOOLEAN).");
                }
            case TEXT:
                // Remover comillas si existen al inicio y final del texto
                if (cleanVal.startsWith("'") && cleanVal.endsWith("'") && cleanVal.length() >= 2) {
                    return cleanVal.substring(1, cleanVal.length() - 1);
                }
                if (cleanVal.startsWith("\"") && cleanVal.endsWith("\"") && cleanVal.length() >= 2) {
                    return cleanVal.substring(1, cleanVal.length() - 1);
                }
                return cleanVal;
            default:
                throw new IllegalStateException("Tipo de dato desconocido: " + this);
        }
    }
}
