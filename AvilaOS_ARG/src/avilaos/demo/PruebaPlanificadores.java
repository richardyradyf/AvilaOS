package avilaos.demo;

import avilaos.hardware.Cpu;
import avilaos.hardware.Ram;
import avilaos.hardware.RelojGlobal;
import avilaos.kernel.Nucleo;
import avilaos.model.EstadoProceso;
import avilaos.model.GeneradorId;
import avilaos.model.Pcb;
import avilaos.model.PoliticaPlanificacion;
import avilaos.model.TipoProceso;
import avilaos.scheduler.FabricaPlanificadores;
import avilaos.scheduler.Planificador;
import avilaos.scheduler.PlanificadorEdf;
import avilaos.scheduler.PlanificadorFcfs;
import avilaos.scheduler.PlanificadorPrioridades;
import avilaos.scheduler.PlanificadorRoundRobin;
import avilaos.util.Bitacora;

/** Pruebas de las politicas de planificacion con el reloj en modo manual. 
 * 
 * 
 * @author andre
 */
public class PruebaPlanificadores {

    private static int total = 0;
    private static int fallos = 0;

    private static class Sistema {
        final Bitacora bitacora = new Bitacora(500);
        final Ram ram = new Ram(1000);
        final Cpu cpu = new Cpu(1);
        final Nucleo nucleo;
        final RelojGlobal reloj = new RelojGlobal(1000);

        Sistema(Planificador planificador) {
            nucleo = new Nucleo(1, ram, cpu, planificador, bitacora);
            reloj.registrar(cpu);
            reloj.registrar(nucleo);
        }

        void avanzar(int ciclos) {
            for (int i = 0; i < ciclos; i++) {
                reloj.avanzarCiclo();
            }
        }

        /** Crea un proceso en el ciclo actual y se lo entrega al nucleo. */
        Pcb crear(String nombre, int prioridad, int instrucciones, int deadline) {
            Pcb p = new Pcb(nombre, 1, TipoProceso.CPU_BOUND, prioridad, 10, instrucciones,
                    deadline, reloj.getCicloActual());
            nucleo.agregarProceso(p);
            return p;
        }
    }

