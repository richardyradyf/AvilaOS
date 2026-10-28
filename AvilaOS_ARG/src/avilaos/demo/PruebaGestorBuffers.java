package avilaos.demo;

import avilaos.hardware.Cpu;
import avilaos.hardware.Ram;
import avilaos.hardware.RelojGlobal;
import avilaos.kernel.Nucleo;
import avilaos.model.EstadoProceso;
import avilaos.model.GeneradorId;
import avilaos.model.MotivoBloqueo;
import avilaos.model.Pcb;
import avilaos.model.TipoProceso;
import avilaos.scheduler.PlanificadorFcfs;
import avilaos.sync.BufferAcotado;
import avilaos.sync.GestorBuffers;
import avilaos.sync.ProgramaBuffer;
import avilaos.util.Bitacora;

/**
 * @author Gianm
 * Pruebas del gestor de buffers: el productor y el consumidor ejecutan su programa
 * de semaforos dentro del nucleo, con buffers locales y remotos. Reloj manual.
 */
public class PruebaGestorBuffers {

    private static int total = 0;
    private static int fallos = 0;
    private static final boolean VER_LOG = false;

    /** Dos computadores con un reloj y un gestor de buffers conectados en el orden correcto. */
    private static class Sistema {
        final Bitacora bitacora = new Bitacora(2000);
        final RelojGlobal reloj = new RelojGlobal(1000);
        final Cpu cpu1 = new Cpu(1);
        final Cpu cpu2 = new Cpu(2);
        final Nucleo nucleo1;
        final Nucleo nucleo2;
        final GestorBuffers gestor;

        Sistema(int latenciaRed) {
            nucleo1 = new Nucleo(1, new Ram(200), cpu1, new PlanificadorFcfs(), bitacora);
            nucleo2 = new Nucleo(2, new Ram(200), cpu2, new PlanificadorFcfs(), bitacora);
            gestor = new GestorBuffers(latenciaRed, bitacora);
            reloj.registrar(cpu1);
            reloj.registrar(nucleo1);
            reloj.registrar(cpu2);
            reloj.registrar(nucleo2);
            reloj.registrar(gestor);          // siempre despues de todos los nucleos
            gestor.registrarNucleo(nucleo1);
            gestor.registrarNucleo(nucleo2);
            bitacora.setEco(VER_LOG);
        }

        /** Crea un productor o consumidor de N elementos sobre un buffer. */
        Pcb crear(String nombre, int computador, TipoProceso tipo, int elementos,
                  int cadaCiclos, BufferAcotado buffer) {
            int instrucciones = ProgramaBuffer.instruccionesTotales(tipo, elementos, cadaCiclos);
            Pcb p = new Pcb(nombre, computador, tipo, 1, 10, instrucciones, 100000, reloj.getCicloActual());
            gestor.asignar(p, buffer.getId(), cadaCiclos);
            (computador == 1 ? nucleo1 : nucleo2).agregarProceso(p);
            return p;
        }

        /** Avanza hasta que todos terminen (o se agoten los ciclos). Devuelve los ciclos usados. */
        int correrHasta(Pcb... procesos) {
            int ciclos = 0;
            while (ciclos < 5000 && !todosTerminaron(procesos)) {
                reloj.avanzarCiclo();
                ciclos++;
            }
            return ciclos;
        }

        private boolean todosTerminaron(Pcb[] procesos) {
            for (Pcb p : procesos) {
                if (p.getEstado() != EstadoProceso.TERMINADO) {
                    return false;
                }
            }
            return true;
        }
    }

