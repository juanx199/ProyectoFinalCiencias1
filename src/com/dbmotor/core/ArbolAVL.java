package com.dbmotor.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación propia del Árbol AVL auto-balanceado.
 * Indexa registros utilizando claves de tipo Integer.
 */
public class ArbolAVL<V> {
    private NodoAVL<V> root;

    public ArbolAVL() {
        this.root = null;
    }

    public NodoAVL<V> getRoot() {
        return root;
    }

    // Obtiene la altura de un nodo de forma segura
    private int height(NodoAVL<V> node) {
        return (node == null) ? 0 : node.height;
    }

    // Obtiene el factor de balanceo de un nodo
    private int getBalance(NodoAVL<V> node) {
        return (node == null) ? 0 : height(node.left) - height(node.right);
    }

    // Rotación a la derecha (Caso Izquierda-Izquierda)
    private NodoAVL<V> rotarDerecha(NodoAVL<V> y) {
        NodoAVL<V> x = y.left;
        NodoAVL<V> T2 = x.right;

        // Realizar rotación
        x.right = y;
        y.left = T2;

        // Actualizar alturas
        y.height = Math.max(height(y.left), height(y.right)) + 1;
        x.height = Math.max(height(x.left), height(x.right)) + 1;

        // Retornar nueva raíz
        return x;
    }

    // Rotación a la izquierda (Caso Derecha-Derecha)
    private NodoAVL<V> rotarIzquierda(NodoAVL<V> x) {
        NodoAVL<V> y = x.right;
        NodoAVL<V> T2 = y.left;

        // Realizar rotación
        y.left = x;
        x.right = T2;

        // Actualizar alturas
        x.height = Math.max(height(x.left), height(x.right)) + 1;
        y.height = Math.max(height(y.left), height(y.right)) + 1;

        // Retornar nueva raíz
        return y;
    }

    /**
     * Inserta una nueva clave y su valor asociado en el árbol.
     * @throws IllegalArgumentException si la clave ya existe (violación de clave primaria).
     */
    public void insert(Integer key, V value) {
        this.root = insert(this.root, key, value);
    }

    private NodoAVL<V> insert(NodoAVL<V> node, Integer key, V value) {
        // 1. Realizar la inserción BST estándar
        if (node == null) {
            return new NodoAVL<>(key, value);
        }

        if (key < node.key) {
            node.left = insert(node.left, key, value);
        } else if (key > node.key) {
            node.right = insert(node.right, key, value);
        } else {
            // Clave duplicada encontrada
            throw new IllegalArgumentException("Violación de restricción de clave primaria única: La clave " + key + " ya existe.");
        }

        // 2. Actualizar la altura de este nodo ancestro
        node.height = 1 + Math.max(height(node.left), height(node.right));

        // 3. Obtener el factor de balanceo y rebalancear si es necesario
        int balance = getBalance(node);

        // Caso Izquierda Izquierda
        if (balance > 1 && key < node.left.key) {
            return rotarDerecha(node);
        }

        // Caso Derecha Derecha
        if (balance < -1 && key > node.right.key) {
            return rotarIzquierda(node);
        }

        // Caso Izquierda Derecha
        if (balance > 1 && key > node.left.key) {
            node.left = rotarIzquierda(node.left);
            return rotarDerecha(node);
        }

        // Caso Derecha Izquierda
        if (balance < -1 && key < node.right.key) {
            node.right = rotarDerecha(node.right);
            return rotarIzquierda(node);
        }

        // Retornar el nodo (sin cambios)
        return node;
    }

    /**
     * Elimina un nodo del árbol dada su clave.
     */
    public void delete(Integer key) {
        this.root = delete(this.root, key);
    }

