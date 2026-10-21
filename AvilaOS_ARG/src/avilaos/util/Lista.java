/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.util;

public class Lista<T> {

    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tamano;

    public int tamano() {
        return tamano;
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    /** Agrega al inicio. O(1). */
    public void agregarAlInicio(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.siguiente = cabeza;
        cabeza = nuevo;
        if (cola == null) {
            cola = nuevo;
        }
        tamano++;
    }

    /** Agrega al final. O(1) gracias a la referencia a la cola. */
    public void agregarAlFinal(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cola == null) {
            cabeza = nuevo;
        } else {
            cola.siguiente = nuevo;
        }
        cola = nuevo;
        tamano++;
    }

    /** Devuelve el primer elemento sin sacarlo, o null si la lista esta vacia. */
    public T verPrimero() {
        return cabeza == null ? null : cabeza.dato;
    }

    /** Saca y devuelve el primer elemento, o null si la lista esta vacia. O(1). */
    public T eliminarPrimero() {
        if (cabeza == null) {
            return null;
        }
        T dato = cabeza.dato;
        cabeza = cabeza.siguiente;
        if (cabeza == null) {
            cola = null;
        }
        tamano--;
        return dato;
    }

    /** Devuelve el elemento en la posicion indicada (la primera es 0). O(n). */
    public T obtener(int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException("Indice " + indice + ", tamano " + tamano);
        }
        Nodo<T> actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.siguiente;
        }
        return actual.dato;
    }

    /** Devuelve la posicion de la primera aparicion del dato, o -1 si no esta. */
    public int indiceDe(T dato) {
        Nodo<T> actual = cabeza;
        int i = 0;
        while (actual != null) {
            if (iguales(actual.dato, dato)) {
                return i;
            }
            actual = actual.siguiente;
            i++;
        }
        return -1;
    }

    public boolean contiene(T dato) {
        return indiceDe(dato) >= 0;
    }

    /**
     * Elimina la primera aparicion del dato.
     *
     * @return true si lo encontro y lo elimino
     */
    public boolean eliminar(T dato) {
        Nodo<T> anterior = null;
        Nodo<T> actual = cabeza;
        while (actual != null) {
            if (iguales(actual.dato, dato)) {
                if (anterior == null) {
                    cabeza = actual.siguiente;
                } else {
                    anterior.siguiente = actual.siguiente;
                }
                if (actual == cola) {
                    cola = anterior;
                }
                tamano--;
                return true;
            }
            anterior = actual;
            actual = actual.siguiente;
        }
        return false;
    }

    public void vaciar() {
        cabeza = null;
        cola = null;
        tamano = 0;
    }

    /**
     * Inserta el dato en su posicion segun el comparador, suponiendo que la
     * lista ya esta ordenada. En caso de empate queda despues de los equivalentes
     * (estable: respeta el orden de llegada).
     */
    public void insertarOrdenado(T dato, Comparador<T> comparador) {
        insertarNodoOrdenado(new Nodo<>(dato), comparador);
    }

    /**
     * Ordena la lista con el comparador usando ordenamiento por insercion.
     * Es estable: los elementos equivalentes conservan su orden relativo.
     */
    public void ordenar(Comparador<T> comparador) {
        Nodo<T> pendiente = cabeza;
        cabeza = null;
        cola = null;
        tamano = 0;
        while (pendiente != null) {
            Nodo<T> siguiente = pendiente.siguiente;
            insertarNodoOrdenado(pendiente, comparador);
            pendiente = siguiente;
        }
    }

    /** Devuelve un iterador que recorre la lista desde la cabeza. */
    public Iterador<T> iterador() {
        return new Iterador<T>() {
            private Nodo<T> actual = cabeza;

            @Override
            public boolean tieneSiguiente() {
                return actual != null;
            }

            @Override
            public T siguiente() {
                if (actual == null) {
                    throw new IllegalStateException("No quedan elementos por recorrer");
                }
                T dato = actual.dato;
                actual = actual.siguiente;
                return dato;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Nodo<T> actual = cabeza;
        while (actual != null) {
            sb.append(actual.dato);
            if (actual.siguiente != null) {
                sb.append(", ");
            }
            actual = actual.siguiente;
        }
        return sb.append("]").toString();
    }

    // ---- metodos privados ----

    private void insertarNodoOrdenado(Nodo<T> nuevo, Comparador<T> comparador) {
        Nodo<T> anterior = null;
        Nodo<T> actual = cabeza;
        while (actual != null && comparador.comparar(actual.dato, nuevo.dato) <= 0) {
            anterior = actual;
            actual = actual.siguiente;
        }
        nuevo.siguiente = actual;
        if (anterior == null) {
            cabeza = nuevo;
        } else {
            anterior.siguiente = nuevo;
        }
        if (actual == null) {
            cola = nuevo;
        }
        tamano++;
    }

    private boolean iguales(T a, T b) {
        return a == b || (a != null && a.equals(b));
    }
}
