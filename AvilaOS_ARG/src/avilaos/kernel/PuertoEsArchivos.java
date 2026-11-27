/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.kernel;

import avilaos.disco.Disco;
import avilaos.disco.PuertoEs;
import avilaos.disco.SolicitudDisco;
import avilaos.fs.Permiso;
import avilaos.fs.ResultadoFs;
import avilaos.fs.SistemaArchivos;
import avilaos.model.Pcb;
import avilaos.model.TipoOperacionES;

/**
 *
 * @author richard
 * 
 * Puerto de E/S real: une el sistema de archivos con el disco. Al pedir el disco,
 * el sistema de archivos calcula el bloque destino y los bloques a transferir, y la
 * solicitud va a la cola del disco. Cuando el disco termina, el nucleo llama a
 * completarOperacion y aqui se hace la operacion en el arbol de archivos.
 */
public class PuertoEsArchivos implements PuertoEs {

    private final SistemaArchivos sistemaArchivos;
    private final Disco disco;

    public PuertoEsArchivos(SistemaArchivos sistemaArchivos, Disco disco) {
        if (sistemaArchivos == null || disco == null) {
            throw new IllegalArgumentException("El puerto necesita el sistema de archivos y el disco");
        }
        this.sistemaArchivos = sistemaArchivos;
        this.disco = disco;
    }

    @Override
    public SolicitudDisco pedirDisco(Pcb proceso, long ciclo) {
        TipoOperacionES op = proceso.getOperacion();
        String ruta = proceso.getRuta();
        int destino = sistemaArchivos.bloqueDestino(op, ruta);
        int bloques = sistemaArchivos.bloquesATransferir(op, ruta, proceso.getBloquesNuevos());
        SolicitudDisco solicitud = new SolicitudDisco(proceso, op, ruta, destino, bloques, ciclo);
        disco.encolar(solicitud);
        return solicitud;
    }

    @Override
    public String completarOperacion(SolicitudDisco solicitud) {
        Pcb proceso = solicitud.getProceso();
        if (proceso == null) {
            return "ERROR - la solicitud no tiene proceso";
        }
        String ruta = solicitud.getRuta();
        ResultadoFs resultado;
        switch (solicitud.getOperacion()) {
            case CREAR:
                resultado = sistemaArchivos.crearArchivo(ruta, proceso.getBloquesNuevos(),
                        proceso.getUsuario(), Permiso.PRIVADO, proceso.getEtiqueta());
                break;
            case LEER:
                resultado = sistemaArchivos.leer(ruta);
                break;
            case ACTUALIZAR:
                resultado = actualizar(ruta, proceso.getNuevoNombre());
                break;
            case ELIMINAR:
                resultado = sistemaArchivos.eliminar(ruta);
                break;
            default:
                return "ERROR - " + solicitud.getOperacion() + " no es una operacion de archivo";
        }
        return (resultado.isExito() ? "OK - " : "ERROR - ") + resultado.getMensaje();
    }

    /**
     * Con nombre nuevo, renombra. Sin nombre nuevo (como los UPDATE del JSON del
     * enunciado) solo se escriben los metadatos: basta con que la ruta exista.
     */
    private ResultadoFs actualizar(String ruta, String nuevoNombre) {
        if (nuevoNombre != null && !nuevoNombre.isBlank()) {
            return sistemaArchivos.renombrar(ruta, nuevoNombre);
        }
        ResultadoFs existe = sistemaArchivos.leer(ruta);
        return existe.isExito() ? ResultadoFs.ok("Metadatos de " + ruta + " actualizados") : existe;
    }
}
