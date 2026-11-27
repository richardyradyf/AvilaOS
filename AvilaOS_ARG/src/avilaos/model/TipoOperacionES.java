/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.model;

/**
 *
 * @author richard
 * 
 * Operaciones que un proceso de E/S le pide al disco. Las cuatro primeras son las
 * operaciones CRUD sobre archivos; SUSPENDER y REACTIVAR las genera el planificador
 * a mediano plazo para mover un proceso entre la RAM y el area de swap.
 */
public enum TipoOperacionES {

    CREAR("Crear", "CREATE"),
    LEER("Leer", "READ"),
    ACTUALIZAR("Actualizar", "UPDATE"),
    ELIMINAR("Eliminar", "DELETE"),
    SUSPENDER("Suspender", null),
    REACTIVAR("Reactivar", null);

    private final String nombreVisible;
    private final String nombreJson;

    TipoOperacionES(String nombreVisible, String nombreJson) {
        this.nombreVisible = nombreVisible;
        this.nombreJson = nombreJson;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }


    public String getNombreJson() {
        return nombreJson;
    }


    public boolean esDeArchivo() {
        return nombreJson != null;
    }

    /**
     * Convierte el texto del JSON de procesos (CREATE, READ, UPDATE, DELETE) en la
     * operacion. No distingue mayusculas. SUSPENDER y REACTIVAR no se aceptan porque
     * solo los genera el sistema.
     *
     */
    public static TipoOperacionES desdeJson(String texto) {
        if (texto != null) {
            String limpio = texto.trim();
            for (TipoOperacionES op : values()) {
                if (op.nombreJson != null && op.nombreJson.equalsIgnoreCase(limpio)) {
                    return op;
                }
            }
        }
        throw new IllegalArgumentException("Operaci\u00f3n de E/S no v\u00e1lida: '" + texto
                + "' (se esperaba CREATE, READ, UPDATE o DELETE)");
    }

    @Override
    public String toString() {
        return nombreVisible;
    }
}
