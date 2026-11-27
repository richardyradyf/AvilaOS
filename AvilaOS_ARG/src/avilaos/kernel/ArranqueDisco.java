/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.kernel;

import avilaos.disco.Disco;
import avilaos.hardware.Cpu;
import avilaos.hardware.Relojable;

/**
 *
 * @author richard
 * 
 * Retiene el disco al arrancar para que las solicitudes de los procesos iniciales
 * lleguen juntas a la cola y la politica pueda elegir entre todas. Lo suelta cuando
 * la CPU queda libre y no hay listos, porque ya nadie mas puede pedir el disco.
 */
public class ArranqueDisco implements Relojable {

    private final Nucleo nucleo;
    private final Cpu cpu;
    private final Disco disco;

    public ArranqueDisco(Nucleo nucleo, Cpu cpu, Disco disco) {
        if (nucleo == null || cpu == null || disco == null) {
            throw new IllegalArgumentException("El arranque necesita el nucleo, la CPU y el disco");
        }
        this.nucleo = nucleo;
        this.cpu = cpu;
        this.disco = disco;
        disco.setRetenido(true);
    }

    @Override
    public void alCiclo(long numeroCiclo) {
        if (disco.isRetenido() && cpu.getProceso() == null && nucleo.getListos().length == 0) {
            disco.setRetenido(false);
        }
    }
}
