package com.dbmotor.model;

import com.dbmotor.core.ArbolAVL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Tabla {
    private final String nombre;
    private final LinkedHashMap<String, TipoDato> esquema;
    private final String clavePrimaria;
    private final ArbolAVL<Registro> indice;

    public Tabla(String nombre, LinkedHashMap<String, TipoDato> esquema, String clavePrimaria) {
        this.nombre = nombre;
        this.esquema = esquema;
        this.clavePrimaria = clavePrimaria;

        if (!esquema.containsKey(clavePrimaria)) {
            throw new IllegalArgumentException(
                    "La columna PK '" + clavePrimaria + "' no existe en el esquema de la tabla.");
        }
        if (esquema.get(clavePrimaria) != TipoDato.INT) {
            throw new IllegalArgumentException(
                    "La clave primaria de la tabla debe ser estrictamente de tipo entero (INT).");
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

    public void insertar(Registro registro) {
        Object pkValRaw = registro.get(clavePrimaria);
        if (pkValRaw == null) {
            throw new IllegalArgumentException(
                    "El valor de la clave primaria '" + clavePrimaria + "' no puede ser nulo.");
        }

        for (Map.Entry<String, TipoDato> col : esquema.entrySet()) {
            String colName = col.getKey();
            TipoDato type = col.getValue();
            Object value = registro.get(colName);

            if (value != null) {
                try {
                    Object parsedVal = type.parsear(value.toString());
                    registro.set(colName, parsedVal);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Error de tipo en columna '" + colName + "': " + e.getMessage());
                }
            } else {
                if (colName.equals(clavePrimaria)) {
                    throw new IllegalArgumentException("La clave primaria '" + colName + "' no puede ser nula.");
                }
            }
        }

        Integer pkValue = (Integer) registro.get(clavePrimaria);
        indice.insert(pkValue, registro);
    }

    public boolean eliminar(Integer pk) {
        if (indice.search(pk) != null) {
            indice.delete(pk);
            return true;
        }
        return false;
    }

    public Registro buscar(Integer pk) {
        return indice.search(pk);
    }

    public List<Registro> buscarRango(Integer minVal, Integer maxVal) {
        return indice.searchRange(minVal, maxVal);
    }

    public List<Registro> obtenerTodos() {
        return indice.inorder();
    }

    public int size() {
        return indice.inorder().size();
    }

    public boolean isEmpty() {
        return indice.getRoot() == null;
    }
}