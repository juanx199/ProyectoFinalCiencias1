package com.dbmotor.model;

import com.dbmotor.core.ArbolAVL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Representa una tabla en memoria.
 * Contiene metadatos de esquema, nombre y el índice del árbol AVL.
 */
public class Tabla {
    private final String nombre;
    private final LinkedHashMap<String, TipoDato> esquema;
    private final String clavePrimaria;
    private final ArbolAVL<Registro> indice;

    public Tabla(String nombre, LinkedHashMap<String, TipoDato> esquema, String clavePrimaria) {
        this.nombre = nombre;
        this.esquema = esquema;
        this.clavePrimaria = clavePrimaria;
        
        // Verificar que la clave primaria exista en el esquema y sea INT
        if (!esquema.containsKey(clavePrimaria)) {
            throw new IllegalArgumentException("La columna PK '" + clavePrimaria + "' no existe en el esquema de la tabla.");
        }
        if (esquema.get(clavePrimaria) != TipoDato.INT) {
            throw new IllegalArgumentException("La clave primaria de la tabla debe ser estrictamente de tipo entero (INT).");
        }
        
        this.indice = new ArbolAVL<>();
    }

    public String getNombre() {
        return nombre;
    }

    public LinkedHashMap<String, TipoDato> getEsquema() {
        return esquema;
    }

    public String getClavePrimaria() {
        return clavePrimaria;
    }

    public ArbolAVL<Registro> getIndice() {
        return indice;
    }

    /**
     * Valida e inserta un registro en la tabla.
     * Realiza verificaciones rigurosas de tipo de datos de acuerdo al esquema.
     */
    public void insertar(Registro registro) {
        // 1. Validar que la clave primaria no sea nula
        Object pkValRaw = registro.get(clavePrimaria);
        if (pkValRaw == null) {
            throw new IllegalArgumentException("El valor de la clave primaria '" + clavePrimaria + "' no puede ser nulo.");
        }

        // 2. Validar tipos de datos del registro contra el esquema
        for (Map.Entry<String, TipoDato> col : esquema.entrySet()) {
            String colName = col.getKey();
            TipoDato type = col.getValue();
            Object value = registro.get(colName);

            if (value != null) {
                // Si el valor no coincide con el tipo esperado, intentar parsearlo o verificar compatibilidad
                try {
                    Object parsedVal = type.parsear(value.toString());
                    registro.set(colName, parsedVal);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Error de tipo en columna '" + colName + "': " + e.getMessage());
                }
            } else {
                // Es nulo, verificar si es clave primaria (no puede ser nulo)
                if (colName.equals(clavePrimaria)) {
                    throw new IllegalArgumentException("La clave primaria '" + colName + "' no puede ser nula.");
                }
            }
        }

        // Obtener el entero de la clave primaria
        Integer pkValue = (Integer) registro.get(clavePrimaria);

        // 3. Insertar en el árbol AVL (éste lanza excepción si hay colisión de clave)
        indice.insert(pkValue, registro);
    }

    /**
     * Elimina un registro por clave primaria.
     * @return true si el registro existía y fue eliminado, false de lo contrario.
     */
    public boolean eliminar(Integer pk) {
        if (indice.search(pk) != null) {
            indice.delete(pk);
            return true;
        }
        return false;
    }

    /**
     * Busca un registro por su clave primaria en O(log n).
     */
    public Registro buscar(Integer pk) {
        return indice.search(pk);
    }

    /**
     * Realiza una búsqueda por rango inclusivo [minVal, maxVal] de la clave primaria.
     */
    public List<Registro> buscarRango(Integer minVal, Integer maxVal) {
        return indice.searchRange(minVal, maxVal);
    }

    /**
     * Retorna todos los registros de la tabla ordenados secuencialmente por clave primaria.
     */
    public List<Registro> obtenerTodos() {
        return indice.inorder();
    }
}
