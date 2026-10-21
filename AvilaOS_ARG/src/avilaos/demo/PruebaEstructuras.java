/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.util.Cola;
import avilaos.util.Cola;
import avilaos.util.Comparador;
import avilaos.util.Comparador;
import avilaos.util.Iterador;
import avilaos.util.Iterador;
import avilaos.util.Lista;
import avilaos.util.Lista;

/**
 * Pruebas manuales de las estructuras propias (Lista, Cola, Comparador, Iterador).
 * Ejecutar con Shift+F6; al final debe decir TODO CORRECTO.
 */
public class PruebaEstructuras {

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        probarListaBasica();
        probarEliminar();
        probarOrdenamiento();
        probarCola();
        probarErrores();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        if (fallos == 0) {
            System.out.println("TODO CORRECTO");
        } else {
            System.out.println("HAY FALLOS: revisar las lineas FALLO de arriba");
        }
    }

    private static void probarListaBasica() {
        System.out.println("--- Lista basica ---");
        Lista<Integer> lista = new Lista<>();
        verificar(lista.estaVacia(), "una lista nueva esta vacia");
        verificar(lista.tamano() == 0, "una lista nueva tiene tamano 0");
        verificar(lista.verPrimero() == null, "verPrimero en lista vacia devuelve null");
        verificar(lista.eliminarPrimero() == null, "eliminarPrimero en lista vacia devuelve null");

        lista.agregarAlFinal(1);
        lista.agregarAlFinal(2);
        lista.agregarAlFinal(3);
        verificarIgual("[1, 2, 3]", lista.toString(), "agregarAlFinal conserva el orden");

        lista.agregarAlInicio(0);
        verificarIgual("[0, 1, 2, 3]", lista.toString(), "agregarAlInicio pone el elemento primero");
        verificar(lista.tamano() == 4, "el tamano es 4");
        verificar(lista.obtener(2) == 2, "obtener(2) devuelve 2");
        verificar(lista.indiceDe(3) == 3, "indiceDe(3) devuelve 3");
        verificar(lista.contiene(3), "contiene(3) es verdadero");
        verificar(!lista.contiene(9), "contiene(9) es falso");
    }

    private static void probarEliminar() {
        System.out.println("--- Eliminar ---");
        Lista<Integer> lista = new Lista<>();
        for (int i = 0; i < 4; i++) {
            lista.agregarAlFinal(i);
        }
        verificar(lista.eliminar(0), "eliminar la cabeza");
        verificarIgual("[1, 2, 3]", lista.toString(), "la lista queda sin la cabeza");

        verificar(lista.eliminar(3), "eliminar la cola");
        verificarIgual("[1, 2]", lista.toString(), "la lista queda sin la cola");

        lista.agregarAlFinal(7);
        verificarIgual("[1, 2, 7]", lista.toString(), "tras eliminar la cola, agregarAlFinal sigue funcionando");

        verificar(lista.eliminar(2), "eliminar un elemento del medio");
        verificarIgual("[1, 7]", lista.toString(), "la lista queda sin el elemento del medio");

        verificar(!lista.eliminar(99), "eliminar un elemento inexistente devuelve false");

        lista.eliminarPrimero();
        lista.eliminarPrimero();
        verificar(lista.estaVacia(), "tras sacar todo, la lista esta vacia");

        lista.agregarAlFinal(5);
        verificarIgual("[5]", lista.toString(), "una lista vaciada se puede volver a usar");
    }

    private static void probarOrdenamiento() {
        System.out.println("--- Ordenamiento ---");
        Comparador<Integer> ascendente = (a, b) -> a - b;

        Lista<Integer> lista = new Lista<>();
        int[] valores = {5, 3, 8, 1, 3};
        for (int v : valores) {
            lista.agregarAlFinal(v);
        }
        lista.ordenar(ascendente);
        verificarIgual("[1, 3, 3, 5, 8]", lista.toString(), "ordenar deja la lista ascendente");
        verificar(lista.tamano() == 5, "ordenar conserva el tamano");
        lista.agregarAlFinal(10);
        verificarIgual("[1, 3, 3, 5, 8, 10]", lista.toString(), "tras ordenar, agregarAlFinal sigue funcionando");

        Lista<Integer> ordenada = new Lista<>();
        ordenada.agregarAlFinal(1);
        ordenada.agregarAlFinal(3);
        ordenada.agregarAlFinal(5);
        ordenada.insertarOrdenado(4, ascendente);
        ordenada.insertarOrdenado(0, ascendente);
        ordenada.insertarOrdenado(9, ascendente);
        ordenada.agregarAlFinal(10);
        verificarIgual("[0, 1, 3, 4, 5, 9, 10]", ordenada.toString(),
                "insertarOrdenado ubica el elemento al inicio, medio y final");

        Comparador<String> porLongitud = (a, b) -> a.length() - b.length();
        Lista<String> palabras = new Lista<>();
        palabras.agregarAlFinal("bb");
        palabras.agregarAlFinal("a");
        palabras.agregarAlFinal("cc");
        palabras.agregarAlFinal("d");
        palabras.ordenar(porLongitud);
        verificarIgual("[a, d, bb, cc]", palabras.toString(),
                "el ordenamiento es estable (los empates conservan el orden de llegada)");
    }

    private static void probarCola() {
        System.out.println("--- Cola ---");
        Cola<String> cola = new Cola<>();
        cola.encolar("A");
        cola.encolar("B");
        cola.encolar("C");
        verificarIgual("A", cola.desencolar(), "desencolar devuelve el primero (FIFO)");
        verificarIgual("B", cola.verPrimero(), "verPrimero no saca el elemento");
        verificar(cola.tamano() == 2, "el tamano es 2");

        cola.encolar("D");
        verificar(cola.eliminar("C"), "eliminar un elemento cualquiera de la cola");
        verificarIgual("[B, D]", cola.toString(), "la cola queda sin el elemento eliminado");

        StringBuilder recorrido = new StringBuilder();
        Iterador<String> it = cola.iterador();
        while (it.tieneSiguiente()) {
            recorrido.append(it.siguiente());
        }
        verificarIgual("BD", recorrido.toString(), "el iterador recorre en orden");

        Cola<Integer> porDeadline = new Cola<>();
        porDeadline.encolar(30);
        porDeadline.encolar(10);
        porDeadline.encolar(20);
        porDeadline.ordenar((a, b) -> a - b);
        verificarIgual("[10, 20, 30]", porDeadline.toString(), "la cola se puede ordenar con un comparador");
        porDeadline.encolarOrdenado(15, (a, b) -> a - b);
        verificarIgual("[10, 15, 20, 30]", porDeadline.toString(), "encolarOrdenado respeta el orden");
    }

    private static void probarErrores() {
        System.out.println("--- Errores esperados ---");
        Lista<Integer> lista = new Lista<>();
        lista.agregarAlFinal(1);

        boolean lanzo = false;
        try {
            lista.obtener(5);
        } catch (IndexOutOfBoundsException e) {
            lanzo = true;
        }
        verificar(lanzo, "obtener con indice mayor al tamano lanza IndexOutOfBoundsException");

        lanzo = false;
        try {
            lista.obtener(-1);
        } catch (IndexOutOfBoundsException e) {
            lanzo = true;
        }
        verificar(lanzo, "obtener con indice negativo lanza IndexOutOfBoundsException");
    }

    // ---- ayudas ----

    private static void verificar(boolean condicion, String descripcion) {
        total++;
        if (condicion) {
            System.out.println("OK    - " + descripcion);
        } else {
            fallos++;
            System.out.println("FALLO - " + descripcion);
        }
    }

    private static void verificarIgual(String esperado, String obtenido, String descripcion) {
        total++;
        if (esperado.equals(obtenido)) {
            System.out.println("OK    - " + descripcion);
        } else {
            fallos++;
            System.out.println("FALLO - " + descripcion);
            System.out.println("        esperado: " + esperado);
            System.out.println("        obtenido: " + obtenido);
        }
    }
}