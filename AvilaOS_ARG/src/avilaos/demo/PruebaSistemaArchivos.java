package avilaos.demo;

import avilaos.fs.MapaBloques;
import avilaos.fs.NodoFs;
import avilaos.fs.Permiso;
import avilaos.fs.ResultadoFs;
import avilaos.fs.SistemaArchivos;
import avilaos.model.TipoOperacionES;

/**
 * @author Gianm
 * Pruebas del sistema de archivos: crear y buscar, validaciones, leer, renombrar
 * archivos y directorios, eliminar (tambien recursivo), falta de espacio y lo que
 * el nucleo pide para el disco (bloqueDestino y bloquesATransferir).
 */
public class PruebaSistemaArchivos {

    private static final Permiso PUB = Permiso.PUBLICO;
    private static final Permiso PRIV = Permiso.PRIVADO;

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        probarCrearYBuscar();
        probarValidacionesDeCrear();
        probarLeer();
        probarRenombrarArchivo();
        probarRenombrarDirectorio();
        probarEliminarArchivo();
        probarEliminarDirectorio();
        probarSinEspacio();
        probarDestinoYTransferencia();
        probarArchivosYCadenaRepartida();
        probarCasosLimite();
        probarConcurrencia();

        System.out.println();
        System.out.println("Comprobaciones: " + total + ", fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY FALLOS: revisar las lineas FALLO");
    }

    private static void probarCrearYBuscar() {
        System.out.println("--- Crear y buscar ---");
        SistemaArchivos fs = new SistemaArchivos(new MapaBloques(50));
        verificar(fs.buscar("/") == fs.getRaiz(), "la raiz / existe desde el inicio");
        verificar(fs.crearDirectorio("/sistema", "admin", PUB).isExito(), "crea el directorio /sistema");
        verificar(fs.crearDirectorio("/usuarios", "admin", PUB).isExito(), "crea el directorio /usuarios");
        ResultadoFs r = fs.crearArchivo("/sistema/boot.bin", 5, "admin", PRIV, "sistema");
        verificar(r.isExito(), "crea /sistema/boot.bin con 5 bloques");
        NodoFs boot = fs.buscar("/sistema/boot.bin");
        verificar(boot != null && boot.esArchivo(), "lo encuentra por su ruta");
        verificar(boot != null && boot.getRuta().equals("/sistema/boot.bin"), "getRuta arma la ruta completa");
        verificar(boot != null && boot.getPrimerBloque() == 0 && boot.getCantidadBloques() == 5,
                "empieza en el bloque 0 y ocupa 5 bloques");
        verificar(fs.getMapa().libres() == 45, "el mapa tiene 45 bloques libres");
        verificar(fs.buscar("/nada") == null && fs.buscar("sistema") == null,
                "una ruta inexistente o sin / devuelve null");
        verificar(fs.getVersion() == 3, "la version subio una vez por cada cambio");
    }

    private static void probarValidacionesDeCrear() {
        System.out.println("--- Validaciones al crear ---");
        SistemaArchivos fs = new SistemaArchivos(new MapaBloques(50));
        fs.crearDirectorio("/docs", "admin", PUB);
        fs.crearArchivo("/docs/a.txt", 2, "admin", PUB, "P1");
        int version = fs.getVersion();
        verificar(!fs.crearArchivo("/x/y.txt", 1, "admin", PUB, "P1").isExito(), "crear en un padre que no existe falla");
        verificar(!fs.crearArchivo("/docs/a.txt", 1, "admin", PUB, "P1").isExito(), "un nombre repetido falla");
        verificar(!fs.crearArchivo("/docs/" + "n".repeat(31), 1, "admin", PUB, "P1").isExito(),
                "un nombre de mas de 30 caracteres falla");
        verificar(!fs.crearDirectorio("docs2", "admin", PUB).isExito(), "una ruta sin / al inicio falla");
        verificar(!fs.crearArchivo("/docs/a.txt/b", 1, "admin", PUB, "P1").isExito(), "no se crea nada dentro de un archivo");
        verificar(!fs.crearArchivo("/docs/b.txt", 1, " ", PUB, "P1").isExito(), "un dueno vacio falla");
        verificar(!fs.crearArchivo("/docs/c.txt", 0, "admin", PUB, "P1").isExito(), "0 bloques falla");
        verificar(fs.getVersion() == version && fs.getMapa().libres() == 48, "los fallos no cambian nada");
    }

    private static void probarLeer() {
        System.out.println("--- Leer ---");
        SistemaArchivos fs = new SistemaArchivos(new MapaBloques(20));
        fs.crearDirectorio("/d", "admin", PUB);
        fs.crearArchivo("/d/f.txt", 3, "admin", PUB, "P1");
        verificar(fs.leer("/d/f.txt").isExito(), "leer un archivo existente sale bien");
        verificar(fs.leer("/d").isExito(), "leer un directorio existente sale bien");
        ResultadoFs r = fs.leer("/d/nada.txt");
        verificar(!r.isExito() && r.getMensaje().contains("/d/nada.txt"), "leer un archivo inexistente falla y dice cual");
        verificar(fs.getVersion() == 2, "leer no cambia la version");
    }

    private static void probarRenombrarArchivo() {
        System.out.println("--- Renombrar un archivo ---");
        SistemaArchivos fs = new SistemaArchivos(new MapaBloques(30));
        fs.crearDirectorio("/usuarios", "admin", PUB);
        fs.crearArchivo("/usuarios/notas.txt", 3, "user1", PRIV, "P1");
        fs.crearArchivo("/usuarios/otro.txt", 1, "user1", PRIV, "P2");
        int primero = fs.buscar("/usuarios/notas.txt").getPrimerBloque();
        verificar(fs.renombrar("/usuarios/notas.txt", "apuntes.txt").isExito(), "renombra notas.txt a apuntes.txt");
        verificar(fs.buscar("/usuarios/notas.txt") == null && fs.buscar("/usuarios/apuntes.txt") != null,
                "la ruta vieja ya no existe y la nueva si");
        boolean ocupantes = true;
        for (int b : fs.getMapa().cadena(primero)) {
            ocupantes = ocupantes && "/usuarios/apuntes.txt".equals(fs.getMapa().getOcupante(b));
        }
        verificar(ocupantes, "todos sus bloques tienen el ocupante nuevo en el mapa");
        verificar(!fs.renombrar("/usuarios/apuntes.txt", "otro.txt").isExito(), "renombrar a un nombre que ya existe falla");
        verificar(!fs.renombrar("/", "raiz").isExito(), "la raiz no se puede renombrar");
        verificar(!fs.renombrar("/usuarios/apuntes.txt", "a/b").isExito(), "un nombre con / falla");
        verificar(!fs.renombrar("/usuarios/nada.txt", "x.txt").isExito(), "renombrar algo que no existe falla");
    }

    private static void probarRenombrarDirectorio() {
        System.out.println("--- Renombrar un directorio ---");
        SistemaArchivos fs = new SistemaArchivos(new MapaBloques(30));
        fs.crearDirectorio("/usuarios", "admin", PUB);
        fs.crearDirectorio("/usuarios/fotos", "user1", PUB);
        fs.crearArchivo("/usuarios/fotos/a.png", 2, "user1", PUB, "P1");
        fs.crearArchivo("/usuarios/b.txt", 1, "user1", PUB, "P2");
        int pa = fs.buscar("/usuarios/fotos/a.png").getPrimerBloque();
        int pb = fs.buscar("/usuarios/b.txt").getPrimerBloque();
        verificar(fs.renombrar("/usuarios", "home").isExito(), "renombra el directorio /usuarios a /home");
        verificar(fs.buscar("/home/fotos/a.png") != null && fs.buscar("/home/b.txt") != null,
                "cambian las rutas de todo lo que tiene adentro");
        verificar("/home/fotos/a.png".equals(fs.getMapa().getOcupante(pa))
                && "/home/b.txt".equals(fs.getMapa().getOcupante(pb)),
                "el mapa tiene las rutas nuevas, tambien en subdirectorios");
        verificar(fs.buscar("/usuarios") == null, "la ruta vieja del directorio ya no existe");
    }

    private static void probarEliminarArchivo() {
        System.out.println("--- Eliminar un archivo ---");
        SistemaArchivos fs = new SistemaArchivos(new MapaBloques(20));
        fs.crearArchivo("/a.txt", 4, "admin", PUB, "P1");   // bloques 0 a 3
        fs.crearArchivo("/b.txt", 3, "admin", PUB, "P2");   // bloques 4 a 6
        verificar(fs.eliminar("/a.txt").isExito(), "elimina /a.txt");
        verificar(fs.getMapa().libres() == 17, "se liberan sus 4 bloques");
        verificar(fs.buscar("/a.txt") == null, "ya no aparece en el arbol");
        verificar(!fs.eliminar("/a.txt").isExito(), "eliminarlo otra vez falla con mensaje (no lanza excepcion)");
        verificar(fs.buscar("/b.txt") != null && "/b.txt".equals(fs.getMapa().getOcupante(4)),
                "el otro archivo no se toca");
    }

    private static void probarEliminarDirectorio() {
        System.out.println("--- Eliminar un directorio con subdirectorios ---");
        SistemaArchivos fs = new SistemaArchivos(new MapaBloques(40));
        int libresInicio = fs.getMapa().libres();
        fs.crearDirectorio("/a", "admin", PUB);
        fs.crearDirectorio("/a/b", "admin", PUB);
        fs.crearArchivo("/a/b/c.txt", 4, "admin", PUB, "P1");
        fs.crearArchivo("/a/d.txt", 3, "admin", PUB, "P2");
        fs.crearDirectorio("/a/b/e", "admin", PUB);
        fs.crearArchivo("/a/b/e/f.bin", 5, "admin", PUB, "P3");
        fs.crearArchivo("/fuera.txt", 2, "admin", PUB, "P4");
        verificar(fs.eliminar("/a").isExito(), "elimina el directorio /a con sus subdirectorios");
        verificar(fs.getMapa().libres() == libresInicio - 2, "se liberan los 12 bloques de los 3 archivos de adentro");
        verificar(fs.buscar("/a") == null && fs.buscar("/a/b/e/f.bin") == null, "no queda nada del directorio en el arbol");
        verificar(fs.buscar("/fuera.txt") != null, "lo que estaba afuera sigue igual");
        verificar(!fs.eliminar("/").isExito(), "la raiz no se puede eliminar");
        fs.eliminar("/fuera.txt");
        verificar(fs.getMapa().libres() == libresInicio, "al final libres() vuelve al valor inicial");
    }

    private static void probarSinEspacio() {
        System.out.println("--- Sin espacio ---");
        SistemaArchivos fs = new SistemaArchivos(new MapaBloques(10));
        fs.crearArchivo("/grande", 8, "admin", PUB, "P1");
        ResultadoFs r = fs.crearArchivo("/b.txt", 5, "admin", PUB, "P2");
        verificar(!r.isExito() && r.getMensaje().contains("se piden 5 y quedan 2"),
                "sin espacio se rechaza con un mensaje que dice cuanto falta");
        verificar(fs.buscar("/b.txt") == null && fs.getMapa().libres() == 2, "no queda nada a medias");
    }

    private static void probarDestinoYTransferencia() {
        System.out.println("--- bloqueDestino y bloquesATransferir ---");
        SistemaArchivos fs = new SistemaArchivos(new MapaBloques(50));
        fs.crearDirectorio("/docs", "admin", PUB);
        fs.crearArchivo("/docs/a.txt", 4, "admin", PUB, "P1");   // bloques 0 a 3
        fs.crearDirectorio("/vacio", "admin", PUB);
        fs.crearArchivo("/b.txt", 2, "admin", PUB, "P2");        // bloques 4 y 5
        verificar(fs.bloqueDestino(TipoOperacionES.LEER, "/docs/a.txt") == 0, "LEER va al primer bloque del archivo");
        verificar(fs.bloqueDestino(TipoOperacionES.ACTUALIZAR, "/b.txt") == 4
                && fs.bloqueDestino(TipoOperacionES.ELIMINAR, "/b.txt") == 4,
                "ACTUALIZAR y ELIMINAR tambien van a su primer bloque");
        verificar(fs.bloqueDestino(TipoOperacionES.CREAR, "/docs/nuevo.txt") == 6, "CREAR va al primer bloque libre");
        verificar(fs.bloqueDestino(TipoOperacionES.ELIMINAR, "/docs") == 0,
                "un directorio va al primer bloque de su primer archivo");
        verificar(fs.bloqueDestino(TipoOperacionES.LEER, "/vacio") == -1,
                "un directorio vacio devuelve -1 (el cabezal no se mueve)");
        verificar(fs.bloqueDestino(TipoOperacionES.LEER, "/nada") == -1, "una ruta inexistente devuelve -1");
        verificar(fs.bloquesATransferir(TipoOperacionES.LEER, "/docs/a.txt", 0) == 4,
                "LEER transfiere todos los bloques del archivo");
        verificar(fs.bloquesATransferir(TipoOperacionES.CREAR, "/docs/n.txt", 7) == 7,
                "CREAR transfiere los bloques nuevos");
        verificar(fs.bloquesATransferir(TipoOperacionES.ACTUALIZAR, "/b.txt", 0) == 1
                && fs.bloquesATransferir(TipoOperacionES.ELIMINAR, "/b.txt", 0) == 1,
                "ACTUALIZAR y ELIMINAR transfieren 1 bloque (solo metadatos)");
    }

    private static void probarArchivosYCadenaRepartida() {
        System.out.println("--- archivos() y cadena repartida ---");
        SistemaArchivos fs = new SistemaArchivos(new MapaBloques(20));
        fs.crearArchivo("/a", 3, "admin", PUB, "P1");   // 0, 1, 2
        fs.crearArchivo("/b", 2, "admin", PUB, "P2");   // 3, 4
        fs.crearArchivo("/c", 2, "admin", PUB, "P3");   // 5, 6
        fs.eliminar("/b");
        fs.crearArchivo("/d", 4, "admin", PUB, "P4");   // 3, 4, 7, 8
        int[] cadena = fs.getMapa().cadena(fs.buscar("/d").getPrimerBloque());
        verificar(cadena.length == 4 && cadena[0] == 3 && cadena[1] == 4 && cadena[2] == 7 && cadena[3] == 8,
                "un archivo nuevo llena el hueco y sigue al final: 3 4 7 8");
        verificar(fs.archivos().length == 3, "archivos() devuelve los 3 archivos del arbol");
    }

        // ================= Dia 9: casos limite y concurrencia =================

    private static void probarCasosLimite() {
        System.out.println("--- Casos limite (procesos que se cruzan) ---");
        SistemaArchivos fs = new SistemaArchivos(new MapaBloques(50));
        fs.crearDirectorio("/docs", "admin", PUB);
        fs.crearArchivo("/docs/a.txt", 3, "admin", PUB, "P1");   // bloques 0, 1 y 2

        // P1 y P2 piden eliminar el mismo archivo; el disco atiende primero a P1
        int destino = fs.bloqueDestino(TipoOperacionES.ELIMINAR, "/docs/a.txt");
        verificar(fs.eliminar("/docs/a.txt").isExito(), "P1 elimina /docs/a.txt");
        ResultadoFs r2 = fs.eliminar("/docs/a.txt");
        verificar(!r2.isExito() && r2.getMensaje().contains("No existe"),
                "P2 intenta eliminar el mismo archivo: falla con mensaje y no lanza excepcion");
        verificar(destino == 0 && fs.bloqueDestino(TipoOperacionES.ELIMINAR, "/docs/a.txt") == -1,
                "despues de eliminado, su bloque destino es -1 (el cabezal no se mueve)");
        verificar(!fs.leer("/docs/a.txt").isExito() && !fs.renombrar("/docs/a.txt", "b.txt").isExito(),
                "leer o renombrar un archivo ya eliminado falla con mensaje");

        // P3 pide crear en /tmp, pero mientras espera al disco otro proceso borra /tmp
        fs.crearDirectorio("/tmp", "admin", PUB);
        int libres = fs.getMapa().libres();
        fs.eliminar("/tmp");
        ResultadoFs r3 = fs.crearArchivo("/tmp/nuevo.txt", 4, "admin", PUB, "P3");
        verificar(!r3.isExito() && r3.getMensaje().contains("No existe el directorio /tmp"),
                "crear en un directorio que se borro mientras esperaba falla con mensaje");
        verificar(fs.getMapa().libres() == libres, "y no se reserva ningun bloque");

        // renombrar un directorio a un nombre que ya existe
        fs.crearDirectorio("/fotos", "admin", PUB);
        fs.crearArchivo("/fotos/f.png", 2, "admin", PUB, "P4");
        ResultadoFs r4 = fs.renombrar("/docs", "fotos");
        verificar(!r4.isExito() && r4.getMensaje().contains("Ya existe /fotos"),
                "renombrar /docs a fotos (que ya existe) falla con mensaje");
        verificar(fs.buscar("/docs") != null && fs.buscar("/fotos/f.png") != null,
                "los dos directorios quedan como estaban");
    }

    /** Primer error que haya lanzado alguno de los hilos de la prueba de concurrencia. */
    private static volatile String errorEnHilo = null;

    private static void probarConcurrencia() {
        System.out.println("--- Concurrencia: tres hilos a la vez durante 1 segundo ---");
        SistemaArchivos fs = new SistemaArchivos(new MapaBloques(200));
        fs.crearDirectorio("/conc", "admin", PUB);
        long fin = System.currentTimeMillis() + 1000;
        int[] operaciones = new int[3];

        Thread escritorA = new Thread(() -> crearYBorrar(fs, "a", fin, operaciones, 0));
        Thread escritorB = new Thread(() -> crearYBorrar(fs, "b", fin, operaciones, 1));
        Thread lector = new Thread(() -> leerSinParar(fs, fin, operaciones, 2));
        escritorA.start();
        escritorB.start();
        lector.start();
        esperar(escritorA);
        esperar(escritorB);
        esperar(lector);

        verificar(errorEnHilo == null,
                "ningun hilo lanzo excepciones" + (errorEnHilo == null ? "" : ": " + errorEnHilo));
        verificar(operaciones[0] > 0 && operaciones[1] > 0 && operaciones[2] > 0,
                "los tres hilos trabajaron a la vez (A: " + operaciones[0] + " archivos, B: "
                + operaciones[1] + " archivos, lector: " + operaciones[2] + " vueltas)");

        int usados = 0;
        boolean coherente = true;
        for (NodoFs a : fs.archivos()) {
            usados += a.getCantidadBloques();
            int[] cadena = fs.getMapa().cadena(a.getPrimerBloque());
            coherente = coherente && cadena.length == a.getCantidadBloques();
            for (int b : cadena) {
                coherente = coherente && a.getRuta().equals(fs.getMapa().getOcupante(b));
            }
        }
        int libres = fs.getMapa().libres();
        verificar(usados + libres == fs.getMapa().getBloquesArchivos(),
                "bloques de los archivos + libres = total (" + usados + " + " + libres + " = "
                + fs.getMapa().getBloquesArchivos() + ")");
        verificar(coherente, "cada archivo conserva su cadena completa y sus bloques lo tienen como ocupante");
    }

    /** Un "proceso" que crea archivos sin parar y va borrando los que creo hace 3 vueltas. */
    private static void crearYBorrar(SistemaArchivos fs, String prefijo, long fin, int[] operaciones, int indice) {
        try {
            int i = 0;
            while (System.currentTimeMillis() < fin) {
                fs.crearArchivo("/conc/" + prefijo + i, 1 + i % 7, "admin", PUB, "P" + prefijo);
                if (i >= 3) {
                    fs.eliminar("/conc/" + prefijo + (i - 3));
                }
                i++;
            }
            operaciones[indice] = i;
        } catch (RuntimeException e) {
            errorEnHilo = prefijo + ": " + e;
        }
    }

    /** Hace lo mismo que la interfaz: leer la lista de archivos y la cadena de cada uno. */
    private static void leerSinParar(SistemaArchivos fs, long fin, int[] operaciones, int indice) {
        try {
            int vueltas = 0;
            while (System.currentTimeMillis() < fin) {
                for (NodoFs a : fs.archivos()) {
                    fs.getMapa().cadena(a.getPrimerBloque());
                    fs.buscar(a.getRuta());
                }
                vueltas++;
            }
            operaciones[indice] = vueltas;
        } catch (RuntimeException e) {
            errorEnHilo = "lector: " + e;
        }
    }

    private static void esperar(Thread hilo) {
        try {
            hilo.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorEnHilo = "se interrumpio la espera de un hilo";
        }
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