    private NodoAVL<V> delete(NodoAVL<V> node, Integer key) {
        // 1. Realizar eliminación BST estándar
        if (node == null) {
            return null; // Clave no encontrada
        }

        if (key < node.key) {
            node.left = delete(node.left, key);
        } else if (key > node.key) {
            node.right = delete(node.right, key);
        } else {
            // Encontramos el nodo a eliminar
            if (node.left == null || node.right == null) {
                // Caso: 0 o 1 hijo
                NodoAVL<V> temp = (node.left != null) ? node.left : node.right;

                if (temp == null) {
                    // Caso sin hijos
                    node = null;
                } else {
                    // Caso con un hijo: Copiar contenido del hijo
                    node = temp;
                }
            } else {
                // Caso: 2 hijos. Obtener el sucesor en inorden (mínimo del subárbol derecho)
                NodoAVL<V> temp = obtenerMinimo(node.right);

                // Copiar los datos del sucesor al nodo actual
                node.key = temp.key;
                node.value = temp.value;

                // Eliminar el sucesor
                node.right = delete(node.right, temp.key);
            }
        }

        // Si el árbol tenía un solo nodo, retornar null
        if (node == null) {
            return null;
        }

        // 2. Actualizar altura
        node.height = 1 + Math.max(height(node.left), height(node.right));

        // 3. Obtener el factor de balanceo y rebalancear si es necesario
        int balance = getBalance(node);

        // Caso Izquierda Izquierda
        if (balance > 1 && getBalance(node.left) >= 0) {
            return rotarDerecha(node);
        }

        // Caso Izquierda Derecha
        if (balance > 1 && getBalance(node.left) < 0) {
            node.left = rotarIzquierda(node.left);
            return rotarDerecha(node);
        }

        // Caso Derecha Derecha
        if (balance < -1 && getBalance(node.right) <= 0) {
            return rotarIzquierda(node);
        }

        // Caso Derecha Izquierda
        if (balance < -1 && getBalance(node.right) > 0) {
            node.right = rotarDerecha(node.right);
            return rotarIzquierda(node);
        }

        return node;
    }

    private NodoAVL<V> obtenerMinimo(NodoAVL<V> node) {
        NodoAVL<V> current = node;
        while (current.left != null) {
            current = current.left;
        }
        return current;
    }

    /**
     * Busca el valor correspondiente a una clave en O(log n).
     */
    public V search(Integer key) {
        NodoAVL<V> result = search(this.root, key);
        return (result == null) ? null : result.value;
    }

    private NodoAVL<V> search(NodoAVL<V> node, Integer key) {
        if (node == null || node.key.equals(key)) {
            return node;
        }
        if (key < node.key) {
            return search(node.left, key);
        }
        return search(node.right, key);
    }

    /**
     * Retorna todos los valores del árbol ordenados por clave (Inorder completo).
     */
    public List<V> inorder() {
        List<V> result = new ArrayList<>();
        inorder(this.root, result);
        return result;
    }

    private void inorder(NodoAVL<V> node, List<V> result) {
        if (node != null) {
            inorder(node.left, result);
            result.add(node.value);
            inorder(node.right, result);
        }
    }

    /**
     * Retorna los valores del árbol que estén dentro de un rango inclusivo [lower, upper].
     * Utiliza un recorrido inorden acotado eficiente en O(log n + m).
     */
    public List<V> searchRange(Integer lower, Integer upper) {
        List<V> result = new ArrayList<>();
        searchRange(this.root, lower, upper, result);
        return result;
    }

    private void searchRange(NodoAVL<V> node, Integer lower, Integer upper, List<V> result) {
        if (node == null) return;

        // Si el valor del nodo es mayor que el límite inferior, explorar el subárbol izquierdo
        if (lower == null || node.key >= lower) {
            searchRange(node.left, lower, upper, result);
        }

        // Si el valor del nodo está dentro del rango, agregarlo al resultado
        if ((lower == null || node.key >= lower) && (upper == null || node.key <= upper)) {
            result.add(node.value);
        }

        // Si el valor del nodo es menor que el límite superior, explorar el subárbol derecho
        if (upper == null || node.key <= upper) {
            searchRange(node.right, lower, upper, result);
        }
    }

    /**
     * Valida recursivamente si todo el árbol cumple la propiedad de balanceo AVL (factor en [-1, 1]).
     * Método de utilidad para las pruebas unitarias y de estrés.
     */
    public boolean verifyBalance() {
        return verifyBalance(this.root);
    }

    private boolean verifyBalance(NodoAVL<V> node) {
        if (node == null) return true;
        int balance = getBalance(node);
        if (balance < -1 || balance > 1) {
            return false;
        }
        return verifyBalance(node.left) && verifyBalance(node.right);
    }
}
