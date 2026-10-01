package dev.dario.mictlan;

import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import java.util.List;
import java.nio.file.Files;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.util.math.Box;

import java.util.Optional;
import java.util.ArrayList;
import java.nio.file.Path;

public class HelpersComandos {
    private static ChunkPos secondChunk;
    private static Path worldDataChunks;
    private static String worldChunkData = "";
    private static WorldData  worldChunkDataReturn;
    private static Path mictlanCoreDataFile;
    private static Path mictlanCoreEntitiesFile;
    private static ServerWorld mundoParaTransportar;
    private static List<Entity> entidadesDelChunk;
    private static NbtCompound entidadesAGuardar;
    private static NbtCompound archivoEntidades;
    private static Box caja;
    private static ServerPlayerEntity player;
    public static ChunkPos chunk;
    public static ChunkPos firstChunk;
    public static ArrayList<Integer> homeChunksX = new ArrayList<>();
    public static ArrayList<Integer> homeChunksZ = new ArrayList<>();
    public static ArrayList<ChunkPos> totalChunkPosCount = new ArrayList<>();
    
    /**
    * -------------------------------------------------------------------------
    * EXTRACTO DE COMANDO PARA GUARDAR CHUNKS DE CASA EN RAM (ENTIDADES/BLOQUES)
    * -------------------------------------------------------------------------
    */

    /**
    * -------------------------------------------------------------------------
    * PRIMERA ESQUINA PARA EXTRACTO DE CHUNKS (BLOQUES/ENTIDADES)
    * -------------------------------------------------------------------------
    */

    public static void primeraEsquina(CommandContext<ServerCommandSource> context) {
        player = context.getSource().getPlayer();
        chunk = player.getChunkPos();
        homeChunksX.clear();
        homeChunksZ.clear();
        totalChunkPosCount.clear();
        firstChunk = chunk;
    }

    /**
    * -------------------------------------------------------------------------
    * SEGUNDA ESQUINA PARA EXTRACTO DE CHUNKS (BLOQUES/ENTIDADES)
    * -------------------------------------------------------------------------
    */

    public static void segundaEsquina() {
        secondChunk = chunk;
        worldDataChunks = Path.of(WorldLoad.worldData.toString(), MictlanMod.eraActual + ".json");
        MictlanMod.CurrentEra.mictlanHomeChunks(secondChunk, firstChunk);
        if(!Files.exists(worldDataChunks)) {
            try {
                Files.writeString(worldDataChunks, MictlanMod.gson.toJson(MictlanMod.CurrentEra));
            } catch (IOException e) {
                MictlanMod.LOGGER.error("[Mictlan] Datos de los Chunks no pusieron ser escritos!");
            }
        }
        // Se relee de disco para usar lo que realmente quedo guardado.
        try {
            worldChunkData = Files.readString(worldDataChunks);
        } catch (Exception e) {
            MictlanMod.LOGGER.error("[Mictlan] Datos de los chunks no pueden ser leidos");
        }
        worldChunkDataReturn = MictlanMod.gson.fromJson(worldChunkData, MictlanMod.CurrentEra.getClass());
        ChunkPos homeStartPos = worldChunkDataReturn.getHomeStart();
        ChunkPos homeFinishPos = worldChunkDataReturn.getHomeFinish();

        // --- Paso 1: rango X (min/max por si las esquinas vienen en cualquier orden) ---
        int lowChunkCount = Math.min(homeStartPos.x, homeFinishPos.x);
        int highChunkCount = Math.max(homeStartPos.x, homeFinishPos.x);
        for(int chunksStart = lowChunkCount; chunksStart <= highChunkCount; chunksStart++) {
            homeChunksX.add(chunksStart);
        }

        // --- Paso 2: rango Z ---
        lowChunkCount = Math.min(homeStartPos.z, homeFinishPos.z);
        highChunkCount = Math.max(homeStartPos.z, homeFinishPos.z);
        for(int chunksFinish = lowChunkCount; chunksFinish <= highChunkCount; chunksFinish++) {
            homeChunksZ.add(chunksFinish);
        }

        // --- Paso 3: producto X * Z ---
        for( int chunkCounterX = 0; chunkCounterX < homeChunksX.size(); chunkCounterX++) {
            for(int chunkCounterZ = 0; chunkCounterZ < homeChunksZ.size(); chunkCounterZ++) {
                ChunkPos chunkAggregation = new ChunkPos (homeChunksX.get(chunkCounterX), homeChunksZ.get(chunkCounterZ));
                totalChunkPosCount.add(chunkAggregation);
            }
        }
    }

