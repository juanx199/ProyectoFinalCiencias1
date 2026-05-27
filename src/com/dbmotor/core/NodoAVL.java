package com.dbmotor.core;

/**
 * Clase que representa un nodo dentro de la estructura del Árbol AVL.
 * Sostiene una clave de tipo Integer (Clave Primaria) y un valor genérico.
 */
public class NodoAVL<V> {
    public Integer key;
    public V value;
    public NodoAVL<V> left;
    public NodoAVL<V> right;
    public int height;

    public NodoAVL(Integer key, V value) {
        this.key = key;
        this.value = value;
        this.height = 1; // Un nodo nuevo se crea inicialmente con altura 1
    }
}
