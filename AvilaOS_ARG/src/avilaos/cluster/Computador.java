/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.cluster;

import avilaos.hardware.Cpu;
import avilaos.hardware.Ram;
import avilaos.hardware.RelojGlobal;
import avilaos.kernel.Nucleo;
import avilaos.model.Pcb;
import avilaos.model.TipoProceso;
import avilaos.scheduler.Planificador;
import avilaos.util.Bitacora;

/**
 * @author richard
 * 
 * Un computador del sistema distribuido: agrupa su RAM, su CPU y su nucleo.
 * Todos los computadores son instancias de esta misma clase, con distinta
 * configuracion (identificador, memoria y politica de planificacion).
 */
public class Computador {

    private final int id;
    private final Ram ram;
    private final Cpu cpu;
    private final Nucleo nucleo;

    public Computador(int id, int memoriaMb, Planificador planificador, Bitacora bitacora) {
        if (id < 1) {
            throw new IllegalArgumentException("El identificador del computador debe ser 1 o mayor");
        }
        this.id = id;
        this.ram = new Ram(memoriaMb);
        this.cpu = new Cpu(id);
        this.nucleo = new Nucleo(id, ram, cpu, planificador, bitacora);
    }


    public void registrarEn(RelojGlobal reloj) {
        reloj.registrar(cpu);
        reloj.registrar(nucleo);
    }

    public Pcb crearProceso(String nombre, TipoProceso tipo, int prioridad, int memoria,
                            int instrucciones, int deadline, long cicloActual) {
        Pcb proceso = new Pcb(nombre, id, tipo, prioridad, memoria, instrucciones, deadline, cicloActual);
        nucleo.agregarProceso(proceso);
        return proceso;
    }

    public int getId() {
        return id;
    }

    public Ram getRam() {
        return ram;
    }

    public Cpu getCpu() {
        return cpu;
    }

    public Nucleo getNucleo() {
        return nucleo;
    }

    @Override
    public String toString() {
        return "Computador " + id + " (RAM " + ram + ", " + nucleo.getPlanificador().getTipo() + ")";
    }
}
