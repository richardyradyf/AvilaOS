package avilaos.demo;

import avilaos.config.ConfiguracionProcesos;
import avilaos.config.EntradaInvalidaException;
import avilaos.model.PoliticaDisco;
import avilaos.model.PoliticaPlanificacion;
import avilaos.model.SolicitudProceso;
import avilaos.model.TipoOperacionES;

/**
 * @author andre
 *
 * Pruebas de la clase ConfiguracionProcesos:
 * 1. Carga del JSON de procesos y configuracion del enunciado (8 procesos, politica FIFO).
 * 2. Rechazo de 8 casos invalidos con EntradaInvalidaException y mensajes claros.
 * 3. Prueba de ida y vuelta: aJson() seguido de desdeJson() produce una configuracion equivalente.
 */
public class PruebaConfiguracionProcesos {

    /** JSON del enunciado (Archivo de Procesos y Configuracion). */
    private static final String JSON_ENUNCIADO = """
            {
              "globalConfig": {
                "cycleDurationMs": 300,
                "swapSizeBlocks": 100,
                "initialDiskPolicy": "FIFO"
              },
              "computer": {
                "ramCapacity": 1024,
                "policy": "FCFS",
                "quantum": 0
              },
              "processes": [
                {
                  "name": "Proc_95",
                  "memory": 16,
                  "deadline": 500,
                  "instructions": 2,
                  "targetPath": "/sistema/config.sys",
                  "operation": "READ"
                },
                {
                  "name": "Proc_180",
                  "memory": 16,
                  "deadline": 500,
                  "instructions": 2,
                  "targetPath": "/usuarios/video.mp4",
                  "operation": "READ"
                },
                {
                  "name": "Proc_34",
                  "memory": 16,
                  "deadline": 500,
                  "instructions": 2,
                  "targetPath": "/usuarios/notas.txt",
                  "operation": "UPDATE"
                },
                {
                  "name": "Proc_119",
                  "memory": 16,
                  "deadline": 500,
                  "instructions": 2,
                  "targetPath": "/usuarios/img1.png",
                  "operation": "DELETE"
                },
                {
                  "name": "Proc_11",
                  "memory": 16,
                  "deadline": 500,
                  "instructions": 2,
                  "targetPath": "/sistema/boot.bin",
                  "operation": "READ"
                },
                {
                  "name": "Proc_123",
                  "memory": 16,
                  "deadline": 500,
                  "instructions": 2,
                  "targetPath": "/usuarios/data.csv",
                  "operation": "UPDATE"
                },
                {
                  "name": "Proc_62",
                  "memory": 16,
                  "deadline": 500,
                  "instructions": 2,
                  "targetPath": "/sistema/script.py",
                  "operation": "READ"
                },
                {
                  "name": "Proc_64",
                  "memory": 16,
                  "deadline": 500,
                  "instructions": 2,
                  "targetPath": "/sistema/core.dll",
                  "operation": "READ"
                }
              ]
            }
            """;

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        probarJsonEnunciado();
        probarCasosInvalidos();
        probarIdaYVuelta();