    /**
    * -------------------------------------------------------------------------
    * GUARDAR LOS CHUNKS CARGADOS EN RAM EN DISCO
    * -------------------------------------------------------------------------
    */

    public static void guardarCasaEnDisco(CommandContext<ServerCommandSource> context, ChunkPos chunks) {
        mictlanCoreDataFile = Path.of(WorldLoad.mictlanCoreDataPath.toString(), "Chunk_" + chunks.x + " " + chunks.z + ".nbt");
        mundoParaTransportar = context.getSource().getWorld();
        // Vacio si el chunk nunca se ha guardado en disco.
        Optional<NbtCompound> datosChunk = mundoParaTransportar.getChunkManager().threadedAnvilChunkStorage.getNbt(chunks).join();
        if(datosChunk.isPresent()) {
            MictlanMod.LOGGER.info("[Mictlan] " + datosChunk.get().getKeys());
            try{
                NbtIo.writeCompressed(datosChunk.get(), mictlanCoreDataFile.toFile());
            } catch(IOException e) {
                MictlanMod.LOGGER.error("[Mictlan] Datos de Chunk no pudieron ser guardados en disco!");
            }
        }
    }

    /**
    * -------------------------------------------------------------------------
    * GUARDAR LAS ENTIDADES CARGADAS EN RAM EN DISCO
    * -------------------------------------------------------------------------
    */

    public static void guardarEntidadesEnDisco(ChunkPos chunks) {
        mictlanCoreEntitiesFile = Path.of(WorldLoad.mictlanCoreEntitiesPath.toString(), "Chunk_" + chunks.x + " " + chunks.z + ".nbt");
        // Entidades: se buscan por volumen (+1 porque getEnd es inclusivo).
        caja = new Box(chunks.getStartX(), mundoParaTransportar.getBottomY(), chunks.getStartZ(), chunks.getEndX() + 1, mundoParaTransportar.getTopY(), chunks.getEndZ() +1);
        entidadesDelChunk = mundoParaTransportar.getOtherEntities(null, caja);
        NbtList listaEntidades = new NbtList();
        for (Entity entity : entidadesDelChunk) {
            entidadesAGuardar = new NbtCompound();
            // false para jugadores, pasajeros y entidades removidas.
            if (entity.saveSelfNbt(entidadesAGuardar)) {
                listaEntidades.add(entidadesAGuardar);
            }
        }
        archivoEntidades = new NbtCompound();
        archivoEntidades.put("entidades", listaEntidades);
        try {
            NbtIo.writeCompressed(archivoEntidades, mictlanCoreEntitiesFile.toFile());
        } catch (Exception e) {
            MictlanMod.LOGGER.error("Entidades del Chunk " + chunks.x + " " + chunks.z + " no se pudieron guardar.", e);
        }
    }

    /**
    * -------------------------------------------------------------------------
    * SEGUNDA ESQUINA PARA EXTRACTO DE CHUNKS (BLOQUES/ENTIDADES)
    * -------------------------------------------------------------------------
    */

    public static void borrarDatosCargados() {
        homeChunksZ.clear();
        totalChunkPosCount.clear();
        // El siguiente /mictlan home vuelve a ser la primera esquina.
        MictlanMod.commandExecuted = 0;
        try {
            Files.delete(Path.of(WorldLoad.worldData.toString(), MictlanMod.eraActual + ".json"));
        } catch (Exception e) {
            MictlanMod.LOGGER.error("[Mictlan] Datos de los chunks no pueden ser borrados");
        }
    }
}
