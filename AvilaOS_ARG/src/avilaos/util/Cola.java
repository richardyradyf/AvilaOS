/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.util;

public class Cola<T> {

    private final Lista<T> elementos = new Lista<>();

    public void encolar(T dato) {
        elementos.agregarAlFinal(dato);
    }

    /** Saca y devuelve el primero, o null si la cola esta vacia. */
    public T desencolar() {
        return elementos.eliminarPrimero();
    }

    /** Devuelve el primero sin sacarlo, o null si la cola esta vacia. */
    public T verPrimero() {
        return elementos.verPrimero();
    }

    /** Devuelve el elemento en la posicion indicada (el primero es 0). */
    public T obtener(int indice) {
        return elementos.obtener(indice);
    }

    /** Elimina un elemento cualquiera (por ejemplo, el que el planificador eligio). */
    public boolean eliminar(T dato) {
        return elementos.eliminar(dato);
    }

    public boolean contiene(T dato) {
        return elementos.contiene(dato);
    }

    public int tamano() {
        return elementos.tamano();
    }

    public boolean estaVacia() {
        return elementos.estaVacia();
    }

    public void vaciar() {
        elementos.vaciar();
    }

    /** Ordena la cola con el comparador (estable). */
    public void ordenar(Comparador<T> comparador) {
        elementos.ordenar(comparador);
    }

    /** Encola respetando el orden, suponiendo que la cola ya esta ordenada. */
    public void encolarOrdenado(T dato, Comparador<T> comparador) {
        elementos.insertarOrdenado(dato, comparador);
    }

    public Iterador<T> iterador() {
        return elementos.iterador();
    }

    @Override
    public String toString() {
        return elementos.toString();
    }
}