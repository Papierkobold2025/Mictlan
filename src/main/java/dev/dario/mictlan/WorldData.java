package dev.dario.mictlan;

import net.minecraft.util.math.ChunkPos;

public class WorldData {
    private String era;
    private ChunkPos mictlanHomeChunks;

    public WorldData(String era) {
        this.era = era;
    }
    
    public ChunkPos worldChunkData(ChunkPos mictlanHomeChunkStart, ChunkPos mictlanHomeChunkFinish) {
        return mictlanHomeChunks;
    }


    public String getEra() {
        return this.era;
    }
}