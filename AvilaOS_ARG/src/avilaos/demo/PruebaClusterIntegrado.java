/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.cluster.Cluster;
import avilaos.model.EstadoProceso;
import avilaos.model.GeneradorId;
import avilaos.model.Pcb;
import avilaos.model.TipoProceso;
import avilaos.scheduler.PlanificadorFcfs;
import avilaos.sync.BufferAcotado;

/**
 *
 * @author richard
 * 
 * Pruebas del cluster completo: asignacion automatica, buffers, E/S, deadline y latencia de red.
 */
public class PruebaClusterIntegrado {
 
    private static int total = 0;
    private static int fallos = 0;
 
    private static Cluster nuevoCluster(int latencia) {
        GeneradorId.reiniciar();
        Cluster c = new Cluster(1000, 2000, latencia);
        c.agregarComputador(100, new PlanificadorFcfs());
        c.agregarComputador(50, new PlanificadorFcfs());
        return c;
    }
 
    private static void correr(Cluster c, int maxCiclos, Pcb... procesos) {
        int ciclos = 0;
        while (ciclos < maxCiclos && !terminaron(procesos)) {
            c.avanzarCiclo();
            ciclos++;
        }
    }
 
    private static boolean terminaron(Pcb[] procesos) {
        for (Pcb p : procesos) {
            if (p.getEstado() != EstadoProceso.TERMINADO) {
                return false;
            }
        }
        return true;
    }
 
    public static void main(String[] args) {
        probarAsignacionAutomatica();
        probarBufferRemotoDesdeElCluster();
        probarDeadlineDeUnConsumidorBloqueado();
        probarMuerteConLaRegionCriticaTomada();
        probarProcesoMixto();
        probarValidaciones();
 
        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }
 
    private static void probarAsignacionAutomatica() {
        System.out.println("--- Asignacion automatica (computador 0) ---");
        Cluster c = nuevoCluster(0);   // RAM: computador 1 = 100 MB, computador 2 = 50 MB
        Pcb a = c.crearProceso(0, "A", TipoProceso.CPU_BOUND, 1, 30, 10, 1000);
        Pcb b = c.crearProceso(0, "B", TipoProceso.CPU_BOUND, 1, 30, 10, 1000);
        Pcb d = c.crearProceso(0, "D", TipoProceso.CPU_BOUND, 1, 30, 10, 1000);
        verificar(a.getIdComputador() == 1, "A va al computador con mas memoria libre (el 1: 100 MB)");
        verificar(b.getIdComputador() == 1, "B tambien (le quedan 70 MB contra 50 MB del otro)");
        verificar(d.getIdComputador() == 2, "D va al computador 2 (el 1 ya solo tiene 40 MB libres)");
        verificar(lanza(() -> c.crearProceso(0, "Enorme", TipoProceso.CPU_BOUND, 1, 500, 10, 1000)),
                "un proceso que no cabe en ningun computador es rechazado");
        BufferAcotado buf = c.crearBuffer(0, 20);
        verificar(buf.getIdComputadorAnfitrion() == 1, "un buffer automatico tambien se aloja donde hay mas memoria libre");
    }
 
    private static void probarBufferRemotoDesdeElCluster() {
        System.out.println("--- Productor y consumidor en computadores distintos, buffer remoto ---");
        Cluster c = nuevoCluster(2);
        BufferAcotado b = c.crearBuffer(2, 3);
        Pcb prod = c.crearProcesoBuffer(1, "P", TipoProceso.PRODUCTOR, 1, 10, 100000, b.getId(), 2, 5);
        Pcb cons = c.crearProcesoBuffer(2, "C", TipoProceso.CONSUMIDOR, 1, 10, 100000, b.getId(), 1, 5);
        verificar(prod.getInstruccionesTotales() == 35 && cons.getInstruccionesTotales() == 30,
                "las instrucciones se calculan: 5 x (2+5) = 35 y 5 x (1+5) = 30");
        correr(c, 5000, prod, cons);
        verificar(terminaron(new Pcb[]{prod, cons}), "ambos terminan");
        verificar(b.getProducidos() == 5 && b.getConsumidos() == 5 && b.getElementos() == 0,
                "5 producidos, 5 consumidos, buffer vacio");
        verificar(c.getComputador(2).getRam().getUsada() == 3, "solo queda el buffer (3 MB) en la RAM del anfitrion");
        verificar(c.getComputador(1).getRam().getUsada() == 0, "el computador 1 quedo sin memoria usada");
        verificar(c.getBitacora().contiene("llegara en el ciclo"), "la bitacora registra los accesos remotos");
    }
 