    public static void main(String[] args) {
        probarEdf();
        probarEdfApropiativo();
        probarRoundRobin();
        probarPrioridades();
        probarCambioEnCaliente();
        probarFabrica();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarEdf() {
        System.out.println("--- EDF: el limite mas cercano va primero ---");
        GeneradorId.reiniciar();
        Sistema s = new Sistema(new PlanificadorEdf());
        Pcb a = s.crear("A", 1, 5, 100);
        Pcb b = s.crear("B", 1, 3, 20);
        Pcb c = s.crear("C", 1, 2, 50);
        s.avanzar(1);
        verificar(s.cpu.getProceso() == b, "se despacha B, el de limite 20");
        s.avanzar(11);
        verificar(b.getCicloFinalizacion() == 4, "B termina en el ciclo 4");
        verificar(c.getCicloFinalizacion() == 6, "C (limite 50) termina en el ciclo 6");
        verificar(a.getCicloFinalizacion() == 11, "A (limite 100) termina al final, en el ciclo 11");
    }

    private static void probarEdfApropiativo() {
        System.out.println("--- EDF apropiativo ---");
        GeneradorId.reiniciar();
        Sistema s = new Sistema(new PlanificadorEdf());
        Pcb a = s.crear("A", 1, 10, 100);
        s.avanzar(3);
        verificar(s.cpu.getProceso() == a, "A esta en la CPU");
        Pcb b = s.crear("B", 1, 2, 10);   // limite 3 + 10 = 13, menor que el de A (100)
        s.avanzar(1);
        verificar(s.cpu.getProceso() == b && a.getEstado() == EstadoProceso.LISTO,
                "B, con limite menor, expulsa a A");

        GeneradorId.reiniciar();
        Sistema n = new Sistema(new PlanificadorEdf(false));
        Pcb a2 = n.crear("A", 1, 10, 100);
        n.avanzar(3);
        n.crear("B", 1, 2, 10);
        n.avanzar(1);
        verificar(n.cpu.getProceso() == a2, "sin apropiacion, A conserva la CPU");
    }

    private static void probarRoundRobin() {
        System.out.println("--- Round Robin ---");
        GeneradorId.reiniciar();
        Sistema s = new Sistema(new PlanificadorRoundRobin(2));
        Pcb a = s.crear("A", 1, 4, 100);
        Pcb b = s.crear("B", 1, 4, 100);
        s.avanzar(3);
        verificar(s.cpu.getProceso() == b && a.getPc() == 2, "A usa su quantum de 2 ciclos y pasa B");
        s.avanzar(6);
        verificar(a.getCicloFinalizacion() == 7 && b.getCicloFinalizacion() == 9,
                "A termina en el ciclo 7 y B en el 9");

        GeneradorId.reiniciar();
        Sistema solo = new Sistema(new PlanificadorRoundRobin(2));
        Pcb unico = solo.crear("Solo", 1, 5, 100);
        solo.avanzar(6);
        verificar(unico.getCicloFinalizacion() == 6,
                "un proceso solo no se expulsa a si mismo: termina en el ciclo 6");

        verificar(lanza(() -> new PlanificadorRoundRobin(0)), "un quantum de 0 es rechazado");
    }

    private static void probarPrioridades() {
        System.out.println("--- Prioridades ---");
        GeneradorId.reiniciar();
        Sistema s = new Sistema(new PlanificadorPrioridades());
        Pcb a = s.crear("A", 3, 6, 100);
        Pcb b = s.crear("B", 1, 2, 100);
        Pcb c = s.crear("C", 2, 2, 100);
        s.avanzar(11);
        verificar(b.getCicloFinalizacion() == 3 && c.getCicloFinalizacion() == 5
                && a.getCicloFinalizacion() == 11, "terminan en orden de prioridad: B(1), C(2), A(3)");

        GeneradorId.reiniciar();
        Sistema ap = new Sistema(new PlanificadorPrioridades());
        Pcb lento = ap.crear("Lento", 3, 10, 100);
        ap.avanzar(3);
        Pcb urgente = ap.crear("Urgente", 1, 2, 100);
        ap.avanzar(1);
        verificar(ap.cpu.getProceso() == urgente && lento.getEstado() == EstadoProceso.LISTO,
                "un proceso de prioridad 1 expulsa al de prioridad 3");

        GeneradorId.reiniciar();
        Sistema igual = new Sistema(new PlanificadorPrioridades());
        Pcb x = igual.crear("X", 2, 10, 100);
        igual.avanzar(3);
        igual.crear("Y", 2, 2, 100);
        igual.avanzar(1);
        verificar(igual.cpu.getProceso() == x, "con prioridad igual no se expulsa");
    }

    private static void probarCambioEnCaliente() {
        System.out.println("--- Cambio de politica en caliente ---");
        GeneradorId.reiniciar();
        Sistema s = new Sistema(new PlanificadorFcfs());
        Pcb a = s.crear("A", 1, 5, 100);
        Pcb b = s.crear("B", 1, 5, 20);
        Pcb c = s.crear("C", 1, 5, 50);
        Pcb[] antes = s.nucleo.getListos();
        verificar(antes[0] == a && antes[1] == b && antes[2] == c, "con FCFS la cola es A, B, C");
        s.nucleo.cambiarPlanificador(new PlanificadorEdf());
        Pcb[] despues = s.nucleo.getListos();
        verificar(despues[0] == b && despues[1] == c && despues[2] == a,
                "al cambiar a EDF la cola se reordena: B, C, A");
    }

    private static void probarFabrica() {
        System.out.println("--- Fabrica ---");
        verificar(FabricaPlanificadores.crear(PoliticaPlanificacion.FCFS).getTipo() == PoliticaPlanificacion.FCFS,
                "crea FCFS");
        verificar(FabricaPlanificadores.crear(PoliticaPlanificacion.EDF).getTipo() == PoliticaPlanificacion.EDF,
                "crea EDF");
        verificar(FabricaPlanificadores.crear(PoliticaPlanificacion.PRIORIDADES).getTipo()
                == PoliticaPlanificacion.PRIORIDADES, "crea Prioridades");
        Planificador rr = FabricaPlanificadores.crear(PoliticaPlanificacion.ROUND_ROBIN, 7);
        verificar(rr.getTipo() == PoliticaPlanificacion.ROUND_ROBIN
                && ((PlanificadorRoundRobin) rr).getQuantum() == 7, "crea Round Robin con quantum 7");
        verificar(lanza(() -> FabricaPlanificadores.crear(null)), "una politica nula es rechazada");
    }

    private static boolean lanza(Runnable accion) {
        try {
            accion.run();
        } catch (IllegalArgumentException e) {
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