    public static void main(String[] args) {
        probarModosDeLaCpu();
        probarProductorYConsumidorLocales();
        probarBufferRemotoConLatencia();
        probarVariosProductoresYConsumidores();
        probarValidaciones();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarModosDeLaCpu() {
        System.out.println("--- Modo usuario y modo sistema operativo ---");
        GeneradorId.reiniciar();
        Sistema s = new Sistema(0);
        BufferAcotado b = s.gestor.crearBuffer(1, 10);
        Pcb prod = s.crear("Prod", 1, TipoProceso.PRODUCTOR, 3, 2, b);
        s.correrHasta(prod);
        verificar(prod.getEstado() == EstadoProceso.TERMINADO, "el productor termina solo (el buffer no se llena)");
        verificar(s.cpu1.getCiclosUsuario() == 6, "3 elementos x 2 'producir' = 6 ciclos en modo usuario");
        verificar(s.cpu1.getCiclosSistema() == 15, "3 elementos x 5 operaciones = 15 ciclos en modo sistema");
        verificar(b.getProducidos() == 3 && b.getElementos() == 3, "el buffer recibio los 3 elementos");
        verificar(s.nucleo1.getRam().getUsada() == 10, "el buffer ocupa 10 MB y el proceso ya libero los suyos");
    }

    private static void probarProductorYConsumidorLocales() {
        System.out.println("--- Productor y consumidor en el mismo computador (buffer de 1 espacio) ---");
        GeneradorId.reiniciar();
        Sistema s = new Sistema(0);
        BufferAcotado b = s.gestor.crearBuffer(1, 1);
        Pcb cons = s.crear("Cons", 1, TipoProceso.CONSUMIDOR, 3, 1, b);   // llega primero
        Pcb prod = s.crear("Prod", 1, TipoProceso.PRODUCTOR, 3, 1, b);

        s.reloj.avanzarCiclo();   // despacha al consumidor
        s.reloj.avanzarCiclo();   // ejecuta semWait(n) con el buffer vacio
        verificar(cons.getEstado() == EstadoProceso.BLOQUEADO && cons.getMotivoBloqueo() == MotivoBloqueo.BUFFER_VACIO,
                "el consumidor se bloquea porque el buffer esta vacio");
        verificar(b.getHayElementos().cantidadBloqueados() == 1, "esta en la cola del semaforo n");

        s.correrHasta(prod, cons);
        verificar(prod.getEstado() == EstadoProceso.TERMINADO && cons.getEstado() == EstadoProceso.TERMINADO,
                "ambos terminan: no hay interbloqueo");
        verificar(b.getProducidos() == 3 && b.getConsumidos() == 3 && b.getElementos() == 0,
                "se produjeron 3, se consumieron 3 y el buffer quedo vacio");
        verificar(b.getMutex().getValor() == 1 && b.getHayElementos().getValor() == 0
                && b.getHayEspacio().getValor() == 1, "los semaforos vuelven a sus valores iniciales");
        verificar(s.bitacora.contiene("BUFFER_VACIO") || s.bitacora.contiene("Buffer vacío"),
                "la bitacora registra el bloqueo por buffer vacio");
        verificar(s.bitacora.contiene("despierta a"), "la bitacora registra los despertares");
    }

    private static void probarBufferRemotoConLatencia() {
        System.out.println("--- Buffer en otro computador: latencia de red ---");
        int sinRed = tiempoProductorYConsumidor(0);
        int conRed = tiempoProductorYConsumidor(3);
        verificar(sinRed > 0, "la corrida sin latencia termina (" + sinRed + " ciclos)");
        verificar(conRed > sinRed, "con latencia 3 tarda mas (" + conRed + " ciclos contra " + sinRed + ")");

        GeneradorId.reiniciar();
        Sistema s = new Sistema(3);
        BufferAcotado b = s.gestor.crearBuffer(2, 5);   // anfitrion: computador 2
        Pcb prod = s.crear("ProdRemoto", 1, TipoProceso.PRODUCTOR, 1, 1, b);
        s.reloj.avanzarCiclo();   // despacho
        s.reloj.avanzarCiclo();   // producir (usuario)
        s.reloj.avanzarCiclo();   // semWait(e): sale hacia el computador 2
        verificar(prod.getEstado() == EstadoProceso.BLOQUEADO
                && prod.getMotivoBloqueo() == MotivoBloqueo.ACCESO_REMOTO,
                "al usar el buffer remoto, el proceso queda bloqueado por acceso remoto");
        verificar(s.gestor.cantidadEnViaje() == 1, "hay 1 operacion viajando por la red");
        verificar(b.getHayEspacio().getValor() == 5, "el buffer todavia no recibio la operacion");
        s.reloj.avanzarCiclo();
        s.reloj.avanzarCiclo();
        verificar(prod.getEstado() == EstadoProceso.BLOQUEADO, "sigue esperando la respuesta (2 de 3 ciclos)");
        s.reloj.avanzarCiclo();   // llega: se aplica y el proceso vuelve a listos
        verificar(b.getHayEspacio().getValor() == 4, "la operacion se aplica al llegar (e baja de 5 a 4)");
        s.correrHasta(prod);
        verificar(prod.getEstado() == EstadoProceso.TERMINADO && b.getProducidos() == 1,
                "el productor remoto termina y el buffer recibio su elemento");
        verificar(s.gestor.cantidadEnViaje() == 0, "no queda nada viajando por la red");
        verificar(s.nucleo2.getRam().getUsada() == 5, "el buffer ocupa 5 MB en la RAM del anfitrion (computador 2)");
        verificar(s.nucleo1.getRam().getUsada() == 0, "el computador 1 no guarda memoria del buffer");
    }

    /** Productor en el computador 1, consumidor en el 2 y buffer en el 2. */
    private static int tiempoProductorYConsumidor(int latencia) {
        GeneradorId.reiniciar();
        Sistema s = new Sistema(latencia);
        BufferAcotado b = s.gestor.crearBuffer(2, 2);
        Pcb prod = s.crear("P", 1, TipoProceso.PRODUCTOR, 4, 2, b);
        Pcb cons = s.crear("C", 2, TipoProceso.CONSUMIDOR, 4, 2, b);
        int ciclos = s.correrHasta(prod, cons);
        boolean bien = prod.getEstado() == EstadoProceso.TERMINADO && cons.getEstado() == EstadoProceso.TERMINADO
                && b.getProducidos() == 4 && b.getConsumidos() == 4 && b.getElementos() == 0;
        verificar(bien, "latencia " + latencia + ": se producen y consumen 4 elementos sin perder ninguno");
        return bien ? ciclos : -1;
    }

    private static void probarVariosProductoresYConsumidores() {
        System.out.println("--- Varios productores y consumidores, un buffer pequeno ---");
        GeneradorId.reiniciar();
        Sistema s = new Sistema(2);
        BufferAcotado b = s.gestor.crearBuffer(1, 2);
        Pcb p1 = s.crear("P1", 1, TipoProceso.PRODUCTOR, 5, 1, b);
        Pcb p2 = s.crear("P2", 2, TipoProceso.PRODUCTOR, 5, 2, b);
        Pcb c1 = s.crear("C1", 1, TipoProceso.CONSUMIDOR, 4, 1, b);
        Pcb c2 = s.crear("C2", 2, TipoProceso.CONSUMIDOR, 6, 1, b);
        s.correrHasta(p1, p2, c1, c2);
        verificar(p1.getEstado() == EstadoProceso.TERMINADO && p2.getEstado() == EstadoProceso.TERMINADO
                && c1.getEstado() == EstadoProceso.TERMINADO && c2.getEstado() == EstadoProceso.TERMINADO,
                "los 4 procesos terminan (sin interbloqueo)");
        verificar(b.getProducidos() == 10 && b.getConsumidos() == 10, "10 producidos y 10 consumidos");
        verificar(b.getElementos() == 0, "el buffer queda vacio");
        verificar(b.getMutex().getValor() == 1 && b.getHayElementos().getValor() == 0
                && b.getHayEspacio().getValor() == 2, "los semaforos vuelven a sus valores iniciales");
        verificar(b.getMutex().cantidadBloqueados() == 0 && b.getHayElementos().cantidadBloqueados() == 0
                && b.getHayEspacio().cantidadBloqueados() == 0, "no queda nadie en las colas de los semaforos");
        verificar(s.nucleo1.getBloqueados().length == 0 && s.nucleo2.getBloqueados().length == 0,
                "no queda ningun proceso bloqueado");
    }

    private static void probarValidaciones() {
        System.out.println("--- Validaciones ---");
        GeneradorId.reiniciar();
        Sistema s = new Sistema(0);
        BufferAcotado b = s.gestor.crearBuffer(1, 4);
        Pcb cpu = new Pcb("Normal", 1, TipoProceso.CPU_BOUND, 1, 10, 5, 100, 0);
        verificar(lanza(() -> s.gestor.asignar(cpu, b.getId(), 1)),
                "un proceso CPU bound no se asigna a un buffer");
        Pcb prod = new Pcb("P", 1, TipoProceso.PRODUCTOR, 1, 10, 12, 100, 0);
        verificar(lanza(() -> s.gestor.asignar(prod, 99, 1)), "un buffer inexistente es rechazado");
        verificar(lanza(() -> s.gestor.asignar(prod, b.getId(), 0)), "cadaCiclos 0 es rechazado");
        s.gestor.asignar(prod, b.getId(), 1);
        verificar(lanza(() -> s.gestor.asignar(prod, b.getId(), 1)), "no se asigna dos veces al mismo proceso");
        verificar(lanza(() -> s.gestor.crearBuffer(1, 500)), "un buffer mas grande que la RAM libre es rechazado");
        verificar(lanza(() -> s.gestor.crearBuffer(7, 2)), "un computador inexistente es rechazado");
        verificar(lanza(() -> new GestorBuffers(-1, s.bitacora)), "una latencia negativa es rechazada");
    }

    private static boolean lanza(Runnable accion) {
        try {
            accion.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return true;
        }
        return false;
    }

    private static void verificar(boolean condicion, String descripcion) {
        total++;
        if (condicion) {
            System.out.println("OK    - " + descripcion);
        } else {
            fallos++;
            System.out.println("FALLO - " + descripcion);
        }
    }
}
