package com.dbmotor.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Representa el motor de base de datos en memoria (catálogo de tablas).
 */
public class BaseDatos {
    // Usamos TreeMap para mantener los nombres de las tablas ordenados alfabéticamente
    private final Map<String, Tabla> tablas;

    public BaseDatos() {
        this.tablas = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * Crea una nueva tabla en el catálogo de base de datos.
     * @return La tabla creada.
     */
    public Tabla crearTabla(String nombre, LinkedHashMap<String, TipoDato> esquema, String clavePrimaria) {
        if (tablas.containsKey(nombre)) {
            throw new IllegalArgumentException("Ya existe una tabla con el nombre '" + nombre + "' en la base de datos.");
        }
        Tabla nuevaTabla = new Tabla(nombre, esquema, clavePrimaria);
        tablas.put(nombre, nuevaTabla);
        return nuevaTabla;
    }

    /**
     * Elimina una tabla del catálogo.
     * @return true si la tabla fue eliminada con éxito.
     */
    public boolean eliminarTabla(String nombre) {
        if (tablas.containsKey(nombre)) {
            tablas.remove(nombre);
            return true;
        }
        return false;
    }

    /**
     * Registra una tabla ya instanciada (utilizado al cargar la persistencia).
     */
    public void registrarTabla(Tabla tabla) {
        tablas.put(tabla.getNombre(), tabla);
    }

    /**
     * Obtiene una tabla dada su nombre (insensible a mayúsculas/minúsculas).
     */
    public Tabla obtenerTabla(String nombre) {
        return tablas.get(nombre);
    }

    /**
     * Obtiene la colección completa de tablas de la base de datos.
     */
    public Collection<Tabla> obtenerTablas() {
        return tablas.values();
    }

    /**
     * Retorna si existe o no una tabla en el catálogo.
     */
    public boolean existeTabla(String nombre) {
        return tablas.containsKey(nombre);
    }
}
