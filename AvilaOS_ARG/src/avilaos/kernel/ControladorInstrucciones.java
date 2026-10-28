package avilaos.kernel;

import avilaos.model.ModoEjecucion;
import avilaos.model.MotivoBloqueo;
import avilaos.model.Pcb;

/**
 * @author Gianm
 * Gancho del nucleo para darle significado a las instrucciones de un proceso mas
 * alla de "gastar un ciclo". Lo implementa, por ejemplo, el gestor de buffers para
 * que el productor y el consumidor ejecuten su programa de semaforos.
 *
 * El nucleo lo llama dentro de su mutex, asi que una implementacion NUNCA debe
 * llamar a metodos publicos del mismo nucleo (el semaforo no es reentrante).
 */
public interface ControladorInstrucciones {
 
    /**
     * Modo (usuario o sistema operativo) en que se ejecutara la proxima instruccion
     * del proceso, o null si no hay que cambiarlo.
     */
    ModoEjecucion modoSiguiente(Pcb proceso);
 
    /**
     * Se llama justo despues de que la CPU ejecuto una instruccion del proceso (su PC
     * ya avanzo: la instruccion ejecutada es la numero getPc() - 1).
     *
     * @return el motivo por el que el proceso debe bloquearse ahora, o null si sigue
     */
    MotivoBloqueo despuesDeEjecutar(Pcb proceso, long ciclo);
 
    /**
     * Se llama cuando un proceso termina (normalmente o por deadline), para que el
     * controlador libere lo que tuviera asociado (colas de semaforos, accesos en
     * viaje...). Por defecto no hace nada.
     */
    default void alTerminar(Pcb proceso) {
    }
}