    private static void probarDeadlineDeUnConsumidorBloqueado() {
        System.out.println("--- Un consumidor bloqueado muere por deadline ---");
        Cluster c = nuevoCluster(0);
        BufferAcotado b = c.crearBuffer(1, 2);
        Pcb cons = c.crearProcesoBuffer(1, "Solo", TipoProceso.CONSUMIDOR, 1, 10, 15, b.getId(), 1, 3);
        for (int i = 0; i < 15; i++) {
            c.avanzarCiclo();
        }
        verificar(cons.getEstado() == EstadoProceso.TERMINADO && cons.isTerminadoPorDeadline(),
                "el consumidor, esperando elementos que nunca llegan, muere por deadline");
        verificar(b.getHayElementos().cantidadBloqueados() == 0 && b.getHayElementos().getValor() == 0,
                "se retira de la cola del semaforo n y su voto vuelve (n = 0)");
        verificar(c.getComputador(1).getRam().getUsada() == 2, "libera su memoria; queda solo el buffer");
    }
 
    private static void probarMuerteConLaRegionCriticaTomada() {
        System.out.println("--- Un productor muere con la region critica tomada ---");
        Cluster c = nuevoCluster(0);
        BufferAcotado b = c.crearBuffer(1, 3);
        // cadaCiclos = 1: PC 0 producir, 1 semWait(e), 2 semWait(s), 3 anadir...
        // Con deadline 4 muere en el ciclo 4, justo despues de ejecutar semWait(s).
        Pcb condenado = c.crearProcesoBuffer(1, "Condenado", TipoProceso.PRODUCTOR, 1, 10, 4, b.getId(), 1, 2);
        for (int i = 0; i < 4; i++) {
            c.avanzarCiclo();
        }
        verificar(condenado.isTerminadoPorDeadline(), "el productor muere por deadline en el ciclo 4");
        verificar(b.getMutex().getValor() == 1, "el mutex del buffer quedo liberado (no se cierra para siempre)");
        verificar(c.getBitacora().contiene("region critica"), "la bitacora lo registra");
 
        Pcb sano = c.crearProcesoBuffer(1, "Sano", TipoProceso.PRODUCTOR, 1, 10, 100000, b.getId(), 1, 1);
        correr(c, 200, sano);
        verificar(sano.getEstado() == EstadoProceso.TERMINADO && b.getProducidos() == 1,
                "otro productor puede seguir usando el buffer despues");
    }
 
    private static void probarProcesoMixto() {
        System.out.println("--- Procesos CPU bound, I/O bound y buffer a la vez ---");
        Cluster c = nuevoCluster(1);
        BufferAcotado b = c.crearBuffer(1, 2);
        Pcb calculo = c.crearProceso(1, "Calculo", TipoProceso.CPU_BOUND, 2, 10, 20, 1000);
        Pcb io = c.crearProcesoEs(2, "Io", 1, 10, 12, 1000, 3, 4);
        Pcb prod = c.crearProcesoBuffer(2, "P", TipoProceso.PRODUCTOR, 1, 10, 1000, b.getId(), 1, 3);
        Pcb cons = c.crearProcesoBuffer(1, "C", TipoProceso.CONSUMIDOR, 3, 10, 1000, b.getId(), 1, 3);
        correr(c, 3000, calculo, io, prod, cons);
        verificar(terminaron(new Pcb[]{calculo, io, prod, cons}), "los 4 procesos terminan");
        verificar(c.contarTerminados() == 4, "el cluster cuenta 4 terminados");
        verificar(b.getProducidos() == 3 && b.getConsumidos() == 3, "el buffer movio 3 elementos");
        verificar(!calculo.isTerminadoPorDeadline() && !io.isTerminadoPorDeadline(),
                "ninguno murio por deadline");
    }
 
    private static void probarValidaciones() {
        System.out.println("--- Validaciones ---");
        Cluster c = nuevoCluster(0);
        verificar(lanza(() -> c.crearProcesoBuffer(1, "X", TipoProceso.PRODUCTOR, 1, 10, 100, 99, 1, 2)),
                "un buffer inexistente es rechazado");
        verificar(lanza(() -> c.crearProcesoBuffer(1, "X", TipoProceso.CPU_BOUND, 1, 10, 100, 1, 1, 2)),
                "un proceso CPU bound no puede usar buffer");
        BufferAcotado b = c.crearBuffer(1, 2);
        verificar(lanza(() -> c.crearProcesoBuffer(1, "Grande", TipoProceso.PRODUCTOR, 1, 5000, 100, b.getId(), 1, 2)),
                "un productor mas grande que la RAM es rechazado");
        verificar(c.getGestorBuffers().getBuffers().length == 1, "el rechazo no deja basura en el gestor");
        c.avanzarCiclo();
        verificar(lanza(() -> c.agregarComputador(10, new PlanificadorFcfs())),
                "tras el primer ciclo no se agregan computadores");
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
