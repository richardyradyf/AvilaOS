/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.demo;

import avilaos.hardware.Cpu;
import avilaos.hardware.Ram;
import avilaos.hardware.RelojGlobal;
import avilaos.hardware.Relojable;
import avilaos.model.ModoEjecucion;
import avilaos.model.Pcb;
import avilaos.model.TipoProceso;

/**
 *
 * @author richard
 * 
 * Pruebas de la RAM, la CPU y el reloj global. Ejecutar con Shift+F6.
 * Dura unos 3 segundos porque prueba el reloj con su hilo real.
 */
public class PruebaHardware {

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) throws InterruptedException {
        probarRam();
        probarCpu();
        probarRelojManual();
        probarRelojYCpuJuntos();
        probarRelojConHilo();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    // ---------------- RAM ----------------

    private static void probarRam() throws InterruptedException {
        System.out.println("--- RAM ---");
        Ram ram = new Ram(1024);
        verificar(ram.getCapacidad() == 1024, "la capacidad es 1024");
        verificar(ram.getUsada() == 0 && ram.getLibre() == 1024, "una RAM nueva esta libre");

        verificar(ram.reservar(300), "reservar 300 cabe");
        verificar(ram.getUsada() == 300 && ram.getLibre() == 724, "quedan 724 libres");
        verificar(!ram.reservar(800), "reservar 800 no cabe y devuelve false");
        verificar(ram.getUsada() == 300, "una reserva fallida no cambia la memoria usada");
        verificar(ram.hayEspacio(724) && !ram.hayEspacio(725), "hayEspacio respeta el limite exacto");
        verificar(ram.reservar(724), "reservar exactamente lo que queda funciona");
        verificar(ram.getLibre() == 0, "la RAM queda llena");
        verificar(!ram.reservar(1), "con la RAM llena no cabe nada mas");

        ram.liberar(300);
        verificar(ram.getLibre() == 300, "liberar 300 deja 300 libres");
        verificar(Math.abs(ram.getPorcentajeUso() - 100.0 * 724 / 1024) < 1e-9,
                "el porcentaje de uso es 724/1024");

        verificar(lanza(IllegalStateException.class, () -> ram.liberar(5000)),
                "liberar mas de lo usado lanza excepcion");
        verificar(lanza(IllegalArgumentException.class, () -> ram.reservar(0)),
                "reservar 0 lanza excepcion");
        verificar(lanza(IllegalArgumentException.class, () -> ram.reservar(-5)),
                "reservar un valor negativo lanza excepcion");
        verificar(lanza(IllegalArgumentException.class, () -> new Ram(0)),
                "una RAM de capacidad 0 no se puede crear");

        Ram compartida = new Ram(1000);
        Runnable tarea = () -> {
            for (int i = 0; i < 500; i++) {
                compartida.reservar(1);
            }
        };
        Thread a = new Thread(tarea, "Ram-A");
        Thread b = new Thread(tarea, "Ram-B");
        a.start();
        b.start();
        a.join();
        b.join();
        verificar(compartida.getUsada() == 1000, "dos hilos reservando a la vez suman exactamente 1000");
    }

    // ---------------- CPU ----------------

    private static void probarCpu() {
        System.out.println("--- CPU ---");
        Cpu cpu = new Cpu(1);
        verificar(cpu.estaOciosa(), "una CPU nueva esta ociosa");
        verificar(cpu.getModo() == ModoEjecucion.SISTEMA_OPERATIVO, "sin proceso, la CPU esta en modo SO");
        verificar(cpu.getUtilizacion() == 0.0, "sin ciclos, la utilizacion es 0");

        Pcb p = nuevoPcb(5);
        verificar(lanza(IllegalStateException.class, () -> cpu.cargar(p)),
                "no se carga un proceso que no esta en EJECUCION");
        p.admitir();
        p.despachar(1);
        cpu.cargar(p);
        verificar(!cpu.estaOciosa() && cpu.getProceso() == p, "la CPU tiene cargado el proceso");
        verificar(cpu.getModo() == ModoEjecucion.USUARIO, "al cargar un proceso, la CPU pasa a modo usuario");

        Pcb otro = nuevoPcb(3);
        otro.admitir();
        otro.despachar(1);
        verificar(lanza(IllegalStateException.class, () -> cpu.cargar(otro)),
                "no se carga un segundo proceso sobre uno en ejecucion");

        cpu.alCiclo(1);
        cpu.alCiclo(2);
        cpu.alCiclo(3);
        verificar(p.getPc() == 3 && p.getInstruccionesRestantes() == 2,
                "tres ciclos ejecutan tres instrucciones del proceso");
        verificar(cpu.getCiclosUsuario() == 3, "tres ciclos contados en modo usuario");

        cpu.setModo(ModoEjecucion.SISTEMA_OPERATIVO);
        cpu.alCiclo(4);
        verificar(cpu.getCiclosSistema() == 1, "una instruccion en modo SO se cuenta aparte");

        Pcb descargado = cpu.descargar();
        verificar(descargado == p && cpu.estaOciosa(), "descargar devuelve el proceso y deja la CPU ociosa");
        verificar(cpu.getModo() == ModoEjecucion.SISTEMA_OPERATIVO, "una CPU ociosa vuelve a modo SO");

        cpu.alCiclo(5);   // ciclo ocioso
        verificar(cpu.getCiclosTotales() == 5 && cpu.getCiclosOcupada() == 4,
                "5 ciclos en total, 4 ocupada");
        verificar(Math.abs(cpu.getUtilizacion() - 0.8) < 1e-9, "utilizacion = 4/5 = 0.8");
        verificar(cpu.descargar() == null, "descargar una CPU ociosa devuelve null");
    }

    // ---------------- Reloj, avance manual ----------------

    private static void probarRelojManual() {
        System.out.println("--- Reloj (avance manual) ---");
        RelojGlobal reloj = new RelojGlobal(1000);
        StringBuilder orden = new StringBuilder();
        Relojable a = n -> orden.append("A").append(n);
        Relojable b = n -> orden.append("B").append(n);
        reloj.registrar(a);
        reloj.registrar(b);

        verificar(reloj.getCicloActual() == 0, "el reloj empieza en el ciclo 0");
        reloj.avanzarCiclo();
        reloj.avanzarCiclo();
        verificar(reloj.getCicloActual() == 2, "tras dos ciclos, el ciclo actual es 2");
        verificar("A1B1A2B2".equals(orden.toString()),
                "los componentes reciben cada ciclo en orden de registro (" + orden + ")");

        RelojGlobal otro = new RelojGlobal(1000);
        int[] contador = {0};
        otro.registrar(n -> {
            throw new IllegalStateException("fallo de prueba");
        });
        otro.registrar(n -> contador[0]++);
        System.out.println("(a continuacion se vera un mensaje de error en rojo: es lo esperado)");
        otro.avanzarCiclo();
        verificar(contador[0] == 1, "un componente que falla no impide que los demas reciban el ciclo");

        verificar(lanza(IllegalArgumentException.class, () -> new RelojGlobal(0)),
                "una duracion de 0 ms no es valida");
        verificar(lanza(IllegalArgumentException.class, () -> otro.setDuracionCicloMs(0)),
                "setDuracionCicloMs(0) lanza excepcion");
        verificar(lanza(IllegalArgumentException.class, () -> otro.registrar(null)),
                "registrar null lanza excepcion");
    }

    // ---------------- Reloj + CPU ----------------

    private static void probarRelojYCpuJuntos() {
        System.out.println("--- Reloj y CPU juntos ---");
        RelojGlobal reloj = new RelojGlobal(1000);
        Cpu cpu = new Cpu(1);
        reloj.registrar(cpu);

        Pcb p = nuevoPcb(6);
        p.admitir();
        p.despachar(0);
        cpu.cargar(p);

        for (int i = 0; i < 6; i++) {
            reloj.avanzarCiclo();
        }
        verificar(p.completoSusInstrucciones() && p.getPc() == 6,
                "6 ciclos del reloj ejecutan las 6 instrucciones del proceso");

        reloj.avanzarCiclo();   // el proceso ya termino sus instrucciones
        verificar(cpu.getCiclosTotales() == 7 && cpu.getCiclosOcupada() == 6,
                "el septimo ciclo cuenta como ocioso, sin error");
    }

    // ---------------- Reloj con su hilo ----------------

    private static class Registrador implements Relojable {
        volatile long ultimo = 0;
        volatile boolean consecutivo = true;
        volatile String hilo = "";

        @Override
        public void alCiclo(long numeroCiclo) {
            if (numeroCiclo != ultimo + 1) {
                consecutivo = false;
            }
            ultimo = numeroCiclo;
            hilo = Thread.currentThread().getName();
        }
    }

    private static void probarRelojConHilo() throws InterruptedException {
        System.out.println("--- Reloj con hilo propio ---");
        RelojGlobal reloj = new RelojGlobal(20);
        Registrador r = new Registrador();
        reloj.registrar(r);
        reloj.iniciar();

        Thread.sleep(400);
        verificar(reloj.getCicloActual() >= 5, "con ciclos de 20 ms, en 400 ms avanzan al menos 5 ciclos");
        verificar(r.consecutivo, "el componente recibe los ciclos consecutivos, sin saltos");
        verificar("Reloj-Global".equals(r.hilo), "los ciclos se ejecutan en el hilo Reloj-Global");
        verificar(lanza(IllegalStateException.class, () -> reloj.iniciar()),
                "no se puede iniciar el reloj dos veces");

        reloj.pausar();
        Thread.sleep(100);
        long congelado = reloj.getCicloActual();
        Thread.sleep(200);
        verificar(reloj.estaPausado() && reloj.getCicloActual() == congelado,
                "en pausa el ciclo no avanza");

        reloj.reanudar();
        Thread.sleep(200);
        verificar(reloj.getCicloActual() > congelado, "al reanudar el ciclo vuelve a avanzar");

        reloj.setDuracionCicloMs(200);
        Thread.sleep(250);
        long antes = reloj.getCicloActual();
        Thread.sleep(1000);
        long avanzados = reloj.getCicloActual() - antes;
        verificar(avanzados >= 3 && avanzados <= 7,
                "con ciclos de 200 ms, en 1 s avanzan entre 3 y 7 ciclos (avanzaron " + avanzados + ")");

        reloj.detener();
        verificar(!reloj.estaActivo(), "tras detener, el reloj no esta activo");
        long alDetener = reloj.getCicloActual();
        Thread.sleep(300);
        verificar(reloj.getCicloActual() == alDetener, "tras detener, el ciclo ya no avanza");
    }

    // ---------------- ayudas ----------------

    private static Pcb nuevoPcb(int instrucciones) {
        return new Pcb("Prueba", 1, TipoProceso.CPU_BOUND, 1, 64, instrucciones, 100, 0);
    }

    private static boolean lanza(Class<? extends RuntimeException> tipo, Runnable accion) {
        try {
            accion.run();
        } catch (RuntimeException e) {
            return tipo.isInstance(e);
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