        System.out.println();
        System.out.println("Pruebas: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar lineas FALLO");
    }

    private static void probarJsonEnunciado() {
        System.out.println("--- Carga del JSON del enunciado ---");
        ConfiguracionProcesos cfg = ConfiguracionProcesos.desdeJson(JSON_ENUNCIADO);

        verificar(cfg.getCycleDurationMs() == 300, "cycleDurationMs es 300");
        verificar(cfg.getSwapSizeBlocks() == 100, "swapSizeBlocks es 100");
        verificar(cfg.getInitialDiskPolicy() == PoliticaDisco.FIFO, "initialDiskPolicy es FIFO");
        verificar(cfg.getHeadSpeed() == 5, "headSpeed por defecto es 5");

        verificar(cfg.getRamCapacity() == 1024, "computer.ramCapacity es 1024");
        verificar(cfg.getComputerPolicy() == PoliticaPlanificacion.FCFS, "computer.policy es FCFS");
        verificar(cfg.getQuantum() == 0, "computer.quantum es 0");

        SolicitudProceso[] procs = cfg.getProcesos();
        verificar(procs.length == 8, "se cargaron exactamente 8 procesos");

        // Proc_95
        verificar(procs[0].getNombre().equals("Proc_95"), "P1 nombre es Proc_95");
        verificar(procs[0].getOperacion() == TipoOperacionES.LEER, "P1 operacion es LEER");
        verificar(procs[0].getRuta().equals("/sistema/config.sys"), "P1 ruta es /sistema/config.sys");
        verificar(procs[0].getMemoria() == 16, "P1 memoria es 16");

        // Proc_180
        verificar(procs[1].getNombre().equals("Proc_180"), "P2 nombre es Proc_180");
        verificar(procs[1].getOperacion() == TipoOperacionES.LEER, "P2 operacion es LEER");
        verificar(procs[1].getRuta().equals("/usuarios/video.mp4"), "P2 ruta es /usuarios/video.mp4");

        // Proc_34
        verificar(procs[2].getNombre().equals("Proc_34"), "P3 nombre es Proc_34");
        verificar(procs[2].getOperacion() == TipoOperacionES.ACTUALIZAR, "P3 operacion es ACTUALIZAR");
        verificar(procs[2].getRuta().equals("/usuarios/notas.txt"), "P3 ruta es /usuarios/notas.txt");

        // Proc_119
        verificar(procs[3].getNombre().equals("Proc_119"), "P4 nombre es Proc_119");
        verificar(procs[3].getOperacion() == TipoOperacionES.ELIMINAR, "P4 operacion es ELIMINAR");
        verificar(procs[3].getRuta().equals("/usuarios/img1.png"), "P4 ruta es /usuarios/img1.png");

        // Proc_11
        verificar(procs[4].getNombre().equals("Proc_11"), "P5 operacion es LEER en /sistema/boot.bin");

        // Proc_123
        verificar(procs[5].getNombre().equals("Proc_123"), "P6 operacion es ACTUALIZAR en /usuarios/data.csv");

        // Proc_62
        verificar(procs[6].getNombre().equals("Proc_62"), "P7 operacion es LEER en /sistema/script.py");

        // Proc_64
        verificar(procs[7].getNombre().equals("Proc_64"), "P8 operacion es LEER en /sistema/core.dll");
    }

    private static void probarCasosInvalidos() {
        System.out.println("--- Casos JSON invalidos rechazados ---");

        // 1. Falta globalConfig
        esperarError("""
                {
                  "computer": { "ramCapacity": 512, "policy": "FCFS" },
                  "processes": []
                }
                """, "falta globalConfig");

        // 2. cycleDurationMs fuera de rango
        esperarError("""
                {
                  "globalConfig": { "cycleDurationMs": 20, "swapSizeBlocks": 100, "initialDiskPolicy": "FIFO" },
                  "computer": { "ramCapacity": 512, "policy": "FCFS" },
                  "processes": []
                }
                """, "cycleDurationMs < 50 rechazado");

        // 3. initialDiskPolicy no valida
        esperarError("""
                {
                  "globalConfig": { "cycleDurationMs": 300, "swapSizeBlocks": 100, "initialDiskPolicy": "LIFO" },
                  "computer": { "ramCapacity": 512, "policy": "FCFS" },
                  "processes": []
                }
                """, "initialDiskPolicy LIFO rechazada");

        // 4. ramCapacity no valida (negativa o 0)
        esperarError("""
                {
                  "globalConfig": { "cycleDurationMs": 300, "swapSizeBlocks": 100, "initialDiskPolicy": "FIFO" },
                  "computer": { "ramCapacity": 0, "policy": "FCFS" },
                  "processes": []
                }
                """, "ramCapacity 0 rechazada");

        // 5. computer.policy inexistente
        esperarError("""
                {
                  "globalConfig": { "cycleDurationMs": 300, "swapSizeBlocks": 100, "initialDiskPolicy": "FIFO" },
                  "computer": { "ramCapacity": 512, "policy": "SHORTEST_JOB_FIRST" },
                  "processes": []
                }
                """, "politica de CPU inexistente rechazada");

        // 6. targetPath sin barra inicial
        esperarError("""
                {
                  "globalConfig": { "cycleDurationMs": 300, "swapSizeBlocks": 100, "initialDiskPolicy": "FIFO" },
                  "computer": { "ramCapacity": 512, "policy": "FCFS" },
                  "processes": [
                    { "name": "P1", "memory": 10, "deadline": 100, "instructions": 2, "targetPath": "archivo.txt", "operation": "READ" }
                  ]
                }
                """, "targetPath sin '/' rechazado");

        // 7. operacion no valida (MOVE)
        esperarError("""
                {
                  "globalConfig": { "cycleDurationMs": 300, "swapSizeBlocks": 100, "initialDiskPolicy": "FIFO" },
                  "computer": { "ramCapacity": 512, "policy": "FCFS" },
                  "processes": [
                    { "name": "P1", "memory": 10, "deadline": 100, "instructions": 2, "targetPath": "/archivo.txt", "operation": "MOVE" }
                  ]
                }
                """, "operation MOVE rechazada");

        // 8. CREATE sin blocks
        esperarError("""
                {
                  "globalConfig": { "cycleDurationMs": 300, "swapSizeBlocks": 100, "initialDiskPolicy": "FIFO" },
                  "computer": { "ramCapacity": 512, "policy": "FCFS" },
                  "processes": [
                    { "name": "P1", "memory": 10, "deadline": 100, "instructions": 2, "targetPath": "/nuevo.txt", "operation": "CREATE" }
                  ]
                }
                """, "CREATE sin blocks rechazado");

        // 9. Texto donde va un numero (memoria no numerica)
        esperarError("""
                {
                  "globalConfig": { "cycleDurationMs": 300, "swapSizeBlocks": 100, "initialDiskPolicy": "FIFO" },
                  "computer": { "ramCapacity": 512, "policy": "FCFS" },
                  "processes": [
                    { "name": "P1", "memory": "diez", "deadline": 100, "instructions": 2, "targetPath": "/nuevo.txt", "operation": "READ" }
                  ]
                }
                """, "memory como texto rechazada");
    }

    private static void probarIdaYVuelta() {
        System.out.println("--- Ida y vuelta: aJson() -> desdeJson() ---");
        ConfiguracionProcesos cfg1 = ConfiguracionProcesos.desdeJson(JSON_ENUNCIADO);
        String jsonGenerado = cfg1.aJson();

        ConfiguracionProcesos cfg2 = ConfiguracionProcesos.desdeJson(jsonGenerado);

        verificar(cfg1.getCycleDurationMs() == cfg2.getCycleDurationMs(), "ida/vuelta: cycleDurationMs coincide");
        verificar(cfg1.getSwapSizeBlocks() == cfg2.getSwapSizeBlocks(), "ida/vuelta: swapSizeBlocks coincide");
        verificar(cfg1.getInitialDiskPolicy() == cfg2.getInitialDiskPolicy(), "ida/vuelta: initialDiskPolicy coincide");
        verificar(cfg1.getHeadSpeed() == cfg2.getHeadSpeed(), "ida/vuelta: headSpeed coincide");
        verificar(cfg1.getRamCapacity() == cfg2.getRamCapacity(), "ida/vuelta: ramCapacity coincide");
        verificar(cfg1.getComputerPolicy() == cfg2.getComputerPolicy(), "ida/vuelta: computerPolicy coincide");

        SolicitudProceso[] p1 = cfg1.getProcesos();
        SolicitudProceso[] p2 = cfg2.getProcesos();
        verificar(p1.length == p2.length, "ida/vuelta: cantidad de procesos coincide (" + p2.length + ")");

        boolean procesosIguales = true;
        for (int i = 0; i < p1.length; i++) {
            if (!p1[i].getNombre().equals(p2[i].getNombre())
                    || p1[i].getOperacion() != p2[i].getOperacion()
                    || !p1[i].getRuta().equals(p2[i].getRuta())
                    || p1[i].getMemoria() != p2[i].getMemoria()
                    || p1[i].getDeadline() != p2[i].getDeadline()
                    || p1[i].getInstrucciones() != p2[i].getInstrucciones()) {
                procesosIguales = false;
                break;
            }
        }
        verificar(procesosIguales, "ida/vuelta: todos los procesos y sus campos coinciden exactamente");
    }

    private static void esperarError(String jsonInvalido, String descripcion) {
        try {
            ConfiguracionProcesos.desdeJson(jsonInvalido);
            verificar(false, descripcion + " (no lanzo excepcion)");
        } catch (EntradaInvalidaException e) {
            verificar(true, descripcion + " (error capturado: " + e.getMessage() + ")");
        }
    }

    private static void verificar(boolean condicion, String mensaje) {
        total++;
        if (condicion) {
            System.out.println("OK    - " + mensaje);
        } else {
            fallos++;
            System.out.println("FALLO - " + mensaje);
        }
    }
}
