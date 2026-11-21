package avilaos.fs;

/**
 * @author Gianm
 * Lo que sale de cargar el JSON del sistema de archivos: el arbol, el mapa de
 * bloques, el total de bloques del disco y la posicion inicial del cabezal.
 */
public final class EstadoDiscoInicial {

    private final SistemaArchivos fs;
    private final MapaBloques mapa;
    private final int totalBloques;
    private final int cabezalInicial;

    EstadoDiscoInicial(SistemaArchivos fs, MapaBloques mapa, int totalBloques, int cabezalInicial) {
        this.fs = fs;
        this.mapa = mapa;
        this.totalBloques = totalBloques;
        this.cabezalInicial = cabezalInicial;
    }

    public SistemaArchivos getSistemaArchivos() {
        return fs;
    }

    public MapaBloques getMapa() {
        return mapa;
    }

    /** Bloques del area de archivos (diskConfig.totalBlocks). */
    public int getTotalBloques() {
        return totalBloques;
    }

    /** Bloque donde empieza el cabezal (diskConfig.initialHead). */
    public int getCabezalInicial() {
        return cabezalInicial;
    }
}
