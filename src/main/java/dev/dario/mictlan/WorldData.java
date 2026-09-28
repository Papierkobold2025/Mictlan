package dev.dario.mictlan;

import net.minecraft.util.math.ChunkPos;

/**
 * Clase para almacenar los datos de una era del mundo: su nombre y la zona
 * "home" marcada con /mictlan home.
 *
 * Gson convierte este objeto a JSON (y de vuelta) usando los nombres de los
 * campos, asi que el archivo world/<era>.json tendra la misma forma que esta clase.
 */
public class WorldData {
    /** Nombre de la era (por ejemplo: "MEDIEVAL"). */
    private String era;

    /** Una esquina de la zona "home" (posicion de chunk: x, z). */
    private ChunkPos mictlanHomeChunkStart;

    /** La esquina opuesta de la zona "home". */
    private ChunkPos mictlanHomeChunkFinish;

    private boolean homeChunksPasted;

    /** Crea los datos de una era nueva, todavia sin zona "home". */
    public WorldData(String era) {
        this.era = era;
    }

    /** Guarda las dos esquinas que delimitan la zona "home". */
    public void mictlanHomeChunks(ChunkPos mictlanHomeChunkStart, ChunkPos mictlanHomeChunkFinish) {
        this.mictlanHomeChunkStart = mictlanHomeChunkStart;
        this.mictlanHomeChunkFinish = mictlanHomeChunkFinish;
    }

    public void homeChunkPasted(boolean homeChunkPasted) {
        this.homeChunksPasted = homeChunkPasted;
    }

    /** Devuelve la primera esquina de la zona "home". */
    public ChunkPos getHomeStart() {
        return this.mictlanHomeChunkStart;
    }

    /** Devuelve la segunda esquina de la zona "home". */
    public ChunkPos getHomeFinish() {
        return this.mictlanHomeChunkFinish;
    }

    /** Devuelve el nombre de la era. */
    public String getEra() {
        return this.era;
    }
}
