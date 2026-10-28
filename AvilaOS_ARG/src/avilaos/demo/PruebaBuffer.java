package avilaos.demo;

import avilaos.model.MotivoBloqueo;
import avilaos.model.Pcb;
import avilaos.model.TipoProceso;
import avilaos.sync.BufferAcotado;
import avilaos.sync.OperacionBuffer;
import avilaos.sync.ProgramaBuffer;
import avilaos.sync.ResultadoOperacion;
import avilaos.sync.SemaforoSO;

/** Pruebas de los semaforos de ÁvilaOS, el buffer acotado y el programa del productor/consumidor. */
public class PruebaBuffer {

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) throws InterruptedException {
        probarPrograma();
        probarSemaforo();
        probarProductorConsumidor();
        probarConsumidorConBufferVacio();
        probarExclusionMutua();
        probarRetiroYProtecciones();
        probarConcurrencia();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarPrograma() {
        System.out.println("--- Programa del productor y consumidor (cadaCiclos = 2) ---");
        TipoProceso p = TipoProceso.PRODUCTOR;
        TipoProceso c = TipoProceso.CONSUMIDOR;
        verificar(ProgramaBuffer.operacionEn(p, 0, 2) == OperacionBuffer.PRODUCIR, "productor, PC 0: producir");
        verificar(ProgramaBuffer.operacionEn(p, 1, 2) == OperacionBuffer.PRODUCIR, "productor, PC 1: producir");
        verificar(ProgramaBuffer.operacionEn(p, 2, 2) == OperacionBuffer.SEM_WAIT_ESPACIOS, "productor, PC 2: semWait(e)");
        verificar(ProgramaBuffer.operacionEn(p, 3, 2) == OperacionBuffer.SEM_WAIT_MUTEX, "productor, PC 3: semWait(s)");
        verificar(ProgramaBuffer.operacionEn(p, 4, 2) == OperacionBuffer.ANADIR, "productor, PC 4: añadir");
        verificar(ProgramaBuffer.operacionEn(p, 5, 2) == OperacionBuffer.SEM_SIGNAL_MUTEX, "productor, PC 5: semSignal(s)");
        verificar(ProgramaBuffer.operacionEn(p, 6, 2) == OperacionBuffer.SEM_SIGNAL_ELEMENTOS, "productor, PC 6: semSignal(n)");
        verificar(ProgramaBuffer.operacionEn(p, 7, 2) == OperacionBuffer.PRODUCIR, "productor, PC 7: empieza el elemento siguiente");

        verificar(ProgramaBuffer.operacionEn(c, 0, 2) == OperacionBuffer.SEM_WAIT_ELEMENTOS, "consumidor, PC 0: semWait(n)");
        verificar(ProgramaBuffer.operacionEn(c, 1, 2) == OperacionBuffer.SEM_WAIT_MUTEX, "consumidor, PC 1: semWait(s)");
        verificar(ProgramaBuffer.operacionEn(c, 2, 2) == OperacionBuffer.EXTRAER, "consumidor, PC 2: extraer");
        verificar(ProgramaBuffer.operacionEn(c, 3, 2) == OperacionBuffer.SEM_SIGNAL_MUTEX, "consumidor, PC 3: semSignal(s)");
        verificar(ProgramaBuffer.operacionEn(c, 4, 2) == OperacionBuffer.SEM_SIGNAL_ESPACIOS, "consumidor, PC 4: semSignal(e)");
        verificar(ProgramaBuffer.operacionEn(c, 5, 2) == OperacionBuffer.CONSUMIR, "consumidor, PC 5: consumir");
        verificar(ProgramaBuffer.operacionEn(c, 6, 2) == OperacionBuffer.CONSUMIR, "consumidor, PC 6: consumir");
        verificar(ProgramaBuffer.operacionEn(c, 7, 2) == OperacionBuffer.SEM_WAIT_ELEMENTOS, "consumidor, PC 7: empieza el elemento siguiente");

        verificar(ProgramaBuffer.instruccionesTotales(p, 3, 2) == 21, "3 elementos con cadaCiclos 2 son 3 x (2 + 5) = 21 instrucciones");
        verificar(lanza(IllegalArgumentException.class,
                () -> ProgramaBuffer.operacionEn(TipoProceso.CPU_BOUND, 0, 2)),
                "un proceso CPU bound no tiene programa de buffer");
        verificar(lanza(IllegalArgumentException.class,
                () -> ProgramaBuffer.instruccionesTotales(p, 0, 2)),
                "0 elementos no es valido");
        verificar(!OperacionBuffer.PRODUCIR.esModoSistemaOperativo()
                && OperacionBuffer.SEM_WAIT_MUTEX.esModoSistemaOperativo(),
                "producir es modo usuario y semWait es modo sistema operativo");
    }

    private static void probarSemaforo() {
        System.out.println("--- SemaforoSO ---");
        SemaforoSO sem = new SemaforoSO("prueba", 1);
        Pcb a = pcb("A");
        Pcb b = pcb("B");
        Pcb c = pcb("C");

        verificar(sem.esperar(a) && sem.getValor() == 0, "con valor 1, A pasa y el valor baja a 0");
        verificar(!sem.esperar(b), "con valor 0, B se bloquea");
        verificar(!sem.esperar(c), "C tambien se bloquea");
        verificar(sem.getValor() == -2 && sem.cantidadBloqueados() == 2,
                "el valor -2 coincide con los 2 procesos en la cola");
        verificar(sem.getBloqueados()[0] == b && sem.getBloqueados()[1] == c, "la cola respeta el orden de llegada");
        verificar(sem.senalar() == b, "semSignal despierta a B, el primero de la cola");
        verificar(sem.senalar() == c, "el siguiente semSignal despierta a C");
        verificar(sem.senalar() == null && sem.getValor() == 1,
                "sin bloqueados, semSignal solo suma (valor 1)");
        verificar(lanza(IllegalArgumentException.class, () -> new SemaforoSO("malo", -1)),
                "un valor inicial negativo no es valido");
    }

    private static void probarProductorConsumidor() {
        System.out.println("--- Productor y consumidor sobre un buffer de capacidad 2 ---");
        BufferAcotado buf = new BufferAcotado(1, 2, 1);
        Pcb prod = pcb("Prod");
        Pcb cons = pcb("Cons");

        verificar(buf.getHayEspacio().getValor() == 2 && buf.getMutex().getValor() == 1
                && buf.getHayElementos().getValor() == 0, "valores iniciales: e=2, s=1, n=0");
        verificar(producir(buf, prod), "se produce el primer elemento");
        verificar(producir(buf, prod), "se produce el segundo elemento");
        verificar(buf.getElementos() == 2 && buf.getProducidos() == 2, "el buffer tiene 2 elementos");
        verificar(buf.getHayEspacio().getValor() == 0 && buf.getHayElementos().getValor() == 2
                && buf.getMutex().getValor() == 1, "queda e=0, n=2, s=1");

        ResultadoOperacion r = buf.ejecutar(OperacionBuffer.SEM_WAIT_ESPACIOS, prod);
        verificar(r.bloqueado() && r.getMotivo() == MotivoBloqueo.BUFFER_LLENO,
                "con el buffer lleno, el productor se bloquea por BUFFER_LLENO");
        verificar(buf.getHayEspacio().getValor() == -1 && buf.getHayEspacio().cantidadBloqueados() == 1,
                "e vale -1 y hay 1 proceso en su cola");

        verificar(buf.ejecutar(OperacionBuffer.SEM_WAIT_ELEMENTOS, cons).continua(), "el consumidor pasa semWait(n)");
        buf.ejecutar(OperacionBuffer.SEM_WAIT_MUTEX, cons);
        buf.ejecutar(OperacionBuffer.EXTRAER, cons);
        buf.ejecutar(OperacionBuffer.SEM_SIGNAL_MUTEX, cons);
        r = buf.ejecutar(OperacionBuffer.SEM_SIGNAL_ESPACIOS, cons);
        verificar(r.getDespertado() == prod, "al liberar un espacio, el consumidor despierta al productor");
        verificar(buf.getElementos() == 1 && buf.getConsumidos() == 1, "el buffer quedo con 1 elemento");

        // el productor despertado sigue desde la instruccion que le faltaba
        buf.ejecutar(OperacionBuffer.SEM_WAIT_MUTEX, prod);
        buf.ejecutar(OperacionBuffer.ANADIR, prod);
        buf.ejecutar(OperacionBuffer.SEM_SIGNAL_MUTEX, prod);
        buf.ejecutar(OperacionBuffer.SEM_SIGNAL_ELEMENTOS, prod);
        verificar(buf.getElementos() == 2 && buf.getProducidos() == 3,
                "el productor despertado termina su elemento: 2 en el buffer, 3 producidos");
        verificar(buf.getEtiqueta().equals("B1") && buf.getMemoriaRequerida() == 2,
                "etiqueta B1 y 2 MB de RAM");
    }

    private static void probarConsumidorConBufferVacio() {
        System.out.println("--- Consumidor con el buffer vacio ---");
        BufferAcotado buf = new BufferAcotado(2, 3, 1);
        Pcb prod = pcb("Prod");
        Pcb cons = pcb("Cons");

        ResultadoOperacion r = buf.ejecutar(OperacionBuffer.SEM_WAIT_ELEMENTOS, cons);
        verificar(r.bloqueado() && r.getMotivo() == MotivoBloqueo.BUFFER_VACIO,
                "con el buffer vacio, el consumidor se bloquea por BUFFER_VACIO");

        buf.ejecutar(OperacionBuffer.SEM_WAIT_ESPACIOS, prod);
        buf.ejecutar(OperacionBuffer.SEM_WAIT_MUTEX, prod);
        buf.ejecutar(OperacionBuffer.ANADIR, prod);
        buf.ejecutar(OperacionBuffer.SEM_SIGNAL_MUTEX, prod);
        r = buf.ejecutar(OperacionBuffer.SEM_SIGNAL_ELEMENTOS, prod);
        verificar(r.getDespertado() == cons, "al producir un elemento, el productor despierta al consumidor");
    }

    private static void probarExclusionMutua() {
        System.out.println("--- Exclusion mutua (semaforo s) ---");
        BufferAcotado buf = new BufferAcotado(3, 2, 1);
        Pcb a = pcb("A");
        Pcb b = pcb("B");

        verificar(buf.ejecutar(OperacionBuffer.SEM_WAIT_MUTEX, a).continua(), "A entra a la region critica");
        ResultadoOperacion r = buf.ejecutar(OperacionBuffer.SEM_WAIT_MUTEX, b);
        verificar(r.bloqueado() && r.getMotivo() == MotivoBloqueo.REGION_CRITICA_OCUPADA,
                "B se bloquea por REGION_CRITICA_OCUPADA");
        r = buf.ejecutar(OperacionBuffer.SEM_SIGNAL_MUTEX, a);
        verificar(r.getDespertado() == b, "al salir A, despierta a B");
    }

    private static void probarRetiroYProtecciones() {
        System.out.println("--- Retiro de un proceso y protecciones ---");
        BufferAcotado buf = new BufferAcotado(4, 1, 1);
        Pcb p1 = pcb("P1");
        Pcb p2 = pcb("P2");

        verificar(producir(buf, p1), "P1 produce y llena el buffer de capacidad 1");
        verificar(buf.ejecutar(OperacionBuffer.SEM_WAIT_ESPACIOS, p2).bloqueado(), "P2 se bloquea esperando espacio");
        verificar(buf.getHayEspacio().getValor() == -1, "e vale -1");
        verificar(buf.retirarProceso(p2), "retirar a P2 de la cola de espera funciona");
        verificar(buf.getHayEspacio().getValor() == 0 && buf.getHayEspacio().cantidadBloqueados() == 0,
                "e vuelve a 0 y la cola queda vacia");
        verificar(!buf.retirarProceso(p2), "retirar a quien ya no esta devuelve false");

        BufferAcotado lleno = new BufferAcotado(5, 1, 1);
        lleno.ejecutar(OperacionBuffer.ANADIR, p1);
        verificar(lanza(IllegalStateException.class, () -> lleno.ejecutar(OperacionBuffer.ANADIR, p1)),
                "añadir a un buffer lleno, saltandose los semaforos, lanza excepcion");
        BufferAcotado vacio = new BufferAcotado(6, 1, 1);
        verificar(lanza(IllegalStateException.class, () -> vacio.ejecutar(OperacionBuffer.EXTRAER, p1)),
                "extraer de un buffer vacio lanza excepcion");
        verificar(lanza(IllegalArgumentException.class, () -> new BufferAcotado(1, 0, 1)),
                "un buffer de capacidad 0 no es valido");
    }

    private static void probarConcurrencia() throws InterruptedException {
        System.out.println("--- Dos hilos de Java sobre el mismo semaforo ---");
        SemaforoSO sem = new SemaforoSO("concurrente", 5000);
        Pcb p = pcb("Hilo");
        Runnable tarea = () -> {
            for (int i = 0; i < 1000; i++) {
                sem.esperar(p);
            }
        };
        Thread a = new Thread(tarea, "Sem-A");
        Thread b = new Thread(tarea, "Sem-B");
        a.start();
        b.start();
        a.join();
        b.join();
        verificar(sem.getValor() == 3000 && sem.cantidadBloqueados() == 0,
                "2 hilos x 1000 semWait dejan el valor exactamente en 3000");
    }

    // ---- ayudas ----

    /** Ejecuta las 5 operaciones de un elemento; devuelve false si el proceso se bloqueo. */
    private static boolean producir(BufferAcotado buf, Pcb p) {
        OperacionBuffer[] pasos = {
            OperacionBuffer.SEM_WAIT_ESPACIOS,
            OperacionBuffer.SEM_WAIT_MUTEX,
            OperacionBuffer.ANADIR,
            OperacionBuffer.SEM_SIGNAL_MUTEX,
            OperacionBuffer.SEM_SIGNAL_ELEMENTOS
        };
        for (OperacionBuffer paso : pasos) {
            if (buf.ejecutar(paso, p).bloqueado()) {
                return false;
            }
        }
        return true;
    }

    private static Pcb pcb(String nombre) {
        return new Pcb(nombre, 1, TipoProceso.PRODUCTOR, 1, 10, 100, 1000, 0);
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
